package com.killrvideo.service.sugestedvideo.dao;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom;
import static com.datastax.oss.driver.api.querybuilder.relation.Relation.column;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.mapper.MapperContext;
import com.datastax.oss.driver.api.mapper.annotations.QueryProvider;
import com.datastax.oss.driver.api.mapper.entity.EntityHelper;
import com.datastax.oss.protocol.internal.util.Bytes;
import com.killrvideo.conf.DriverConfigurationFile;
import com.killrvideo.dse.dao.DseSchema;
import com.killrvideo.dse.dto.ResultListPage;
import com.killrvideo.dse.dto.Video;

/**
 * Implementation of specific queries for {@link SuggestedVideosDseDaos} interface.
 *
 * @author DataStax Developer Advocates team.
 */
public class SuggestedVideosDseDaoQueryProvider implements DseSchema {
    
    /** Logger for the class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SuggestedVideosDseDaoQueryProvider.class);
    
    /**
     * Wrap search queries with "paging":"driver" to dynamically enable
     * paging to ensure we pull back all available results in the application.
     * https://docs.datastax.com/en/dse/6.0/cql/cql/cql_using/search_index/cursorsDeepPaging.html#cursorsDeepPaging__using-paging-with-cql-solr-queries-solrquery-Rim2GsbY
     */
    private String pagingDriverStart = "{\"q\":\"";
    private String pagingDriverEnd = "\", \"paging\":\"driver\"}";
    
    private final CqlSession dseSession;
    
    private EntityHelper<Video> entityHelperVideo;
    
    private PreparedStatement psSelectVideoById;
    private PreparedStatement psSearchRelatedVideos;
    
    /**
     * Constructor invoked by the DataStax driver based on Annotation {@link QueryProvider} 
     * set on class {@link SuggestedVideosDseDao}.
     * 
     * @param context
     *      context to extrat dse session
     */
    public SuggestedVideosDseDaoQueryProvider(MapperContext context, EntityHelper<Video> entityHelper) {
        this.dseSession  = context.getSession();
        this.entityHelperVideo = entityHelper;
        psSelectVideoById = dseSession.prepare(
                selectFrom(TABLENAME_VIDEOS).all()
                .where(column(COLUMN_PLAYBACK_VIDEOID_).isEqualTo(bindMarker(COLUMN_PLAYBACK_VIDEOID_))).build());
        psSearchRelatedVideos = dseSession.prepare(
                selectFrom(TABLENAME_VIDEOS).all()
                .where(column(SOLR_QUERY).isEqualTo(bindMarker(SOLR_QUERY))).build());
    }
    
    public CompletionStage<Video> findVideoById(UUID videoId) {
        BoundStatement bs = psSelectVideoById.bind().setUuid(COLUMN_PLAYBACK_VIDEOID_, videoId);
        LOGGER.debug("Find video by its id {} in table 'videos'", videoId);
        return dseSession.executeAsync(bs)
                         .thenApply(ars -> ars.map(entityHelperVideo::get).currentPage().iterator().next());
    }
    
    /** {@inheritDoc} from {@link SuggestedVideosDseDao} */
    public CompletionStage< ResultListPage<Video> > getRelatedVideos(
            UUID videoId, int fetchSize, Optional<String> pagingState, Set < String> ignoredWords) {
        LOGGER.debug("Find videos related to id {}", videoId);
        return this.findVideoById(videoId)
                   .thenCompose(video -> dseSession.executeAsync(_bindStmtSearchVideos(video, fetchSize, pagingState, ignoredWords)))
                   .thenApply(ars -> ars.map(entityHelperVideo::get))
                   .thenApply(ResultListPage::new);
    }
    
    /**
     * Perform a query using DSE Search to find other videos that are similar
     * to the "request" video using terms parsed from the name, tags,
     * and description columns of the "request" video.
     *
     * The regex below will help us parse out individual words that we add to our
     * set. The set will automatically handle any duplicates that we parse out.
     * We can then use the end result termSet to query across the name, tags, and
     * description columns to find similar videos.
     */
    private BoundStatement _bindStmtSearchVideos(Video video, int fetchSize, 
            Optional<String> pagingState, Set < String> ignoredWords) {
        final String space = " ";
        final String eachWordRegEx = "[^\\w]";
        final String eachWordPattern = Pattern.compile(eachWordRegEx).pattern();

        final HashSet<String> termSet = new HashSet<>(50);
        Collections.addAll(termSet, video.getName().toLowerCase().split(eachWordPattern));
        Collections.addAll(video.getTags()); // getTags already returns a set
        Collections.addAll(termSet, video.getDescription().toLowerCase().split(eachWordPattern));
        termSet.removeAll(ignoredWords);
        termSet.removeIf(String::isEmpty);
        
        final String delimitedTermList = termSet.stream().map(Object::toString).collect(Collectors.joining(","));
        LOGGER.debug("delimitedTermList is : " + delimitedTermList);
       
        final StringBuilder solrQuery = new StringBuilder();
        solrQuery.append(pagingDriverStart);
        solrQuery.append("name:(").append(delimitedTermList).append(")^2").append(space);
        solrQuery.append("tags:(").append(delimitedTermList).append(")^4").append(space);
        solrQuery.append("description:").append(delimitedTermList);
        solrQuery.append(pagingDriverEnd);
        BoundStatement stmt = psSearchRelatedVideos.bind()
                    .setString(SOLR_QUERY, solrQuery.toString())
                    .setPageSize(fetchSize)
                    .setExecutionProfileName(DriverConfigurationFile.EXECUTION_PROFILE_SEARCH);
        if (pagingState.isPresent()) {
            stmt = stmt.setPagingState(Bytes.fromHexString(pagingState.get()));
        }
        return stmt;
    }
}
