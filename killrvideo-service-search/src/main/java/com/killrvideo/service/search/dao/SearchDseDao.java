package com.killrvideo.service.search.dao;

import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletionStage;

import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.QueryProvider;
import com.killrvideo.dse.dto.ResultListPage;
import com.killrvideo.dse.dto.Video;

/**
 * Implementing
 * 
 * @author DataStax Developer Advocates team.
 */
@Dao
public interface SearchDseDao {
    
    /**
     * Search for video in synchronous manner.
     *
     * @param query
     *      current query
     * @param fetchSize
     *      fetch size
     * @param pagingState
     *      optional paging state
     * @return
     *      result
     */
    @QueryProvider(providerClass = SearchDseDaoQueryProvider.class, entityHelpers = {Video.class})
    ResultListPage<Video> searchVideos(String query, int fetchSize, Optional<String> pagingState);
    
    /**
     * Do a Solr query against DSE search to find videos using Solr's ExtendedDisMax query parser. Query the
     * name, tags, and description fields in the videos table giving a boost to matches in the name and tags
     * fields as opposed to the description field
     * More info on ExtendedDisMax: http://wiki.apache.org/solr/ExtendedDisMax
     *
     * Notice the "paging":"driver" parameter.  This is to ensure we dynamically
     * enable pagination regardless of our nodes dse.yaml setting.
     * https://docs.datastax.com/en/dse/5.1/dse-dev/datastax_enterprise/search/cursorsDeepPaging.html#cursorsDeepPaging__srchCursorCQL
     */
    @QueryProvider(providerClass = SearchDseDaoQueryProvider.class, entityHelpers = {Video.class})
    CompletionStage < ResultListPage<Video> > searchVideosAsync(String query, int fetchSize, Optional<String> pagingState);
    
    /**
     * Search for tags starting with provided query string (ASYNC).
     *
     * @param query
     *      pattern
     * @param fetchSize
     *      numbner of results to retrieve
     * @return
     */
     @QueryProvider(providerClass = SearchDseDaoQueryProvider.class, entityHelpers = {Video.class})
     CompletionStage < TreeSet< String > > getQuerySuggestionsAsync(String query, int fetchSize);
}
