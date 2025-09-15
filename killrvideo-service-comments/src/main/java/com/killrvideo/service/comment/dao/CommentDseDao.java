package com.killrvideo.service.comment.dao;

import java.util.concurrent.CompletionStage;

import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.QueryProvider;
import com.killrvideo.dse.dto.ResultListPage;
import com.killrvideo.service.comment.dao.dto.Comment;
import com.killrvideo.service.comment.dao.dto.CommentByUserEntity;
import com.killrvideo.service.comment.dao.dto.CommentByVideoEntity;
import com.killrvideo.service.comment.grpc.dto.QueryCommentByUser;
import com.killrvideo.service.comment.grpc.dto.QueryCommentByVideo;

/**
 * Implementation of Services to work with Comments in Killrvideo. We work with 
 * 2 tables 'comments_by_user' and 'comments_by_video'.
 * 
 * @author DataStax Developer Advocates team.
 */
@Dao
public interface CommentDseDao {
    
    /**
     * Create a comment in both table if not exist, update otherwise.
     *
     * @param comment
     *      bean wrapping comments information
     */
    @QueryProvider(
            providerClass = CommentDseDaoQueryProvider.class,
            entityHelpers = { CommentByUserEntity.class, CommentByVideoEntity.class})
    void upsert(Comment comment);

    /**
     * Create a comment in both table ASYNCHRONOUSLY if not exist, update otherwise.
     * This state as an example to implement async method.
     *
     * @param comment
     *      bean wrapping comments information
     */
    @QueryProvider(
            providerClass = CommentDseDaoQueryProvider.class,
            entityHelpers = { CommentByUserEntity.class, CommentByVideoEntity.class})
    CompletionStage<Void> upsertAsync(Comment comment);
    
    /**
     * Delete a comment base don 
     *
     * @param comment
     *      bean wrapping comments information
     */
    @QueryProvider(
      providerClass = CommentDseDaoQueryProvider.class,
      entityHelpers = { CommentByUserEntity.class, CommentByVideoEntity.class})
    void delete(Comment res);
    
    /**
     * As this query is dynamic (comment it can be present or not) we delegate the query to query
     * builder. The result is mapped to expected {@link Comment} bean.
     * 
     * @param query
     *      bean wrapping expecting parameters
     * @return
     *      a page of resultss
     */
    @QueryProvider(
            providerClass = CommentDseDaoQueryProvider.class,
            entityHelpers = { CommentByUserEntity.class, CommentByVideoEntity.class})
    ResultListPage<CommentByVideoEntity> findCommentsByVideoId(final QueryCommentByVideo query);
    
    @QueryProvider(
            providerClass = CommentDseDaoQueryProvider.class,
            entityHelpers = { CommentByUserEntity.class, CommentByVideoEntity.class})
    CompletionStage<ResultListPage<CommentByVideoEntity>> findCommentsByVideoIdAsync(final QueryCommentByVideo query);
    
    /**
     * As this query is dynamic (comment it can be present or not) we delegate the query to query
     * builder. The result is mapped to expected {@link Comment} bean.
     * 
     * @param query
     *      bean wrapping expecting parameters
     * @return
     *      a page of resultss
     */
    @QueryProvider(
            providerClass = CommentDseDaoQueryProvider.class,
            entityHelpers = { CommentByUserEntity.class, CommentByVideoEntity.class})
    ResultListPage<CommentByUserEntity> findCommentsByUserId(final QueryCommentByUser query);
    
    @QueryProvider(
            providerClass = CommentDseDaoQueryProvider.class,
            entityHelpers = { CommentByUserEntity.class, CommentByVideoEntity.class})
    CompletionStage<ResultListPage<CommentByUserEntity>> findCommentsByUserIdAsync(final QueryCommentByUser query);
    
}
