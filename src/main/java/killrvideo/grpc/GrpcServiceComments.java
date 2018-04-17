package killrvideo.grpc;

import static killrvideo.utils.TypeConverter.dateToTimestamp;
import static killrvideo.utils.TypeConverter.uuidToTimeUuid;
import static killrvideo.utils.TypeConverter.uuidToUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import io.grpc.stub.StreamObserver;
import killrvideo.comments.CommentsServiceGrpc.CommentsServiceImplBase;
import killrvideo.comments.CommentsServiceOuterClass;
import killrvideo.comments.CommentsServiceOuterClass.CommentOnVideoRequest;
import killrvideo.comments.CommentsServiceOuterClass.CommentOnVideoResponse;
import killrvideo.comments.CommentsServiceOuterClass.GetUserCommentsRequest;
import killrvideo.comments.CommentsServiceOuterClass.GetUserCommentsResponse;
import killrvideo.comments.CommentsServiceOuterClass.GetVideoCommentsRequest;
import killrvideo.comments.CommentsServiceOuterClass.GetVideoCommentsResponse;
import killrvideo.dao.bean.QueryCommentByVideo;
import killrvideo.dao.bean.CommentListResult;
import killrvideo.dao.bean.QueryCommentByUser;
import killrvideo.dao.dse.CommentDseDao;
import killrvideo.dao.event.MessagingDao;
import killrvideo.entity.Comment;
import killrvideo.validation.KillrVideoInputValidator;

/**
 * Exposition of comment services with GPRC Technology & Protobuf Interface
 * 
 * @author DataStax evangelist team.
 */
@Service
public class GrpcServiceComments extends CommentsServiceImplBase {
    
    /** Loger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(GrpcServiceComments.class);
    
    /** Inter-service communication channel (messaging). */
    @Inject
    private MessagingDao msgDao;
    
    /** Communications and queries to DSE (Comment). */
    @Inject
    private CommentDseDao dseCommentDao;
    
    /** JSR-303 Validator. */
    @Inject
    private KillrVideoInputValidator validator;
    
    /** As demo application we demonstrate both synchronous and asynchronous samples. */
    @Value("${killrvideo.services.comment.async: true}")
    private boolean asynchronousExecution;
   
    /** {@inheritDoc} */
    @Override
    public void commentOnVideo(final CommentOnVideoRequest grpcReq, StreamObserver<CommentOnVideoResponse> grpcResObserver) {
        
        // GRPC Parameters Validation
        Assert.isTrue(validator.isValid(grpcReq, grpcResObserver), "Invalid parameter for 'commentOnVideo'");
        
        // Stopwqtch used to compute elapsed times
        final long start = System.currentTimeMillis();
        
        // Mapping from GRPC to Domain (DDD)
        Comment q = new Comment(grpcReq);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Insert comment on video {} for user {} : {}",  q.getVideoid(), q.getUserid(), q);
        }
        
        /** 
         * As reference application we propose 2 implementations SYNC and ASYNC.
         * => Pick the one relevant to your use cases
         * => To change behaviour of the killrVideo App, change the value in application.yaml file.
         **/ 
        if (asynchronousExecution) {
            
            // ASYNCHRONOUS works with ComputableFuture
            dseCommentDao.insertCommentAsync(q).whenComplete((result, error) -> {
                if (error != null ) {
                    traceError("commentOnVideo", start, error);
                    msgDao.publishExceptionEvent(q, error);
                    grpcResObserver.onError(error);
                } else {
                    traceSuccess("commentOnVideo", start);
                    msgDao.publishCommentCreateEvent(grpcReq, Instant.ofEpochMilli(start));
                    grpcResObserver.onNext(CommentOnVideoResponse.newBuilder().build());
                    grpcResObserver.onCompleted();
                }
            });
            
        } else {
            
            // SYNCHRONOUS with the classic try/catch
            try {
                dseCommentDao.insertComment(q);
                traceSuccess("commentOnVideo", start);
                msgDao.publishCommentCreateEvent(grpcReq, Instant.ofEpochMilli(start));
                grpcResObserver.onNext(CommentOnVideoResponse.newBuilder().build());
                grpcResObserver.onCompleted();
            } catch(Throwable t) {
                traceError("commentOnVideo", start, t);
                msgDao.publishExceptionEvent(grpcReq, t);
                grpcResObserver.onError(t);
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void getVideoComments(final GetVideoCommentsRequest grpcReq, StreamObserver<GetVideoCommentsResponse> responseObserver) {
        
        // Parameter validations
        Assert.isTrue(validator.isValid(grpcReq, responseObserver), "Invalid parameter for 'getVideoComments'");
        
        // Service Implementation is agnostic from exposition (here GRPC) 
        QueryCommentByVideo query = mapFromGrpcVideoCommentToDseQuery(grpcReq);
        
        // Executing request ASYNC and Mapping Back to GRPC
        final long start = System.currentTimeMillis();
        
        /** 
         * As reference application we propose 2 implementations SYNC and ASYNC.
         * => Pick the one relevant to your use cases
         * => To change behaviour of the killrVideo App, change the value in application.yaml file.
         **/ 
        if (asynchronousExecution) {
            
            // ASYNCHRONOUS works with ComputableFuture
            dseCommentDao.findCommentsByVideosIdAsync(query).whenComplete((result, error) -> {
                if (result != null) {
                    traceSuccess("getVideoComments", start);
                    responseObserver.onNext(mapFromDseVideoCommentToGrpcResponse(result));
                    responseObserver.onCompleted();
                } else if (error != null){
                    traceError("getVideoComments", start, error);
                    msgDao.publishExceptionEvent(grpcReq, error);
                    responseObserver.onError(error);
                }
            });
            
        } else {
           // SYNCHRONOUS with the classic try/catch
            try {
                traceSuccess("getVideoComments", start);
                responseObserver.onNext(mapFromDseVideoCommentToGrpcResponse(dseCommentDao.findCommentsByVideoId(query)));
                responseObserver.onCompleted();
            } catch(Throwable t) {
                traceError("getVideoComments", start, t);
                msgDao.publishExceptionEvent(grpcReq, t);
                responseObserver.onError(t);
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void getUserComments(final GetUserCommentsRequest grpcReq, StreamObserver<GetUserCommentsResponse> responseObserver) {

        // GRPC Parameters Validation
        Assert.isTrue(validator.isValid(grpcReq, responseObserver), "Invalid parameter for 'getUserComments'");
        
        // Executing request ASYNC and Mapping Back to GRPC
        final long start = System.currentTimeMillis();
        
        // Service Implementation is agnostic from exposition (here GRPC) 
        QueryCommentByUser query = mapFromGrpcUserCommentToDseQuery(grpcReq);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Listing comment for user {}",  query.getUserId());
        }
        
        /** 
         * As reference application we propose 2 implementations SYNC and ASYNC.
         * => Pick the one relevant to your use cases
         * => To change behaviour of the killrVideo App, change the value in application.yaml file.
         **/ 
        if (asynchronousExecution) {
        
            // ASYNCHRONOUS works with ComputableFuture
            dseCommentDao.findCommentsByUserIdAsync(query).whenComplete((result, error) -> {
                if (result != null) {
                    traceSuccess("getUserComments", start);
                    responseObserver.onNext(mapFromDseUserCommentToGrpcResponse(result));
                    responseObserver.onCompleted();
                } else if (error != null){
                    traceError("getUserComments", start, error);
                    msgDao.publishExceptionEvent(grpcReq, error);
                    responseObserver.onError(error);
                }
            });
            
        } else {
            // SYNCHRONOUS with the classic try/catch
            try {
                traceSuccess("getUserComments", start);
                responseObserver.onNext(mapFromDseUserCommentToGrpcResponse(dseCommentDao.findCommentsByUserId(query)));
                responseObserver.onCompleted();
            } catch(Throwable t) {
                traceError("getUserComments", start, t);
                msgDao.publishExceptionEvent(grpcReq, t);
                responseObserver.onError(t);
            }
        }
    }
    
    /**
     * Utility to TRACE.
     *
     * @param method
     *      current operation
     * @param start
     *      timestamp for starting
     */
    private void traceSuccess(String method, long start) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("End successfully '{}' in {} millis", method, System.currentTimeMillis() - start);
        }
    }
    
    /**
     * Utility to TRACE.
     *
     * @param method
     *      current operation
     * @param start
     *      timestamp for starting
     */
    private void traceError(String method, long start, Throwable t) {
        LOGGER.error("An error occured in " + method + " after " + (System.currentTimeMillis() - start) + " millis", method, t);
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
    private QueryCommentByUser mapFromGrpcUserCommentToDseQuery(GetUserCommentsRequest grpcReq) {
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
    private GetVideoCommentsResponse mapFromDseVideoCommentToGrpcResponse(CommentListResult dseRes) {
        LOGGER.debug("{} comment(s) retrieved from DSE", dseRes.getComments().size());
        final GetVideoCommentsResponse.Builder builder = GetVideoCommentsResponse.newBuilder();
        for (Comment c : dseRes.getComments()) {
           LOGGER.debug(" + Mapping comment {} on video {} ", c.getCommentid().toString(), c.getVideoid());
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
    private GetUserCommentsResponse mapFromDseUserCommentToGrpcResponse(CommentListResult dseRes) {
        LOGGER.debug("{} comment(s) retrieved from DSE", dseRes.getComments().size());
        final GetUserCommentsResponse.Builder builder = GetUserCommentsResponse.newBuilder();
        for (Comment c : dseRes.getComments()) {
           LOGGER.debug(" + Mapping comment {} on user {} ", c.getCommentid().toString(), c.getUserid());
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
    private QueryCommentByVideo mapFromGrpcVideoCommentToDseQuery(GetVideoCommentsRequest grpcReq) {
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
