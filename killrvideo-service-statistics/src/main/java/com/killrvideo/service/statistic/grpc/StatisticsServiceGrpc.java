package com.killrvideo.service.statistic.grpc;

import static com.killrvideo.service.statistic.grpc.StatisticsServiceGrpcMapper.buildGetNumberOfPlayResponse;
import static com.killrvideo.service.statistic.grpc.StatisticsServiceGrpcValidator.validateGrpcRequest_GetNumberPlays;
import static com.killrvideo.service.statistic.grpc.StatisticsServiceGrpcValidator.validateGrpcRequest_RecordPlayback;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.killrvideo.service.statistic.dao.StatisticsDseDao;
import com.killrvideo.service.statistic.dao.StatisticsDseDaoMapperBuilder;
import com.killrvideo.service.statistic.dto.VideoPlaybackStats;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import killrvideo.common.CommonTypes.Uuid;
import killrvideo.statistics.StatisticsServiceGrpc.StatisticsServiceImplBase;
import killrvideo.statistics.StatisticsServiceOuterClass.GetNumberOfPlaysRequest;
import killrvideo.statistics.StatisticsServiceOuterClass.GetNumberOfPlaysResponse;
import killrvideo.statistics.StatisticsServiceOuterClass.RecordPlaybackStartedRequest;
import killrvideo.statistics.StatisticsServiceOuterClass.RecordPlaybackStartedResponse;

/**
 * Get statistics on a video.
 *
 * @author DataStax advocates Team
 */
@Service
public class StatisticsServiceGrpc extends StatisticsServiceImplBase {

    /** Loger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(StatisticsServiceGrpc.class);
    
    /** Definition of statistics operations. */
    private StatisticsDseDao statisticsDseDao;
    
    @Autowired
    private CqlSession cqlSession;
    
    @Autowired
    @Qualifier("killrvideo.keyspace")
    private CqlIdentifier dseKeySpace;
    
    @PostConstruct
    public void init() {
        statisticsDseDao = new StatisticsDseDaoMapperBuilder(cqlSession).build().statisticsDao(dseKeySpace);
    }
    
    /** {@inheritDoc} */
    @Override
    public void recordPlaybackStarted(RecordPlaybackStartedRequest grpcReq, StreamObserver<RecordPlaybackStartedResponse> grpcResObserver) {
        
        // Validate Parameters
        validateGrpcRequest_RecordPlayback(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        // Mapping GRPC => Domain (Dao)
        final UUID videoId = UUID.fromString(grpcReq.getVideoId().getValue());
        
        // Map Result back to GRPC
        statisticsDseDao.recordPlaybackStartedAsync(videoId).whenComplete((result, error) -> {
            if (error != null ) {
                traceError("recordPlaybackStarted", starts, error);
                grpcResObserver.onError(Status.INTERNAL.withCause(error).asRuntimeException());
            } else {
                grpcResObserver.onNext(RecordPlaybackStartedResponse.newBuilder().build());
                grpcResObserver.onCompleted();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void getNumberOfPlays(GetNumberOfPlaysRequest grpcReq, StreamObserver<GetNumberOfPlaysResponse> grpcResObserver) {
        
        // Validate Parameters
        validateGrpcRequest_GetNumberPlays(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        List<CompletableFuture<VideoPlaybackStats>> callBackList = 
                grpcReq.getVideoIdsList().stream()
                   .map(Uuid::getValue)
                   .map(UUID::fromString)
                   .map(statisticsDseDao::getNumberOfPlaysAsync)
                   .map(CompletionStage::toCompletableFuture)
                   .collect(Collectors.toList());
        
        CompletableFuture
                .allOf(callBackList.toArray(CompletableFuture[]::new))
                .thenApply(v -> callBackList.stream().map(CompletableFuture::join).collect(Collectors.toList()))
                .whenComplete((videoList, error) -> {
            if (error != null ) {
                traceError("getNumberOfPlays", starts, error);
                grpcResObserver.onError(Status.INTERNAL.withCause(error).asRuntimeException());
            } else {
                traceSuccess("getNumberOfPlays", starts);
                grpcResObserver.onNext(buildGetNumberOfPlayResponse(grpcReq, videoList));
                grpcResObserver.onCompleted();
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
