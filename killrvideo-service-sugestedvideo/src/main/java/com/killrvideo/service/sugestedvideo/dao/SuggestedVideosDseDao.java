package com.killrvideo.service.sugestedvideo.dao;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.QueryProvider;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import com.killrvideo.dse.dto.ResultListPage;
import com.killrvideo.dse.dto.Video;

/**
 * Operations against DSE (Cassandra Part) to implements Suggested Videos
 * 
 * @author DataStax Developer Advocates team.
 */
@Dao
public interface SuggestedVideosDseDao {
    
    @Select
    CompletionStage<Video> findVideo(UUID videoId);
    
    /**
     * Retrieve a record in table 'video' based on its PRIMARY KEY. Use query provider
     * as use in second query as well.
     *
     * @param videoId
     *      unique video identifier
     * @return
     *      expected bean
     */
    @QueryProvider(providerClass = SuggestedVideosDseDaoQueryProvider.class, 
                   entityHelpers = {Video.class})
    CompletionStage<Video> findVideoById(UUID videoId);
    
    /**
     * List related videos based on tag search.
     * 
     * @param videoId
     *      current video identifier
     * @param fetchSize
     *      page size
     * @param pagingState
     *      paging state
     * @return
     *      list of video
     */
    @QueryProvider(
            providerClass = SuggestedVideosDseDaoQueryProvider.class, 
            entityHelpers = {Video.class})
    CompletionStage< ResultListPage<Video> > getRelatedVideos(
            UUID videoId, 
            int fetchSize, 
            Optional<String> pagingState, 
            Set < String> ignoredWords);
    
}
