package com.killrvideo.service.search.dao;

import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.killrvideo.dse.dto.ResultListPage;
import com.killrvideo.dse.dto.Video;

/**
 * Dedicated Implementation of Search when Solr is not Available
 * (easier to isolate dedicated implementation);
 */
public class SearchDseDaoApollo implements SearchDseDao {

    /** {@inheritDoc} */
    @Override
    public ResultListPage<Video> searchVideos(String query, int fetchSize, Optional<String> pagingState) {
        return new ResultListPage<>();
    }

    /** {@inheritDoc} */
    @Override
    public CompletionStage<ResultListPage<Video>> searchVideosAsync(String query, int fetchSize, Optional<String> pagingState) {
        return new CompletableFuture<ResultListPage<Video>>();
    }

    /** {@inheritDoc} */
    @Override
    public CompletionStage<TreeSet<String>> getQuerySuggestionsAsync(String query, int fetchSize) {
        return new CompletableFuture<TreeSet<String>>();
    }
    
}
