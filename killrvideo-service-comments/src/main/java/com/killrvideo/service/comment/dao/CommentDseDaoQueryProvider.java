package com.killrvideo.service.comment.dao;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom;
import static com.datastax.oss.driver.api.querybuilder.relation.Relation.column;
import static com.killrvideo.dse.utils.DseUtils.bind;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.mapper.MapperContext;
import com.datastax.oss.driver.api.mapper.annotations.QueryProvider;
import com.datastax.oss.driver.api.mapper.entity.EntityHelper;
import com.datastax.oss.driver.api.querybuilder.select.Selector;
import com.killrvideo.dse.dao.DseSchema;
import com.killrvideo.dse.dto.ResultListPage;
import com.killrvideo.service.comment.dao.dto.Comment;
import com.killrvideo.service.comment.dao.dto.CommentByUserEntity;
import com.killrvideo.service.comment.dao.dto.CommentByVideoEntity;
import com.killrvideo.service.comment.grpc.dto.QueryCommentByUser;
import com.killrvideo.service.comment.grpc.dto.QueryCommentByVideo;

/**
 * Query implementation for Comment Dse and Mapper.
 *
 * @author DataStax Developer Advocates team.
 */
public class CommentDseDaoQueryProvider implements CommentDseDao, DseSchema {

    private final CqlSession cqlSession;
    
    private final EntityHelper<CommentByUserEntity>  entityHelperCommentsByUser;
    private final EntityHelper<CommentByVideoEntity> entityHelperCommentsByVideo;
    
    private PreparedStatement psInsertCommentUser;
    private PreparedStatement psDeleteCommentUser;
    private PreparedStatement psInsertCommentVideo;
    private PreparedStatement psDeleteCommentVideo;
    
    /** Precompile statements to speed up queries. */
    private PreparedStatement psFindCommentsByUser;
    private PreparedStatement psFindCommentsByUserPageable;
    private PreparedStatement psFindCommentsByVideo;
    private PreparedStatement psFindCommentsByVideoPageable;
    
    /**
     * Constructor invoked by the DataStax driver based on Annotation {@link QueryProvider} 
     * set on class {@link CommentDseDao}.
     * 
     * @param context
     *      context to extrat dse session
     * @param helperUser
     *      entity helper to interact with bean {@link CommentByUserEntity}
     * @param helperVideo
     *      entity helper to interact with bean {@link CommentByVideoEntity}
     */
    public CommentDseDaoQueryProvider(MapperContext context,
            EntityHelper<CommentByUserEntity> helperUser,
            EntityHelper<CommentByVideoEntity> helperVideo) {
        
        this.cqlSession                    = context.getSession();
        this.entityHelperCommentsByUser    = helperUser;
        this.entityHelperCommentsByVideo   = helperVideo;
        
        this.psFindCommentsByUser          = cqlSession.prepare(
                selectFrom(TABLENAME_COMMENTS_BY_USER_)
                .column(COMMENTS_COLUMN_USERID).column(COMMENTS_COLUMN_COMMENTID)
                .column(COMMENTS_COLUMN_VIDEOID).column(COMMENTS_COLUMN_COMMENT)
                .function("toTimestamp", Selector.column(COMMENTS_COLUMN_COMMENTID)).as("comment_timestamp")
                .where(column(COMMENTS_COLUMN_USERID).isEqualTo(bindMarker(COMMENTS_COLUMN_USERID)))
                .build());
        
        this.psFindCommentsByUserPageable  = cqlSession.prepare(
                selectFrom(TABLENAME_COMMENTS_BY_USER_)
                .column(COMMENTS_COLUMN_USERID).column(COMMENTS_COLUMN_USERID)
                .column(COMMENTS_COLUMN_VIDEOID).column(COMMENTS_COLUMN_COMMENT)
                .column(COMMENTS_COLUMN_COMMENTID)
                .function("toTimestamp", Selector.column(COMMENTS_COLUMN_COMMENTID)).as("comment_timestamp")
                .where(column(COMMENTS_COLUMN_USERID).isEqualTo(bindMarker(COMMENTS_COLUMN_USERID)),
                       column(COMMENTS_COLUMN_COMMENTID).isLessThanOrEqualTo(bindMarker(COMMENTS_COLUMN_COMMENTID)))
                .build());
        
        this.psFindCommentsByVideo         = cqlSession.prepare(
                selectFrom(TABLENAME_COMMENTS_BY_VIDEO)
                .column(COMMENTS_COLUMN_USERID).column(COMMENTS_COLUMN_USERID)
                .column(COMMENTS_COLUMN_VIDEOID).column(COMMENTS_COLUMN_COMMENT)
                .column(COMMENTS_COLUMN_COMMENTID)
                .function("toTimestamp", Selector.column(COMMENTS_COLUMN_COMMENTID)).as("comment_timestamp")
                .where(column(COMMENTS_COLUMN_VIDEOID).isEqualTo(bindMarker(COMMENTS_COLUMN_VIDEOID)))
                .build());
        
        this.psFindCommentsByVideoPageable = cqlSession.prepare(
                selectFrom(TABLENAME_COMMENTS_BY_VIDEO)
                .column(COMMENTS_COLUMN_USERID).column(COMMENTS_COLUMN_USERID)
                .column(COMMENTS_COLUMN_VIDEOID).column(COMMENTS_COLUMN_COMMENT)
                .column(COMMENTS_COLUMN_COMMENTID)
                .function("toTimestamp", Selector.column(COMMENTS_COLUMN_COMMENTID)).as("comment_timestamp")
                .where(column(COMMENTS_COLUMN_VIDEOID).isEqualTo(bindMarker(COMMENTS_COLUMN_VIDEOID)),
                       column(COMMENTS_COLUMN_COMMENTID).isLessThanOrEqualTo(bindMarker(COMMENTS_COLUMN_COMMENTID)))
                .build());
        
        this.psInsertCommentUser  = cqlSession.prepare(helperUser.insert().asCql());
        this.psDeleteCommentUser  = cqlSession.prepare(helperUser.deleteByPrimaryKey().asCql());
        this.psInsertCommentVideo = cqlSession.prepare(helperVideo.insert().asCql());
        this.psDeleteCommentVideo = cqlSession.prepare(helperVideo.deleteByPrimaryKey().asCql());
    }
    
    /** Javadoc in {@link CommentDseDao} */
    public void upsert(Comment comment) {
        cqlSession.execute(_buildStatementInsertComment(comment));
    }
    
    /** Javadoc in {@link CommentDseDao} */
    public CompletionStage<Void> upsertAsync(Comment comment) {
        return cqlSession.executeAsync(_buildStatementInsertComment(comment))
                         .thenApply(rs -> null);
    }
    
    /** Javadoc in {@link CommentDseDao} */
    public void delete(Comment comment) {
        CommentByUserEntity  c1 = new CommentByUserEntity();
        c1.setUserid(comment.getUserid());
        c1.setCommentid(comment.getCommentid());
        CommentByVideoEntity c2 = new CommentByVideoEntity();
        c2.setVideoid(comment.getVideoid());
        c2.setCommentid(comment.getCommentid());
        cqlSession.execute(
             BatchStatement.builder(DefaultBatchType.LOGGED)
                 .addStatement(bind(psDeleteCommentUser,  c1, entityHelperCommentsByUser))
                 .addStatement(bind(psDeleteCommentVideo, c2, entityHelperCommentsByVideo))
                 .build());
     }
    
    /** Javadoc in {@link CommentDseDao} */
    public ResultListPage<CommentByVideoEntity> findCommentsByVideoId(final QueryCommentByVideo query) {
        return new ResultListPage<>(cqlSession.execute(_buildStatementVideoComments(query))
                                              .map(entityHelperCommentsByVideo::get));
    }
    
    /** Javadoc in {@link CommentDseDao} */
    public CompletionStage<ResultListPage<CommentByVideoEntity>> findCommentsByVideoIdAsync(final QueryCommentByVideo query) {
        return cqlSession.executeAsync(_buildStatementVideoComments(query))
                         .thenApply(ars  -> ars.map(entityHelperCommentsByVideo::get))
                         .thenApply(ResultListPage::new);
    }
    
    /** Javadoc in {@link CommentDseDao} */
    public ResultListPage<CommentByUserEntity> findCommentsByUserId(final QueryCommentByUser query) {
        return new ResultListPage<>(cqlSession.execute(_buildStatementUserComments(query))
                                              .map(entityHelperCommentsByUser::get));
    }
    
    /** Javadoc in {@link CommentDseDao} */
    public CompletionStage<ResultListPage<CommentByUserEntity>> findCommentsByUserIdAsync(final QueryCommentByUser query) {
        return cqlSession.executeAsync(_buildStatementUserComments(query))
                         .thenApply(ars  -> ars.map(entityHelperCommentsByUser::get))
                         .thenApply(ResultListPage::new);
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
        if (cqlSession  != null && !cqlSession.isClosed()) {
            cqlSession.close();
        }
    }
    
    /**
     * Init statement based on comment tag.
     *  
     * @param request
     *      current request
     * @return
     *      statement
     */
    private BoundStatement _buildStatementVideoComments(final QueryCommentByVideo query) {
        BoundStatement bs = null;
        if (query.getCommentId().isPresent()) {
            bs = psFindCommentsByVideoPageable.bind()
                        .setUuid(COMMENTS_COLUMN_VIDEOID, query.getVideoId())
                        .setUuid(COMMENTS_COLUMN_COMMENTID, query.getCommentId().get());
        } else {
            bs = psFindCommentsByVideo.bind()
                        .setUuid(COMMENTS_COLUMN_VIDEOID, query.getVideoId());
        }
        if (query.getPageState().isPresent() && query.getPageState().get().length() > 0) {
            bs = bs.setPagingState(ByteBuffer.wrap(query.getPageState().get().getBytes()));
        }
        bs = bs.setPageSize(query.getPageSize());
        return bs;
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
    private BoundStatement _buildStatementUserComments(final QueryCommentByUser query) {
        BoundStatement bs = null;
        if (query.getCommentId().isPresent()) {
            bs = psFindCommentsByUserPageable.bind()
                        .setUuid(COMMENTS_COLUMN_USERID, query.getUserId())
                        .setUuid(COMMENTS_COLUMN_COMMENTID, query.getCommentId().get());
        } else {
            bs = psFindCommentsByUser.bind()
                        .setUuid(COMMENTS_COLUMN_USERID, query.getUserId());
        }
        if (query.getPageState().isPresent() && query.getPageState().get().length() > 0) {
            bs = bs.setPagingState(ByteBuffer.wrap(query.getPageState().get().getBytes()));
        }
        bs = bs.setPageSize(query.getPageSize());
        return bs;
    }
    
    private BatchStatement _buildStatementInsertComment(Comment comment) {
        return BatchStatement.builder(DefaultBatchType.LOGGED)
                .addStatement(bind(psInsertCommentUser,  new CommentByUserEntity(comment),  entityHelperCommentsByUser))
                .addStatement(bind(psInsertCommentVideo, new CommentByVideoEntity(comment), entityHelperCommentsByVideo))
                .build();
    }
    
}
