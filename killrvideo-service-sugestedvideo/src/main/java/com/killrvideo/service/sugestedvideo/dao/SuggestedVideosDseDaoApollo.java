package com.killrvideo.service.sugestedvideo.dao;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.killrvideo.dse.dto.ResultListPage;
import com.killrvideo.dse.dto.Video;

/**
 *
 * @author Cedrick LUNVEN (@clunven)
 *
 */
public class SuggestedVideosDseDaoApollo implements SuggestedVideosDseDao {

    /** {@inheritDoc} */
    @Override
    public CompletionStage<Video> findVideo(UUID videoId) {
        return new CompletableFuture<Video>();
    }

    /** {@inheritDoc} */
    @Override
    public CompletionStage<Video> findVideoById(UUID videoId) {
        return new CompletableFuture<Video>();
    }

    /** {@inheritDoc} */
    @Override
    public CompletionStage<ResultListPage<Video>> getRelatedVideos(UUID videoId, int fetchSize, 
            Optional<String> pagingState,
            Set<String> ignoredWords) {
        return new CompletableFuture<ResultListPage<Video>>();
    }

}
