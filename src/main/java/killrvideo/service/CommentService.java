package killrvideo.service;

import static java.util.UUID.fromString;
import static killrvideo.utils.ExceptionUtils.mergeStackTrace;
import static killrvideo.utils.FutureUtils.buildCompletableFuture;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PagingState;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.dse.DseSession;
import com.google.common.eventbus.EventBus;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import killrvideo.comments.CommentsServiceGrpc.CommentsServiceImplBase;
import killrvideo.comments.CommentsServiceOuterClass.CommentOnVideoRequest;
import killrvideo.comments.CommentsServiceOuterClass.CommentOnVideoResponse;
import killrvideo.comments.CommentsServiceOuterClass.GetUserCommentsRequest;
import killrvideo.comments.CommentsServiceOuterClass.GetUserCommentsResponse;
import killrvideo.comments.CommentsServiceOuterClass.GetVideoCommentsRequest;
import killrvideo.comments.CommentsServiceOuterClass.GetVideoCommentsResponse;
import killrvideo.comments.events.CommentsEvents.UserCommentedOnVideo;
import killrvideo.common.CommonTypes.TimeUuid;
import killrvideo.common.CommonTypes.Uuid;
import killrvideo.entity.CommentsByUser;
import killrvideo.entity.CommentsByVideo;
import killrvideo.entity.Schema;
import killrvideo.events.CassandraMutationError;
import killrvideo.utils.TypeConverter;
import killrvideo.validation.KillrVideoInputValidator;

/**
 * Handling Comments. Create and search by user and videos.
 *
 * @author DataStax evangelist team.
 */
@Service
public class CommentService extends CommentsServiceImplBase {

    /** Loger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(CommentService.class);
    
    @Inject
    private DseSession dseSession;
    
    @Inject
    private EventBus eventBus;

    @Inject
    private KillrVideoInputValidator validator;
   
    /** Insert comment in table comments_by_user. */
    private PreparedStatement statementInsertCommentByUser;
    
    /** Insert comment in table comments_by_video. */
    private PreparedStatement statementInsertCommentByVideo;
    
    /** Get all comments from a single user (Account page). */
    private PreparedStatement statementSearchAllCommentsForUser;
    
    /** Get comments from a single user but with filtering on comment id. */
    private PreparedStatement statementSearchCommentsForUserWithStartingPoint;

    /** Get all comments from a single video (Video Details page). */
    private PreparedStatement statementSearchAllCommentsForVideo;

    /** Get comments from a single video but with filtering on comment id. */
    private PreparedStatement statementSearchCommentsForVideoWithStartingPoint;
    
    /**
     * Preparing statement before aueries allow signifiant performance improvements.
     * This can only be done it the statement is 'static', mean the number of parameter
     * to bind() is fixed. If not the case you can find sample in method buildStatement*() in this class.
     */
    @PostConstruct
    private void initializeStatements () {
        prepareStatementInsertCommentByUser();
        prepareStatementInsertCommentByVideo();
        prepareStatementSearchAllCommentsForUser();
        prepareStatementSearchCommentsForUserWithStartingPoint();
        prepareStatementSearchAllCommentsForVideo();
        prepareStatementSearchCommentsForVideoWithStartingPoint();
    }

    /** {@inheritDoc} */
    @Override
    public void commentOnVideo(
            final CommentOnVideoRequest request, 
            StreamObserver<CommentOnVideoResponse> responseObserver) {
        
        // Parameter Validation
        Assert.isTrue(validator.isValid(request, responseObserver), "Invalid parameter for 'commentOnVideo'");
        
        /**
         * Building statement :
         * We need to insert into comments_by_user and comments_by_video simultaneously, thus using 
         * logged batch for automatic retries in case of error.
         */
        final Instant start     = Instant.now();
        final UUID    userId    = UUID.fromString(request.getUserId().getValue());
        final UUID    videoId   = UUID.fromString(request.getVideoId().getValue());
        final UUID    commentId = UUID.fromString(request.getCommentId().getValue());
        final String  comment   = request.getComment();
        final BatchStatement batchStatement = new BatchStatement(BatchStatement.Type.LOGGED);
        batchStatement.add(statementInsertCommentByUser.bind(userId, commentId, comment, videoId));
        batchStatement.add(statementInsertCommentByVideo.bind(videoId, commentId, comment, userId));
        batchStatement.setDefaultTimestamp(start.getEpochSecond());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Inserting comment on video {} for user {} : {}", videoId, userId, comment);
        }
        
        // Execute Query asynchronously and process result
        buildCompletableFuture(dseSession.executeAsync(batchStatement)).handle((rs, ex) -> {
            if (rs != null) {
                // if success post message to bus  + routing (onComplete)
                eventBus.post(UserCommentedOnVideo.newBuilder()
                        .setCommentId(request.getCommentId())
                        .setVideoId(request.getVideoId())
                        .setUserId(request.getUserId())
                        .setCommentTimestamp(TypeConverter.instantToTimeStamp(start))
                        .build());
                responseObserver.onNext(CommentOnVideoResponse.newBuilder().build());
                responseObserver.onCompleted();
                LOGGER.debug("End successfully 'commentOnVideo' in {}", System.currentTimeMillis() - start.getEpochSecond());
            } else if (ex != null) { 
                // if error post error message to bus + routing (onError)
                LOGGER.error("Exception commenting on video {} }", ex);
                eventBus.post(new CassandraMutationError(request, ex));
                responseObserver.onError(Status.INTERNAL.withCause(ex).asRuntimeException());
            }
            return rs;
        });
    }
    
    /** {@inheritDoc} */
    @Override
    public void getUserComments(
            final GetUserCommentsRequest request, 
            StreamObserver<GetUserCommentsResponse> responseObserver) {

        // Parameter Validation
        Assert.isTrue(validator.isValid(request, responseObserver), "Invalid parameter for 'getUserComments'");
        
        //  Building statement
        final Instant start = Instant.now();
        BoundStatement stmt = buildStatementUserComments(request);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Retrieving comments for user {}", request.getUserId().getValue());
        }
        
        // Execute Query asynchronously and process result
        buildCompletableFuture(dseSession.executeAsync(stmt)).handle((rs, ex) -> {
            if (rs != null) {
                final GetUserCommentsResponse.Builder builder = GetUserCommentsResponse.newBuilder();
                int totalComments = rs.getAvailableWithoutFetching();
                int remaining     = rs.getAvailableWithoutFetching();
                Iterator<Row> iterRows = rs.iterator();
                while(remaining > 0 && iterRows.hasNext()) {
                    Row row = iterRows.next(); 
                    CommentsByUser commentByUser = new CommentsByUser();
                    commentByUser.setUserid(row.getUUID("userid"));
                    commentByUser.setComment(row.getString("comment"));
                    commentByUser.setCommentid(row.getUUID("commentid"));
                    commentByUser.setVideoid(row.getUUID("videoid"));
                    commentByUser.setDateOfComment(row.getTimestamp("comment_timestamp"));
                    builder.addComments(commentByUser.toUserComment());
                    remaining--;
                }
                responseObserver.onNext(builder.build());
                responseObserver.onCompleted();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{} comments successfully retrieved in {} millis.", totalComments, System.currentTimeMillis() - start.getEpochSecond());
                }
               } else if (ex != null) {
                   LOGGER.error("Error during user comments query", ex);
                   eventBus.post(new CassandraMutationError(request, ex));
                   responseObserver.onError(Status.INTERNAL.withCause(ex).asRuntimeException());
               }
           
            return rs;
        });
    }

    /** {@inheritDoc} */
    @Override
    public void getVideoComments(
                final GetVideoCommentsRequest request, 
                StreamObserver<GetVideoCommentsResponse> responseObserver) {

        // Parameter Validation
        LOGGER.debug("Starting 'getVideoComments'");
        Assert.isTrue(validator.isValid(request, responseObserver), "Invalid parameter for 'getVideoComments'");
        
        // Building statement
        BoundStatement statement = buildStatementVideoComments(request);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Retrieving comments for videos {}", request.getVideoId().getValue());
        }
        
        // Execute Query asynchronously and process result
        buildCompletableFuture(dseSession.executeAsync(statement)).handle((commentResult, ex) -> {
                    try {
                        if (commentResult != null) {
                            final GetVideoCommentsResponse.Builder builder = GetVideoCommentsResponse.newBuilder();

                            int remaining = commentResult.getAvailableWithoutFetching();
                            for (Row row : commentResult) {
                                CommentsByVideo commentByVideo = new CommentsByVideo(
                                        row.getUUID("videoid"), row.getUUID("commentid"),
                                        row.getUUID("userid"), row.getString("comment")
                                );

                                /**
                                 * Explicitly set dateOfComment because I cannot use the @Computed
                                 * annotation set on the dateOfComment field when using QueryBuilder.
                                 * This gives us the "proper" return object expected for the response to the front-end
                                 * UI.  It does not function if this value is null or not the correct type.
                                 */
                                commentByVideo.setDateOfComment(row.getTimestamp("comment_timestamp"));
                                builder.addComments(commentByVideo.toVideoComment());

                                if (--remaining == 0) {
                                    break;
                                }
                            }

                            Optional.ofNullable(commentResult.getExecutionInfo().getPagingState())
                                    .map(PagingState::toString)
                                    .ifPresent(builder::setPagingState);
                            responseObserver.onNext(builder.build());
                            responseObserver.onCompleted();

                            LOGGER.debug("End get video comments request");

                        } else if (ex != null) {
                            LOGGER.error("Exception getting video comments : " + mergeStackTrace(ex));

                            responseObserver.onError(Status.INTERNAL.withCause(ex).asRuntimeException());
                        }

                    } catch (Exception exception) {
                        LOGGER.error("CATCH Exception getting video comments : " + mergeStackTrace(exception));

                    }
                    return commentResult;
                });
    }
    
    /**
     * Init statement based on comment tag.
     *  
     * @param request
     *      current request
     * @return
     *      statement
     */
    private BoundStatement buildStatementUserComments(GetUserCommentsRequest request) {
        BoundStatement statement = null;
        if (null != request.getStartingCommentId() | isBlank(request.getStartingCommentId().getValue())) {
            LOGGER.debug("Query without startingCommentId");
            statement = statementSearchAllCommentsForUser
                        .bind()
                        .setUUID("userid", fromString(request.getUserId().getValue()));
        } else {            
            /**
             * Subsequent requests always provide startingCommentId to load page
             * of user comments. Fetch size/page size is expected to be > 1
             */
            LOGGER.debug("Query WITH startingCommentId");
            statement = statementSearchCommentsForUserWithStartingPoint
                        .bind()
                        .setUUID("userid", fromString(request.getUserId().getValue()))
                        .setUUID("commentid", fromString(request.getStartingCommentId().getValue()));
        }
        statement.setFetchSize(request.getPageSize());
        if (org.springframework.util.StringUtils.hasLength(request.getPagingState())) {
            statement.setPagingState(PagingState.fromString(request.getPagingState()));
        }
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
    private BoundStatement buildStatementVideoComments(GetVideoCommentsRequest request) {
        BoundStatement statement         = null;
        final TimeUuid startingCommentId = request.getStartingCommentId();
        final Uuid     videoId           = request.getVideoId();
       
        /**
         * Query without startingCommentId to get a reference point
         * Normally, the requested fetch size/page size is 1 to get
         * the first video comment as reference point
         */
        if (startingCommentId == null || isBlank(startingCommentId.getValue())) {
            LOGGER.debug("Query without startingCommentId");
            statement = statementSearchAllCommentsForVideo.bind()
                    .setUUID("videoid", fromString(videoId.getValue()));
        }
        /**
         * Subsequent requests always provide startingCommentId to load page
         * of video comments. Fetch size/page size is expected to be > 1
         */
        else {
            LOGGER.debug("Query WITH startingCommentId");
            statement = statementSearchCommentsForVideoWithStartingPoint.bind()
                    .setUUID("videoid", fromString(videoId.getValue()))
                    .setUUID("commentid", fromString(startingCommentId.getValue()));
        }
        statement.setFetchSize(request.getPageSize());
        if (org.springframework.util.StringUtils.hasLength(request.getPagingState())) {
            statement.setPagingState(PagingState.fromString(request.getPagingState()));
        }
        return statement;
    }
        
    /**
     * Building static prepareStatement in advance to speed up queries.
     */
    private void prepareStatementInsertCommentByUser() {
        StringBuilder queryCreateCommentByUser = new StringBuilder("INSERT INTO ");
        queryCreateCommentByUser.append(Schema.KEYSPACE + "." + Schema.TABLENAME_COMMENTS_BY_USER);
        queryCreateCommentByUser.append(" (userid, commentid, comment, videoid)");
        queryCreateCommentByUser.append(" VALUES (?, ?, ?, ?) ");
        statementInsertCommentByUser = dseSession.prepare(queryCreateCommentByUser.toString())
                                                 .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
    
    /**
     * Building static prepareStatement in advance to speed up queries.
     */
    private void prepareStatementInsertCommentByVideo() {
        StringBuilder queryCreateCommentByVideo = new StringBuilder("INSERT INTO ");
        queryCreateCommentByVideo.append(Schema.KEYSPACE + "." + Schema.TABLENAME_COMMENTS_BY_VIDEO);
        queryCreateCommentByVideo.append(" (videoid, commentid, comment, userid)");
        queryCreateCommentByVideo.append(" VALUES (?, ?, ?, ?) ");
        statementInsertCommentByVideo = dseSession.prepare(queryCreateCommentByVideo.toString())
                                                  .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
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
        RegularStatement querySearchAllCommentsForUser = QueryBuilder.select()
                        .column("userid").column("commentid").column("videoid").column("comment")
                        .fcall("toTimestamp", QueryBuilder.column("commentid")).as("comment_timestamp")
                        .from(Schema.KEYSPACE, Schema.TABLENAME_COMMENTS_BY_USER )
                        .where(QueryBuilder.eq("userid", QueryBuilder.bindMarker()));
        statementSearchAllCommentsForUser = dseSession.prepare(querySearchAllCommentsForUser)
                                                      .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
    
    /**
     * Building static prepareStatement in advance to speed up queries.
     */
    private void prepareStatementSearchCommentsForUserWithStartingPoint () {
        RegularStatement querySearchCommentsFoUserWithStartingPoint = QueryBuilder.select()
                .column("userid").column("commentid").column("videoid").column("comment")
                .fcall("toTimestamp", QueryBuilder.column("commentid")).as("comment_timestamp")
                .from(Schema.KEYSPACE, Schema.TABLENAME_COMMENTS_BY_USER )
                .where(QueryBuilder.eq("userid", QueryBuilder.bindMarker()))
                .and(QueryBuilder.lte("commentid", QueryBuilder.bindMarker()));
        statementSearchCommentsForUserWithStartingPoint = dseSession.prepare(querySearchCommentsFoUserWithStartingPoint)
                                                                    .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
    
    /**
     * Building static prepareStatement in advance to speed up queries.
     */
    private void prepareStatementSearchAllCommentsForVideo() {
        RegularStatement auerySearchAllCommentForvideo = QueryBuilder.select()
                .column("videoid").column("commentid").column("userid").column("comment")
                .fcall("toTimestamp", QueryBuilder.column("commentid")).as("comment_timestamp")
                .from(Schema.KEYSPACE, Schema.TABLENAME_COMMENTS_BY_VIDEO)
                .where(QueryBuilder.eq("videoid", QueryBuilder.bindMarker()));
        statementSearchAllCommentsForVideo = dseSession.prepare(auerySearchAllCommentForvideo)
                                                       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
    
    /**
     * Building static prepareStatement in advance to speed up queries.
     */
    private void prepareStatementSearchCommentsForVideoWithStartingPoint() {
        RegularStatement auerySearchCommentForVideo = QueryBuilder.select()
                .column("videoid").column("commentid").column("userid").column("comment")
                .fcall("toTimestamp", QueryBuilder.column("commentid")).as("comment_timestamp")
                .from(Schema.KEYSPACE, Schema.TABLENAME_COMMENTS_BY_VIDEO)
                .where(QueryBuilder.eq("videoid", QueryBuilder.bindMarker()))
                .and(QueryBuilder.lte("commentid", QueryBuilder.bindMarker()));
        statementSearchCommentsForVideoWithStartingPoint = dseSession.prepare(auerySearchCommentForVideo)
                .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
    }
}
