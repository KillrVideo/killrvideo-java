package com.killrvideo.service.search.grpc;

import static com.killrvideo.service.search.grpc.SearchServiceGrpcValidator.validateGrpcRequest_GetQuerySuggestions;
import static com.killrvideo.service.search.grpc.SearchServiceGrpcValidator.validateGrpcRequest_SearchVideos;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.killrvideo.dse.dto.ResultListPage;
import com.killrvideo.dse.dto.Video;
import com.killrvideo.service.search.dao.SearchDseDao;
import com.killrvideo.service.search.dao.SearchDseDaoApollo;
import com.killrvideo.service.search.dao.SearchDseDaoMapperBuilder;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import killrvideo.search.SearchServiceGrpc.SearchServiceImplBase;
import killrvideo.search.SearchServiceOuterClass.GetQuerySuggestionsRequest;
import killrvideo.search.SearchServiceOuterClass.GetQuerySuggestionsResponse;
import killrvideo.search.SearchServiceOuterClass.SearchVideosRequest;
import killrvideo.search.SearchServiceOuterClass.SearchVideosResponse;

/**
 * Service SEARCG.
 *
 * @author DataStax advocates Team
 */
@Service
public class SearchServiceGrpc extends SearchServiceImplBase {

    /** Logger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(SearchServiceGrpc.class);
    
    /** Definition of operation for Search. */
    private SearchDseDao dseSearchDao;
    
    @Autowired
    private CqlSession cqlSession;
    
    @Autowired
    @Qualifier("killrvideo.keyspace")
    private CqlIdentifier dseKeySpace;
    
    @Value("${killrvideo.apollo.override-local-dse:false}")
    private boolean connectApollo = false;
    
    /**
     * Create a set of sentence conjunctions and other "undesirable"
     * words we will use later to exclude from search results.
     * Had to use .split() below because of the following conversation:
     * https://github.com/spring-projects/spring-boot/issues/501
     */
    @Value("#{'${killrvideo.dse.search.ignoredWords}'.split(',')}")
    private Set<String> ignoredWords = new HashSet<>();
    
    @PostConstruct
    public void init() {
        if (connectApollo) {
            LOGGER.info("Search service will use Apollo");
            dseSearchDao = new SearchDseDaoApollo();
        } else {
            dseSearchDao = new SearchDseDaoMapperBuilder(cqlSession).build().searchDao(dseKeySpace);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void searchVideos(SearchVideosRequest grpcReq, StreamObserver<SearchVideosResponse> grpcResObserver) {
        
        // Validate Parameters
        validateGrpcRequest_SearchVideos(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        // Mapping GRPC => Domain (Dao)
        String           searchQuery = grpcReq.getQuery();
        int              searchPageSize = grpcReq.getPageSize();
        Optional<String> searchPagingState = Optional.ofNullable(grpcReq.getPagingState()).filter(StringUtils::isNotBlank);
        
        // Map Result back to GRPC
        dseSearchDao
            .searchVideosAsync(searchQuery, searchPageSize,searchPagingState)
            .whenComplete((resultPage, error) -> {
              if (error == null) {
                  traceSuccess("searchVideos", starts);
                  grpcResObserver.onNext(buildSearchGrpcResponse(resultPage, grpcReq));
                  grpcResObserver.onCompleted();
                  
               } else {
                  traceError("searchVideos", starts, error);
                  grpcResObserver.onError(Status.INTERNAL.withCause(error).asRuntimeException());
               }
        });
    }
    
    private SearchVideosResponse buildSearchGrpcResponse(ResultListPage<Video> resultPage, SearchVideosRequest initialRequest) {
        final SearchVideosResponse.Builder builder = SearchVideosResponse.newBuilder();
        builder.setQuery(initialRequest.getQuery());
        resultPage.getPagingState().ifPresent(builder::setPagingState);
        resultPage.getResults().stream()
                  .map(SearchServiceGrpcMapper::maptoResultVideoPreview)
                  .forEach(builder::addVideos);
        return builder.build();
    }

    /** {@inheritDoc} */
    @Override
    public void getQuerySuggestions(GetQuerySuggestionsRequest grpcReq, StreamObserver<GetQuerySuggestionsResponse> grpcResObserver) {
        
        // Validate Parameters
        validateGrpcRequest_GetQuerySuggestions(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        // Mapping GRPC => Domain (Dao)
        String           searchQuery = grpcReq.getQuery();
        int              searchPageSize = grpcReq.getPageSize();
        
        // Invoke Dao (Async)
        dseSearchDao.getQuerySuggestionsAsync(searchQuery, searchPageSize)
                    // Mapping back to GRPC beans
                    .whenComplete((suggestionSet, error) -> {
          if (error == null) {
              traceSuccess("getQuerySuggestions", starts);
              final GetQuerySuggestionsResponse.Builder builder = GetQuerySuggestionsResponse.newBuilder();
              builder.setQuery(grpcReq.getQuery());
              suggestionSet.removeAll(ignoredWords);
              builder.addAllSuggestions(suggestionSet);
              grpcResObserver.onNext(builder.build());
              grpcResObserver.onCompleted();
          } else {
              traceError("getQuerySuggestions", starts, error);
              grpcResObserver.onError(Status.INTERNAL.withCause(error).asRuntimeException());
          }             
        });
    }
    
    /**
     * Utility to TRACE.
     *
     * @param method
     *      current operation
     * @param start
     *      timestamp for starting
     */
    private void traceSuccess(String method, Instant starts) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("End successfully '{}' in {} millis", method, Duration.between(starts, Instant.now()).getNano()/1000);
        }
    }
    
    /**
     * Utility to TRACE.
     *
     * @param method
     *      current operation
     * @param start
     *      timestamp for starting
     */
    private void traceError(String method, Instant starts, Throwable t) {
        LOGGER.error("An error occured in {} after {}", method, Duration.between(starts, Instant.now()), t);
    }

}