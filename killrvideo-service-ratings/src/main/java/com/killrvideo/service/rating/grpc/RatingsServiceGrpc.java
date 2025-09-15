package com.killrvideo.service.rating.grpc;

import static com.killrvideo.service.rating.grpc.RatingsServiceGrpcMapper.maptoRatingResponse;
import static com.killrvideo.service.rating.grpc.RatingsServiceGrpcMapper.maptoUserRatingResponse;
import static com.killrvideo.service.rating.grpc.RatingsServiceGrpcValidator.validateGrpcRequest_GetRating;
import static com.killrvideo.service.rating.grpc.RatingsServiceGrpcValidator.validateGrpcRequest_GetUserRating;
import static com.killrvideo.service.rating.grpc.RatingsServiceGrpcValidator.validateGrpcRequest_RateVideo;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.killrvideo.grpc.GrpcMappingUtils;
import com.killrvideo.messaging.dao.MessagingDao;
import com.killrvideo.service.rating.dao.RatingDseDao;
import com.killrvideo.service.rating.dao.RatingDseDaoMapperBuilder;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import killrvideo.ratings.RatingsServiceGrpc.RatingsServiceImplBase;
import killrvideo.ratings.RatingsServiceOuterClass.GetRatingRequest;
import killrvideo.ratings.RatingsServiceOuterClass.GetRatingResponse;
import killrvideo.ratings.RatingsServiceOuterClass.GetUserRatingRequest;
import killrvideo.ratings.RatingsServiceOuterClass.GetUserRatingResponse;
import killrvideo.ratings.RatingsServiceOuterClass.RateVideoRequest;
import killrvideo.ratings.RatingsServiceOuterClass.RateVideoResponse;
import killrvideo.ratings.events.RatingsEvents.UserRatedVideo;

/**
 * Operations on Ratings with GRPC.
 *
 * @author DataStax advocates Team
 */
@Service
public class RatingsServiceGrpc extends RatingsServiceImplBase {
    
    /** Loger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(RatingsServiceGrpc.class);
    
    /** Service definition for ratings.*/
    private RatingDseDao dseRatingDao;
    
    /** Inter-service communications (messaging). */
    @Autowired
    private MessagingDao messagingDao;
    
    @Value("${killrvideo.messaging.kafka.topics.videoRated : topic-kv-videoRating}")
    private String topicvideoRated;
    
    @Autowired
    private CqlSession cqlSession;
    
    @Autowired
    @Qualifier("killrvideo.keyspace")
    private CqlIdentifier dseKeySpace;
    
    @PostConstruct
    public void init() {
        dseRatingDao = new RatingDseDaoMapperBuilder(cqlSession).build().ratingDao(dseKeySpace);
    }
    
    /** {@inheritDoc} */
    @Override
    public void rateVideo(final RateVideoRequest grpcReq, final StreamObserver<RateVideoResponse> grpcResObserver) {
        
        // Validate Parameters
        validateGrpcRequest_RateVideo(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        // Mapping GRPC => Domain (Dao)
        UUID videoid = UUID.fromString(grpcReq.getVideoId().getValue());
        UUID userid  = UUID.fromString(grpcReq.getUserId().getValue());
        Integer rate = grpcReq.getRating();
        
        // Invoking Dao (Async), publish event if successful
        dseRatingDao.rateVideo(videoid, userid, rate).whenComplete((result, error) -> {
            if (error == null) {
                traceSuccess("rateVideo", starts);
                messagingDao.sendEvent(topicvideoRated, 
                        UserRatedVideo.newBuilder()
                        .setRating(grpcReq.getRating())
                        .setRatingTimestamp(GrpcMappingUtils.instantToTimeStamp(Instant.now()))
                        .setUserId(grpcReq.getUserId())
                        .setVideoId(grpcReq.getVideoId()).build());
                grpcResObserver.onNext(RateVideoResponse.newBuilder().build());
                grpcResObserver.onCompleted();
            } else {
                traceError("rateVideo", starts, error);
                grpcResObserver.onError(Status.INTERNAL.withCause(error).asRuntimeException());
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void getRating(GetRatingRequest grpcReq, StreamObserver<GetRatingResponse> grpcResObserver) {
        
        // Validate Parameters
        validateGrpcRequest_GetRating(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        // Mapping GRPC => Domain (Dao)
        UUID videoid = UUID.fromString(grpcReq.getVideoId().getValue());
        
        // Invoking Dao (Async) and map result back to GRPC (maptoRatingResponse)
        dseRatingDao.findRating(videoid).whenComplete((videoRating, error) -> {
            if (error == null) {
                traceSuccess("getRating", starts);
                if (videoRating.isPresent()) {
                    grpcResObserver.onNext(maptoRatingResponse(videoRating.get()));
                } else {
                    grpcResObserver.onNext(GetRatingResponse.newBuilder()
                            .setVideoId(grpcReq.getVideoId())
                            .setRatingsCount(0L)
                            .setRatingsTotal(0L)
                            .build());
                }
                grpcResObserver.onCompleted();
            } else {
                traceError("getRating", starts, error);
                grpcResObserver.onError(Status.INTERNAL.withCause(error).asRuntimeException());
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void getUserRating(GetUserRatingRequest grpcReq, StreamObserver<GetUserRatingResponse> grpcResObserver) {
        
        // Validate Parameters
        validateGrpcRequest_GetUserRating(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        // Mapping GRPC => Domain (Dao)
        UUID videoid = UUID.fromString(grpcReq.getVideoId().getValue());
        UUID userid  = UUID.fromString(grpcReq.getUserId().getValue());
        
        // Invoking Dao (Async) and map result back to GRPC (maptoRatingResponse)
        dseRatingDao.findUserRating(videoid,userid).whenComplete((videoRating, error) -> {
            if (error == null) {
                traceSuccess("getUserRating" , starts);
                if (videoRating.isPresent()) {
                    grpcResObserver.onNext(maptoUserRatingResponse(videoRating.get()));
                } else {
                    grpcResObserver.onNext(GetUserRatingResponse.newBuilder()
                            .setUserId(grpcReq.getUserId())
                            .setVideoId(grpcReq.getVideoId())
                            .setRating(0)
                            .build());
                }
                grpcResObserver.onCompleted();
            } else {
                traceError("getUserRating", starts, error);
                grpcResObserver.onError(Status.INTERNAL.withCause(error).asRuntimeException());
            }
        });
    }
    
    /**
     * Utility to TRACE.
     *
     * @param method
     *      current operation
     * @param start
     *      timestamp for starting
     */
    private void traceSuccess(String method, Instant starts) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("End successfully '{}' in {} millis", method, Duration.between(starts, Instant.now()).getNano()/1000);
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
    private void traceError(String method, Instant starts, Throwable t) {
        LOGGER.error("An error occured in {} after {}", method, Duration.between(starts, Instant.now()), t);
    }
    
}
