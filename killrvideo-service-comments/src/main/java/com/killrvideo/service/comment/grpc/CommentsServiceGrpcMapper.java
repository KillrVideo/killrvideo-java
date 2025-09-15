package com.killrvideo.service.comment.grpc;

import static com.killrvideo.grpc.GrpcMappingUtils.dateToTimestamp;
import static com.killrvideo.grpc.GrpcMappingUtils.uuidToTimeUuid;
import static com.killrvideo.grpc.GrpcMappingUtils.uuidToUuid;
import static com.killrvideo.service.comment.grpc.CommentsServiceGrpcValidator.initErrorString;
import static com.killrvideo.service.comment.grpc.CommentsServiceGrpcValidator.notEmpty;
import static com.killrvideo.service.comment.grpc.CommentsServiceGrpcValidator.positive;
import static com.killrvideo.service.comment.grpc.CommentsServiceGrpcValidator.validate;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.util.Assert;

import com.killrvideo.dse.dto.ResultListPage;
import com.killrvideo.service.comment.dao.dto.Comment;
import com.killrvideo.service.comment.dao.dto.CommentByUserEntity;
import com.killrvideo.service.comment.dao.dto.CommentByVideoEntity;
import com.killrvideo.service.comment.grpc.dto.QueryCommentByUser;
import com.killrvideo.service.comment.grpc.dto.QueryCommentByVideo;

import io.grpc.stub.StreamObserver;
import killrvideo.comments.CommentsServiceOuterClass;
import killrvideo.comments.CommentsServiceOuterClass.CommentOnVideoRequest;
import killrvideo.comments.CommentsServiceOuterClass.GetUserCommentsRequest;
import killrvideo.comments.CommentsServiceOuterClass.GetUserCommentsResponse;
import killrvideo.comments.CommentsServiceOuterClass.GetVideoCommentsRequest;
import killrvideo.comments.CommentsServiceOuterClass.GetVideoCommentsResponse;

/**
 * Validation of inputs and mapping
 *
 * @author DataStax Developer Advocates team.
 */
public class CommentsServiceGrpcMapper {
    
    /**
     * Hide constructor.
     */
    private CommentsServiceGrpcMapper() {
    }
    
    /**
     * Validate comment On video comment query.
     * 
     * @param request
     *      current GRPC Request
     * @param streamObserver
     *      response async
     * @return
     *      true if the query is valid
     */
    public static void validateGrpcRequestCommentOnVideo(Logger logger, CommentOnVideoRequest request, StreamObserver<?> streamObserver) {
        StringBuilder errorMessage = initErrorString(request);
        boolean isValid = 
                  notEmpty(!request.hasUserId()    || isBlank(request.getUserId().getValue()),  "userId",  "video request",errorMessage) &&
                  notEmpty(!request.hasVideoId()   || isBlank(request.getVideoId().getValue()), "videoId", "video request",errorMessage) &&
                  notEmpty(!request.hasCommentId() || isBlank(request.getCommentId().getValue()), "commentId", "video request",errorMessage) &&
                  notEmpty(isBlank(request.getComment()), "comment", "video request",errorMessage);
        Assert.isTrue(validate(logger, streamObserver, errorMessage, isValid), "Invalid parameter for 'commentOnVideo'");
    }
    
    /**
     * Validate get video comment query.
     * 
     * @param request
     *      current GRPC Request
     * @param streamObserver
     *      response async
     * @return
     *      true if the query is valid
     */
    public static void validateGrpcRequestGetVideoComment(Logger logger, GetVideoCommentsRequest request, StreamObserver<?> streamObserver) {
        final StringBuilder errorMessage = initErrorString(request);
        boolean isValid = true;

        if (!request.hasVideoId() || isBlank(request.getVideoId().getValue())) {
            errorMessage.append("\t\tvideo id should be provided for get video comment request\n");
            isValid = false;
        }

        if (request.getPageSize() <= 0) {
            errorMessage.append("\t\tpage size should be strictly positive for get video comment request");
            isValid = false;
        }

        Assert.isTrue(validate(logger, streamObserver, errorMessage, isValid), "Invalid parameter for 'getVideoComments'");
    }
    
    /**
     * Validate get user comment query.
     * 
     * @param request
     *      current GRPC Request
     * @param streamObserver
     *      response async
     * @return
     *      true if the query is valid
     */
    public static void validateGrpcRequest_GetUserComments(Logger logger, GetUserCommentsRequest request, StreamObserver<?> streamObserver) {
        final StringBuilder errorMessage = initErrorString(request);
        boolean isValid = 
                  notEmpty(!request.hasUserId() || isBlank(request.getUserId().getValue()),  "userId",  "comment request",errorMessage) &&
                  positive(request.getPageSize() <= 0,  "page size",  "comment request",errorMessage);
        Assert.isTrue(validate(logger, streamObserver, errorMessage, isValid), "Invalid parameter for 'getUserComments'");
    }
    
    // --- Mappings ---
    
    /**
     * Utility from exposition to Dse query.
     * 
     * @param grpcReq
     *      grpc Request
     * @return
     *      query bean for Dao
     */
    public static QueryCommentByUser mapFromGrpcUserCommentToDseQuery(GetUserCommentsRequest grpcReq) {
        QueryCommentByUser targetQuery = new QueryCommentByUser();
        if (grpcReq.hasStartingCommentId() && 
                !isBlank(grpcReq.getStartingCommentId().getValue())) {
            targetQuery.setCommentId(Optional.of(UUID.fromString(grpcReq.getStartingCommentId().getValue())));
        }
        targetQuery.setUserId(UUID.fromString(grpcReq.getUserId().getValue()));
        targetQuery.setPageSize(grpcReq.getPageSize());
        targetQuery.setPageState(Optional.ofNullable(grpcReq.getPagingState()));
        return targetQuery;
    }
    
    // Map from CommentDseDao response bean to expected GRPC object.
    public static GetVideoCommentsResponse mapFromDseVideoCommentToGrpcResponse(ResultListPage<CommentByVideoEntity> dseRes) {
        final GetVideoCommentsResponse.Builder builder = GetVideoCommentsResponse.newBuilder();
        for (Comment c : dseRes.getResults()) {
           builder.setVideoId(uuidToUuid(c.getVideoid()));
           builder.addComments(CommentsServiceOuterClass.VideoComment.newBuilder()
                  .setComment(c.getComment())
                  .setUserId(uuidToUuid(c.getUserid()))
                  .setCommentId(uuidToTimeUuid(c.getCommentid()))
                  .setCommentTimestamp(dateToTimestamp(c.getDateOfComment()))
                  .build());
        }
        dseRes.getPagingState().ifPresent(builder::setPagingState);
        return builder.build();
    }
    
    // Map from CommentDseDao response bean to expected GRPC object.
    public static GetUserCommentsResponse mapFromDseUserCommentToGrpcResponse(ResultListPage<CommentByUserEntity> dseRes) {
        final GetUserCommentsResponse.Builder builder = GetUserCommentsResponse.newBuilder();
        for (Comment c : dseRes.getResults()) {
           builder.setUserId(uuidToUuid(c.getUserid()));
           builder.addComments(CommentsServiceOuterClass.UserComment.newBuilder()
                   .setComment(c.getComment())
                   .setCommentId(uuidToTimeUuid(c.getCommentid()))
                   .setVideoId(uuidToUuid(c.getVideoid()))
                   .setCommentTimestamp(dateToTimestamp(c.getDateOfComment()))
                   .build());
        }
        dseRes.getPagingState().ifPresent(builder::setPagingState);
        return builder.build();
    }
    
    /**
     * Utility from exposition to Dse query.
     * 
     * @param grpcReq
     *      grpc Request
     * @return
     *      query bean for Dao
     */
    public static QueryCommentByVideo mapFromGrpcVideoCommentToDseQuery(GetVideoCommentsRequest grpcReq) {
        QueryCommentByVideo targetQuery = new QueryCommentByVideo();
        if (grpcReq.hasStartingCommentId() && 
                !isBlank(grpcReq.getStartingCommentId().getValue())) {
            targetQuery.setCommentId(Optional.of(UUID.fromString(grpcReq.getStartingCommentId().getValue())));
        }
        targetQuery.setVideoId(UUID.fromString(grpcReq.getVideoId().getValue()));
        targetQuery.setPageSize(grpcReq.getPageSize());
        targetQuery.setPageState(Optional.ofNullable(grpcReq.getPagingState()));
        return targetQuery;
    }

}
