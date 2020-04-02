package com.killrvideo.service.search.dao;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom;
import static com.datastax.oss.driver.api.querybuilder.relation.Relation.column;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.mapper.MapperContext;
import com.datastax.oss.driver.api.mapper.annotations.QueryProvider;
import com.datastax.oss.driver.api.mapper.entity.EntityHelper;
import com.datastax.oss.protocol.internal.util.Bytes;
import com.killrvideo.conf.DriverConfigurationFile;
import com.killrvideo.dse.dao.DseSchema;
import com.killrvideo.dse.dto.ResultListPage;
import com.killrvideo.dse.dto.Video;

/**
 * Implementation of specific queries for {@link SearchDseDao} interface.
 *
 * @author DataStax Developer Advocates team.
 */
public class SearchDseDaoQueryProvider implements DseSchema, SearchDseDao {

    /** Logger for the class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchDseDao.class);
    
    /**
     * Wrap search queries with "paging":"driver" to dynamically enable
     * paging to ensure we pull back all available results in the application.
     * https://docs.datastax.com/en/dse/6.0/cql/cql/cql_using/search_index/cursorsDeepPaging.html#cursorsDeepPaging__using-paging-with-cql-solr-queries-solrquery-Rim2GsbY
     */
    final private String pagingDriverStart = "{\"q\":\"";
    final private String pagingDriverEnd = "\", \"paging\":\"driver\"}";
    
    private final CqlSession dseSession;
    
    private EntityHelper<Video> mapperVideo;
    
    private PreparedStatement psFindSuggestedTags;
    
    private PreparedStatement psFindVideosByTags;
    
    /**
     * Constructor invoked by the DataStax driver based on Annotation {@link QueryProvider} 
     * set on class {@link SearchDseDao}.
     * 
     * @param context
     *      context to extrat dse session
     */
    public SearchDseDaoQueryProvider(MapperContext context, EntityHelper<Video> entityHelper) {
        this.dseSession  = context.getSession();
        this.mapperVideo = entityHelper;
        
        this.psFindSuggestedTags = dseSession.prepare(
                selectFrom(TABLENAME_VIDEOS)
                    .column(VIDEOS_COLUMN_NAME_)
                    .column(VIDEOS_COLUMN_TAGS_)
                    .column(VIDEOS_COLUMN_DESCRIPTION_)
                    .where(column(SOLR_QUERY).isEqualTo(bindMarker(SOLR_QUERY))).build());
        
        this.psFindVideosByTags = dseSession.prepare(
                selectFrom(TABLENAME_VIDEOS).all()
                    .where(column(SOLR_QUERY).isEqualTo(bindMarker(SOLR_QUERY))).build());
    }
   
    /** {@inheritDoc} from {@link SearchDseDao} */
    public ResultListPage<Video> searchVideos(String query, int fetchSize, Optional<String> pagingState) {
        BoundStatement bs = _bindStmtSearchVideos(query, fetchSize, pagingState);
        return new ResultListPage<>(dseSession.execute(bs).map(mapperVideo::get));
    }

    /** {@inheritDoc} from {@link SearchDseDao} */
    public CompletionStage<ResultListPage<Video>> searchVideosAsync(String query, int fetchSize, Optional<String> pagingState) {
        BoundStatement bs = _bindStmtSearchVideos(query, fetchSize, pagingState);
        return dseSession.executeAsync(bs)
                         .thenApply(ars -> ars.map(mapperVideo::get))
                         .thenApply(ResultListPage::new);
    }

    /** {@inheritDoc} from {@link SearchDseDao} */
    public CompletionStage<TreeSet<String>> getQuerySuggestionsAsync(String query, int fetchSize) {
        BoundStatement stmt = _bindStmtSuggestedTags(query, fetchSize);
        return dseSession.executeAsync(stmt)
                         .thenApplyAsync(rs -> _mapTagSetAsync(rs, query));
    }
    
    /**
     * Cassandra and DSE Session are stateless. For each request a coordinator is chosen
     * and execute query against the cluster. The Driver is stateful, it has to maintain some
     * network connections pool, here we properly cleanup things.
     * 
     * @throws Exception
     *      error on cleanup.
     */
    @Override
    protected void finalize() throws Throwable {
        if (dseSession  != null && !dseSession.isClosed()) {
            dseSession.close();
        }
    }
    
    /**
     * In this case we are using DSE Search to query across the name, tags, and
     * description columns with a boost on name and tags.  Note that tags is a
     * collection of tags per each row with no extra steps to include all data
     * in the collection.
     * 
     * This is a more comprehensive search as
     * we are not just looking at values within the tags column, but also looking
     * across the other fields for similar occurrences.  This is especially helpful
     * if there are no tags for a given video as it is more likely to give us results.
     */
    private BoundStatement _bindStmtSearchVideos(String query, int fetchSize, Optional<String> pagingState) {
        LOGGER.debug("Start searching videos by name, tag, and description");
        
        /**
         * Perform a query using DSE search to find videos. Query the name, tags, and description columns 
         * in the videos table giving a boost to matches in the name and tags columns as opposed to the 
         * description column.s
         */
        final String replaceFind = " ";
        final String replaceWith = " AND ";
        String requestQuery = query.trim()
                                   .replaceAll(replaceFind, Matcher.quoteReplacement(replaceWith));
        
        /**
         * In this case we are using DSE Search to query across the name, tags, and
         * description columns with a boost on name and tags.  The boost will put
         * more priority on the name column, then tags, and finally description.
         *
         * Note that tags is a
         * collection of tags per each row with no extra steps to include all data
         * in the collection.  This is a more comprehensive search as
         * we are not just looking at values within the tags column, but also looking
         * across the other columns for similar occurrences.  This is especially helpful
         * if there are no tags for a given video as it is more likely to give us results.
         *
         * Refer to the following documentation for a deeper look at term boosting:
         * https://docs.datastax.com/en/dse/6.0/cql/cql/cql_using/search_index/advancedTerms.html
         */
        final StringBuilder solrQuery = new StringBuilder()
                .append(pagingDriverStart)
                .append("name:(").append(requestQuery).append("*)^4 OR ")
                .append("tags:(").append(requestQuery).append("*)^2 OR ")
                .append("description:(").append(requestQuery).append("*)")
                .append(pagingDriverEnd);
        
        BoundStatement stmt = psFindVideosByTags.bind()
                    .setString(SOLR_QUERY, solrQuery.toString())
                    .setPageSize(fetchSize)
                    .setExecutionProfileName(DriverConfigurationFile.EXECUTION_PROFILE_SEARCH);
        if (pagingState.isPresent()) {
            stmt = stmt.setPagingState(Bytes.fromHexString(pagingState.get()));
        }
        LOGGER.debug("searchVideos() executed query is : " + stmt.getPreparedStatement().getQuery());
        LOGGER.debug("searchVideos() solr_query ? is : "   + solrQuery);
        return stmt;
    }
    
    /**
     * Do a query against DSE search to find query suggestions using a simple search.
     * The search_suggestions "column" references a field we created in our search index
     * to store name and tag data.
     *
     * Notice the "paging":"driver" parameter.  This is to ensure we dynamically
     * enable pagination regardless of our nodes dse.yaml setting.
     * https://docs.datastax.com/en/dse/5.1/dse-dev/datastax_enterprise/search/cursorsDeepPaging.html#cursorsDeepPaging__srchCursorCQL
     */
    private BoundStatement _bindStmtSuggestedTags(String query, int fetchSize) {
        
        final StringBuilder solrQuery = new StringBuilder()
                 .append(pagingDriverStart)
                 .append("name:(").append(query).append("*) OR ")
                 .append("tags:(").append(query).append("*) OR ")
                 .append("description:(").append(query).append("*)")
                 .append(pagingDriverEnd);        
        LOGGER.debug("getQuerySuggestions() solr_query is : {}", solrQuery.toString());
        
        BoundStatement stmt =  psFindSuggestedTags.bind()
                .setString(SOLR_QUERY, solrQuery.toString())
                .setPageSize(fetchSize)
                .setExecutionProfileName(DriverConfigurationFile.EXECUTION_PROFILE_SEARCH);
        LOGGER.debug("getQuerySuggestions() full query is : {}", stmt.getPreparedStatement().getQuery());
        return stmt;
    }
    
    private TreeSet < String > _mapTagSetAsync(AsyncResultSet rs, String requestQuery) {
        final Pattern checkRegex = Pattern.compile("(?i)\\b" + requestQuery + "[a-z]*\\b");
        TreeSet< String > suggestionSet = new TreeSet<>();
        _addTagSetCurrentPage(suggestionSet, checkRegex, rs.currentPage());
        LOGGER.debug("TagSet resturned are {}", suggestionSet);
        return suggestionSet;
    }
    
    /**
     * Since I simply want matches from both the name and tags fields
     * concatenate them together, apply regex, and add any results into
     * our suggestionSet TreeSet.  The TreeSet will handle any duplicates.
     */
    private void _addTagSetCurrentPage(TreeSet < String > suggestionSet, Pattern checkRegex , Iterable<Row> rs) {
        for (Row row : rs) {
            String name = row.getString(VIDEOS_COLUMN_NAME_);
            Set<String> tags = row.getSet(VIDEOS_COLUMN_TAGS, String.class);
            Matcher regexMatcher = checkRegex.matcher(name.concat(tags.toString()));
            while (regexMatcher.find()) {
                suggestionSet.add(regexMatcher.group().toLowerCase());
            }
        }
    }
}
