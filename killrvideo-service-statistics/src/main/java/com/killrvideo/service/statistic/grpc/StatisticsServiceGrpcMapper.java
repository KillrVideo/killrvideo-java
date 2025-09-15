package com.killrvideo.service.statistic.grpc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.killrvideo.grpc.GrpcMappingUtils;
import com.killrvideo.service.statistic.dto.VideoPlaybackStats;

import killrvideo.common.CommonTypes.Uuid;
import killrvideo.statistics.StatisticsServiceOuterClass.GetNumberOfPlaysRequest;
import killrvideo.statistics.StatisticsServiceOuterClass.GetNumberOfPlaysResponse;
import killrvideo.statistics.StatisticsServiceOuterClass.PlayStats;

/**
 * Helper and mappers for DAO <=> GRPC Communications
 *
 * @author DataStax Developer Advocates Team
 */
@Component
public class StatisticsServiceGrpcMapper {
    
    public static GetNumberOfPlaysResponse buildGetNumberOfPlayResponse(GetNumberOfPlaysRequest grpcReq, List<VideoPlaybackStats> videoList) {
        final Map<Uuid, PlayStats> result = videoList.stream()
                .filter(x -> x != null)
                .map(StatisticsServiceGrpcMapper::mapVideoPlayBacktoPlayStats)
                .collect(Collectors.toMap(x -> x.getVideoId(), x -> x));

        final GetNumberOfPlaysResponse.Builder builder = GetNumberOfPlaysResponse.newBuilder();
        for (Uuid requestedVideoId : grpcReq.getVideoIdsList()) {
            if (result.containsKey(requestedVideoId)) {
                builder.addStats(result.get(requestedVideoId));
            } else {
                builder.addStats(PlayStats
                        .newBuilder()
                        .setVideoId(requestedVideoId)
                        .setViews(0L)
                        .build());
            }
        }
        return builder.build();
    }
    
    /**
     * Mapping to generated GPRC beans.
     */
    private static PlayStats mapVideoPlayBacktoPlayStats(VideoPlaybackStats v) {
        return PlayStats.newBuilder()
                .setVideoId(GrpcMappingUtils.uuidToUuid(v.getVideoid()))
                .setViews(Optional.ofNullable(v.getViews()).orElse(0L)).build();
    }
    

}
