package killrvideo.dao.dse;

import static killrvideo.entity.Comment.COLUMN_COMMENT;
import static killrvideo.entity.Comment.COLUMN_COMMENTID;
import static killrvideo.entity.Comment.COLUMN_USERID;
import static killrvideo.entity.Comment.COLUMN_VIDEOID;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PagingState;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import killrvideo.dao.bean.QueryCommentByVideo;
import killrvideo.dao.bean.CommentListResult;
import killrvideo.dao.bean.QueryCommentByUser;
import killrvideo.entity.Comment;
import killrvideo.entity.CommentByUser;
import killrvideo.entity.CommentByVideo;
import killrvideo.entity.Schema;
import killrvideo.utils.FutureUtils;
/**
 * Implementation of queries and related to {@link Comment} objects within DataStax Enterprise.
 * Comments are store in 2 tables and all queries are performed against Apache Cassandra.
 * 
 * @author DataStax evangelist team.
 */
@Repository
public class CommentDseDao implements Schema {

    /** Loger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(CommentDseDao.class);
    
    /** Hold Connectivity to DSE. */
    @Inject
    private DseSession dseSession;
    
    /** Hold Driver Mapper to implement ORM with Cassandra. */
    @Inject
    private MappingManager manager;

    /** Map {@link ResultSet} to bean {@link CommentByVideo} through Mappers. */
    private Mapper< CommentByVideo > commentByVideoMapper;
    
    /** Map {@link ResultSet} back to bean {@link CommentByVideo} through Mappers. */
    private Mapper< CommentByUser > commentByUserMapper;
    
    /** Simplify code. */
    private String commentByVideoKeyspace;
    
    /** Simplify code. */
    private String commentByVideoTableName;
    
    /** Simplify code. */
    private String commentByUserKeyspace;
   
    /** Simplify code. */
    private String commentByUserTableName;
   
    /** PreCompiled instruction to Insert comment in table comments_by_user. */
    private PreparedStatement statementInsertCommentByUser;
    
    /** PreCompiled instruction to Insert comment in table comments_by_video. */
    private PreparedStatement statementInsertCommentByVideo;
    
    /** PreCompiled instruction  to Get all comments from a single user (Account page). */
    private PreparedStatement statementSearchAllCommentsForUser;
    
    /** PreCompiled instruction  to Get comments from a single user but with filtering on comment id. */
    private PreparedStatement statementSearchCommentsForUserWithStartingPoint;

    /** PreCompiled instruction  to Get all comments from a single video (Video Details page). */
    private PreparedStatement statementSearchAllCommentsForVideo;

    /** PreCompiled instruction  to Get comments from a single video but with filtering on comment id. */
    private PreparedStatement statementSearchCommentsForVideoWithStartingPoint;
    
    /**
     * Prepariation of statement before queries allow signifiant performance improvements.
     * This can only be done it the statement is 'static', mean the number of parameter
     * to bind() is fixed. If not the case you can find sample in method buildStatement*() in this class.
     */
    @PostConstruct
    private void initializeStatements () {
        commentByVideoMapper    = manager.mapper(CommentByVideo.class);
        commentByVideoKeyspace  = commentByVideoMapper.getTableMetadata().getKeyspace().getName();
        commentByVideoTableName = commentByVideoMapper.getTableMetadata().getName();
        commentByUserMapper     = manager.mapper(CommentByUser.class);
        commentByUserKeyspace   = commentByUserMapper.getTableMetadata().getKeyspace().getName();
        commentByUserTableName  = commentByUserMapper.getTableMetadata().getName();
        
        prepareStatementInsertCommentByUser();
        prepareStatementInsertCommentByVideo();
        prepareStatementSearchAllCommentsForUser();
        prepareStatementSearchCommentsForUserWithStartingPoint();
        prepareStatementSearchAllCommentsForVideo();
        prepareStatementSearchCommentsForVideoWithStartingPoint();
    }
    
    /**
     * Execute synchronously some operations in DSE.
     * 
     * @param comment
     *      current comment.
     */
    public void insertComment(final Comment comment) {
        dseSession.execute(buildInsertCommentBatchStatement(comment));
    }
    
    /**
     * Insert a comment for a video. (in multiple table at once). When executing query aync result will be a completable future.
     * Note the 'executeAsync'> No result are expected from insertion and we return CompletableFuture<VOID>.
     * 
     * @param comment
     *     comment to be inserted by signup user.
     */
    public CompletableFuture<Void> insertCommentAsync(final Comment comment) {
        BatchStatement stmt = buildInsertCommentBatchStatement(comment);
        CompletableFuture<Void> cfv = new CompletableFuture<>();
        Futures.addCallback(dseSession.executeAsync(stmt), new FutureCallback<ResultSet>() {
            
            // Propagation exception to handle it in the EXPOSITION LAYER. 
            public void onFailure(Throwable ex) { cfv.completeExceptionally(ex); }
            
            // Insertion return Void and we can put null in the complete
            public void onSuccess(ResultSet rs) { cfv.complete(null); } 
        });
        return cfv;
    }
    
    /**
     * Search comment_by_video Asynchronously with Pagination.
     *
     * @param videoId
     *      video unique identifier (required)
     * @param commentId
     *     comment id as offsert or starting point for the query/page 
     * @param pageSize
     *      pageable query, here is the page size
     * @param pageState
     *      provie the PagingState
     * @return
     *      list of comments for this video
     */
    public CommentListResult findCommentsByVideoId(final QueryCommentByVideo query) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Retrieving SYNCHRONOUSLY comments for video {}", query.getVideoId());
        }
        return mapToCommentByVideoResultBean(dseSession.execute(buildStatementVideoComments(query)));
    }
    
    /**
     * Search comment_by_video Asynchronously with Pagination.
     *
     * @param videoId
     *      video unique identifier (required)
     * @param commentId
     *     comment id as offsert or starting point for the query/page 
     * @param pageSize
     *      pageable query, here is the page size
     * @param pageState
     *      provie the PagingState
     * @return
     *      list of comments for this video
     */
    public CompletableFuture < CommentListResult > findCommentsByVideosIdAsync(final QueryCommentByVideo query) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Retrieving ASYNCHRONOUSLY comments for video {}", query.getVideoId());
        return FutureUtils.buildCompletableFuture(dseSession
                            .executeAsync(buildStatementVideoComments(query)))
                            .thenApplyAsync(this::mapToCommentByVideoResultBean);
    }
    
    /**
     * Execute a query against the 'comment_by_user' table.
     *
     * @param query
     *      bean holding all parameters to search.
     * @return
     *      execution result
     */
    public CommentListResult findCommentsByUserId(final QueryCommentByUser query) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Retrieving SYNCHRONOUSLY comments for user {}", query.getUserId());
        }
        return mapToCommentByVideoResultBean(dseSession.execute(buildStatementUserComments(query)));
    }
    
    /**
     * Execute a query against the 'comment_by_user' table (ASYNC).
     *
     * @param query
     *      bean holding all parameters to search.
     * @return
     *      execution result
     */
    public CompletableFuture< CommentListResult > findCommentsByUserIdAsync(final QueryCommentByUser query) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Retrieving ASYNCHRONOUSLY comments for user {}", query.getUserId());
        return FutureUtils.buildCompletableFuture(dseSession
                            .executeAsync(buildStatementUserComments(query)))
                            .thenApplyAsync(this::mapToCommentByVideoResultBean);
    }
    
    /**
     * Implementation of mapping.
     *
     * @param rs
     *      current result set.
     * @return
     *      target result
     */
    private CommentListResult mapToCommentByVideoResultBean(ResultSet rs) {
        CommentListResult result = new CommentListResult();
        for (Row row : rs) {
            Comment c = new Comment();
            c.setComment(row.getString(COLUMN_COMMENT));
            c.setUserid(row.getUUID(COLUMN_USERID));
            c.setCommentid(row.getUUID(COLUMN_COMMENTID));
            c.setVideoid(row.getUUID(COLUMN_VIDEOID));
            /**
             * Explicitly set dateOfComment because the @Computed
             * annotation set on the dateOfComment field when using QueryBuilder is not executed
             * This gives us the "proper" return object expected for the response to the front-end
             * UI.  It does not function if this value is null or not the correct type.
             * This is the reason why we did not (simply) use commentByVideoMapper.map(rs).  
             */
            c.setDateOfComment(row.getTimestamp("comment_timestamp"));
            result.getComments().add(c);
            LOGGER.debug(" + Parsing comment {}", c.getCommentid().toString());
        }
        result.setPagingState(Optional.ofNullable(rs.getExecutionInfo().getPagingState()).map(PagingState::toString));
        return result;
    }
    
    /** 
     * When inserting a comment in (multiple) Cassandra tables it is important that the same data is written to
     * both tables to keep them in synchronization. The {@link BatchStatement} ensure light transactions and minimum
     * consistency in the date.   
     **/
    private BatchStatement buildInsertCommentBatchStatement(final Comment comment) {
        final BatchStatement batchStatement = new BatchStatement(BatchStatement.Type.LOGGED);
        batchStatement.add(statementInsertCommentByUser.bind(
                comment.getUserid(), comment.getCommentid(), 
                comment.getComment(), comment.getVideoid()));
        batchStatement.add(statementInsertCommentByVideo.bind(
                comment.getVideoid(), comment.getCommentid(), 
                comment.getComment(), comment.getUserid()));
        batchStatement.setDefaultTimestamp(System.currentTimeMillis());
        return batchStatement;
    }
    
    /**
     * This statement is dynamic this is the reason why it is not implemented as a
     * {@link PreparedStatement} but simple {@link BoundStatement}.
     * 
     * @param userId
     *      user unique identifier (required)
     * @param commentId
     *     comment id as offsert or starting point for the query/page 
     * @param pageSize
     *      pageable query, here is the page size
     * @param pageState
     *      provie the PagingState
     * @return
     *      statement to retrieve comments
     */
    private BoundStatement buildStatementUserComments(final QueryCommentByUser query) {
        BoundStatement statement = null;
        if (query.getCommentId().isPresent()) {
            statement = statementSearchCommentsForUserWithStartingPoint.bind()
                            .setUUID(COLUMN_USERID, query.getUserId())
                            .setUUID(COLUMN_COMMENTID, query.getCommentId().get());
        } else {
            statement = statementSearchAllCommentsForUser.bind()
                            .setUUID(COLUMN_USERID, query.getUserId());
        }
        if (query.getPageState().isPresent() && query.getPageState().get().length() > 0) {
            statement.setPagingState(PagingState.fromString(query.getPageState().get()));
        }
        statement.setFetchSize(query.getPageSize());
        return statement;
    }
    
    /**
     * Init statement based on comment tag.
     *  
     * @param request
     *      current request
     * @return
     *      statement
     */
    private BoundStatement buildStatementVideoComments(final QueryCommentByVideo query) {
        BoundStatement statement = null;
        if (query.getCommentId().isPresent()) {
            statement = statementSearchCommentsForVideoWithStartingPoint.bind()
                        .setUUID(COLUMN_VIDEOID, query.getVideoId())
                        .setUUID(COLUMN_COMMENTID, query.getCommentId().get());
        } else {
            statement = statementSearchAllCommentsForVideo.bind()
                        .setUUID(COLUMN_VIDEOID, query.getVideoId());
        }
        if (query.getPageState().isPresent() && query.getPageState().get().length() > 0) {
            statement.setPagingState(PagingState.fromString(query.getPageState().get()));
        }
        statement.setFetchSize(query.getPageSize());
        return statement;
    }
    
    /**
     * Create static statement (prepareStatement) at startup to speed up queries. 
     * + Here we define the query as a String using CQL Syntax.
     */
    private void prepareStatementInsertCommentByUser() {
        StringBuilder queryCreateCommentByUser = new StringBuilder("INSERT INTO ");
        queryCreateCommentByUser.append(commentByUserKeyspace);
        queryCreateCommentByUser.append("." + commentByUserTableName);
        queryCreateCommentByUser.append(" (" + COLUMN_USERID + ", " + COLUMN_COMMENTID + ", ");
        queryCreateCommentByUser.append(COLUMN_COMMENT + ", " + COLUMN_VIDEOID + ")");
        queryCreateCommentByUser.append(" VALUES (?, ?, ?, ?) ");
        statementInsertCommentByUser = dseSession.prepare(queryCreateCommentByUser.toString())
                                                 .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
    
    /**
     * Create static statement (prepareStatement) at startup to speed up queries. 
     * + Here we use the QueryBuilder
     * which provide helper to build the target statement.
     */
    private void prepareStatementInsertCommentByVideo() {
        StringBuilder queryCreateCommentByVideo = new StringBuilder("INSERT INTO ");
        queryCreateCommentByVideo.append(commentByVideoKeyspace);
        queryCreateCommentByVideo.append("." + commentByVideoTableName);
        queryCreateCommentByVideo.append(" (" + COLUMN_USERID + ", " + COLUMN_COMMENTID + ", ");
        queryCreateCommentByVideo.append(COLUMN_COMMENT + ", " + COLUMN_VIDEOID + ")");
        queryCreateCommentByVideo.append(" VALUES (?, ?, ?, ?) ");
        Insert insertQuery = QueryBuilder.insertInto(commentByVideoKeyspace, commentByVideoTableName)
                    .value(COLUMN_VIDEOID, "?").value(COLUMN_COMMENTID, "?")
                    .value(COLUMN_COMMENT, "?").value(COLUMN_USERID,    "?");
        statementInsertCommentByVideo = dseSession.prepare(insertQuery.getQueryString());
        statementInsertCommentByVideo.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
    
    /**
     * Notice below I execute fcall() to pull the timestamp out of the
     * commentid timeuuid field, yet I am using the @Computed annotation
     * to do the same thing within the CommentsByUser entity for the dateOfComment
     * field.  I do this because I am using QueryBuilder for the query below.
     * @Computed is only supported when using the mapper stated per
     * http://docs.datastax.com/en/drivers/java/3.2/com/datastax/driver/mapping/annotations/Computed.html.
     * So, I essentially have 2 ways to get the timestamp out of my timeUUID column
     * depending on the type of query I am executing.
     */
    private void prepareStatementSearchAllCommentsForUser() {
        RegularStatement querySearchAllCommentsForUser = QueryBuilder
                .select()
                    .column(COLUMN_USERID).column(COLUMN_COMMENTID)
                    .column(COLUMN_VIDEOID).column(COLUMN_COMMENT)
                    .fcall("toTimestamp", QueryBuilder.column(COLUMN_COMMENTID)).as("comment_timestamp")
                .from(commentByUserKeyspace, commentByUserTableName)
                .where(QueryBuilder.eq(COLUMN_USERID, QueryBuilder.bindMarker()));
        statementSearchAllCommentsForUser = dseSession.prepare(querySearchAllCommentsForUser);
        statementSearchAllCommentsForUser.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
    
    /**
     * Building static prepareStatement in advance to speed up queries.
     */
    private void prepareStatementSearchCommentsForUserWithStartingPoint () {
        RegularStatement querySearchCommentsFoUserWithStartingPoint = QueryBuilder
                .select()
                    .column(COLUMN_USERID).column(COLUMN_COMMENTID)
                    .column(COLUMN_VIDEOID).column(COLUMN_COMMENT)
                    .fcall("toTimestamp", QueryBuilder.column(COLUMN_COMMENTID)).as("comment_timestamp")
                    .from(commentByUserKeyspace, commentByUserTableName)
                .where(QueryBuilder.eq(COLUMN_USERID, QueryBuilder.bindMarker()))
                .and(QueryBuilder.lte(COLUMN_COMMENTID, QueryBuilder.bindMarker()));
        statementSearchCommentsForUserWithStartingPoint = dseSession.prepare(querySearchCommentsFoUserWithStartingPoint)
                                                                    .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
    
    /**
     * Building static prepareStatement in advance to speed up queries.
     */
    private void prepareStatementSearchAllCommentsForVideo() {
        RegularStatement auerySearchAllCommentForvideo = QueryBuilder
                .select()
                    .column(COLUMN_VIDEOID).column(COLUMN_COMMENTID)
                    .column(COLUMN_USERID).column(COLUMN_COMMENT)
                    .fcall("toTimestamp", QueryBuilder.column(COLUMN_COMMENTID)).as("comment_timestamp")
                .from(commentByVideoKeyspace, commentByVideoTableName)
                .where(QueryBuilder.eq(COLUMN_VIDEOID, QueryBuilder.bindMarker()));
        statementSearchAllCommentsForVideo = dseSession.prepare(auerySearchAllCommentForvideo)
                                                       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
    
    /**
     * Building static prepareStatement in advance to speed up queries.
     */
    private void prepareStatementSearchCommentsForVideoWithStartingPoint() {
        RegularStatement auerySearchCommentForVideo = QueryBuilder
                .select()
                    .column(COLUMN_VIDEOID).column(COLUMN_COMMENTID)
                    .column(COLUMN_USERID).column(COLUMN_COMMENT)
                    .fcall("toTimestamp", QueryBuilder.column(COLUMN_COMMENTID)).as("comment_timestamp")
                .from(commentByVideoKeyspace, commentByVideoTableName)
                .where(QueryBuilder.eq(COLUMN_VIDEOID, QueryBuilder.bindMarker()))
                .and(QueryBuilder.lte(COLUMN_COMMENTID, QueryBuilder.bindMarker()));
        statementSearchCommentsForVideoWithStartingPoint = dseSession.prepare(auerySearchCommentForVideo)
                                                                     .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
            
}
