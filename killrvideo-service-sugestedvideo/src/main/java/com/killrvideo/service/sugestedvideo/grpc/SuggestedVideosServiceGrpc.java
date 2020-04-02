package com.killrvideo.service.sugestedvideo.grpc;

import static com.killrvideo.grpc.GrpcMappingUtils.uuidToUuid;
import static com.killrvideo.service.sugestedvideo.grpc.SuggestedVideosServiceGrpcMapper.validateGrpcRequest_getRelatedVideo;
import static com.killrvideo.service.sugestedvideo.grpc.SuggestedVideosServiceGrpcMapper.validateGrpcRequest_getUserSuggestedVideo;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
import com.killrvideo.dse.dto.Video;
import com.killrvideo.service.sugestedvideo.dao.SuggestedVideosDseDao;
import com.killrvideo.service.sugestedvideo.dao.SuggestedVideosDseDaoApollo;
import com.killrvideo.service.sugestedvideo.dao.SuggestedVideosDseDaoMapperBuilder;
import com.killrvideo.service.sugestedvideo.dao.SuggestedVideosGraphDao;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import killrvideo.common.CommonTypes.Uuid;
import killrvideo.suggested_videos.SuggestedVideoServiceGrpc.SuggestedVideoServiceImplBase;
import killrvideo.suggested_videos.SuggestedVideosService.GetRelatedVideosRequest;
import killrvideo.suggested_videos.SuggestedVideosService.GetRelatedVideosResponse;
import killrvideo.suggested_videos.SuggestedVideosService.GetSuggestedForUserRequest;
import killrvideo.suggested_videos.SuggestedVideosService.GetSuggestedForUserResponse;

/**
 * Suggested video for a user.
 *
 * @author DataStax advocates Team
 */
@Service
public class SuggestedVideosServiceGrpc extends SuggestedVideoServiceImplBase {
    
    /** Loger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(SuggestedVideosServiceGrpc.class);
     
    /** Services related to videos suggestions. */
    private SuggestedVideosDseDao suggestedVideosDseDao;
    
    @Autowired
    private CqlSession cqlSession;
    
    @Autowired
    @Qualifier("killrvideo.keyspace")
    private CqlIdentifier dseKeySpace;
    
    @Value("${killrvideo.apollo.override-local-dse:false}")
    private boolean connectApollo = false;
    
    @Autowired
    private SuggestedVideosGraphDao suggestedVideosGraphDao;
    
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
            LOGGER.info("Suggested Video service will use Apollo");
            suggestedVideosDseDao = new SuggestedVideosDseDaoApollo();
        } else {
            suggestedVideosDseDao = 
                    new SuggestedVideosDseDaoMapperBuilder(cqlSession).build().suggestedVideosDao(dseKeySpace);
        }
        
        
    }
    
    /** {@inheritDoc} */
    @Override
    public void getRelatedVideos(GetRelatedVideosRequest grpcReq, StreamObserver<GetRelatedVideosResponse> grpcResObserver) {
        
        // Validate Parameters
        validateGrpcRequest_getRelatedVideo(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        // Mapping GRPC => Domain (Dao)
        final UUID       videoId = UUID.fromString(grpcReq.getVideoId().getValue());
        int              videoPageSize = grpcReq.getPageSize();
        Optional<String> videoPagingState = Optional.ofNullable(grpcReq.getPagingState()).filter(StringUtils::isNotBlank);
        
        // Map Result back to GRPC
        suggestedVideosDseDao
                .getRelatedVideos(videoId, videoPageSize, videoPagingState, ignoredWords)
                .whenComplete((resultPage, error) -> {
            
            if (error != null ) {
                traceError("getRelatedVideos", starts, error);
                grpcResObserver.onError(Status.INTERNAL.withCause(error).asRuntimeException());
                
            } else {
                
                traceSuccess("getRelatedVideos", starts);
                Uuid videoGrpcUUID = uuidToUuid(videoId);
                final GetRelatedVideosResponse.Builder builder = 
                        GetRelatedVideosResponse.newBuilder().setVideoId(videoGrpcUUID);
                resultPage.getResults().stream()
                      .map(SuggestedVideosServiceGrpcMapper::mapVideotoSuggestedVideoPreview)
                      .filter(preview -> !preview.getVideoId().equals(videoGrpcUUID))
                      .forEach(builder::addVideos);
                resultPage.getPagingState().ifPresent(builder::setPagingState);
                grpcResObserver.onNext(builder.build());
                grpcResObserver.onCompleted();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void getSuggestedForUser(GetSuggestedForUserRequest grpcReq, StreamObserver<GetSuggestedForUserResponse> grpcResObserver) {
        
        // Validate Parameters
        validateGrpcRequest_getUserSuggestedVideo(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        // Mapping GRPC => Domain (Dao)
        final UUID userid = UUID.fromString(grpcReq.getUserId().getValue());
        
        CompletionStage< List<Video> > result;
        if (connectApollo) {
            result = new CompletableFuture<List<Video>>();
        } else {
            result = suggestedVideosGraphDao.getSuggestedVideosForUser(userid);
        }
        
        result.whenComplete((videos, error) -> {
                if (error != null ) {
                    traceError("getSuggestedForUser", starts, error);
                    grpcResObserver.onError(Status.INTERNAL.withCause(error).asRuntimeException());
                    
                } else {
                    traceSuccess("getSuggestedForUser", starts);
                    Uuid userGrpcUUID = uuidToUuid(userid);
                    final GetSuggestedForUserResponse.Builder builder = GetSuggestedForUserResponse.newBuilder().setUserId(userGrpcUUID);
                    videos.stream().map(SuggestedVideosServiceGrpcMapper::mapVideotoSuggestedVideoPreview).forEach(builder::addVideos);
                    grpcResObserver.onNext(builder.build());
                    grpcResObserver.onCompleted();
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
