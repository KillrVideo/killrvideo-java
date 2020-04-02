package com.killrvideo.service.rating.grpc;

import static com.killrvideo.grpc.GrpcMappingUtils.uuidToUuid;

import java.util.Optional;

import com.killrvideo.service.rating.dto.VideoRatings;
import com.killrvideo.service.rating.dto.VideoRatingsByUser;

import killrvideo.ratings.RatingsServiceOuterClass.GetRatingResponse;
import killrvideo.ratings.RatingsServiceOuterClass.GetUserRatingResponse;

/**
 * Helper and mappers for DAO <=> GRPC Communications
 *
 * @author DataStax Developer Advocates Team
 */
public class RatingsServiceGrpcMapper {
    
    /**
     * Hide constructor.
     */
    private RatingsServiceGrpcMapper() {
    }
     
   /**
     * Mapping to generated GPRC beans.
     */
    public static GetRatingResponse maptoRatingResponse(VideoRatings vr) {
        return GetRatingResponse.newBuilder()
                .setVideoId(uuidToUuid(vr.getVideoid()))
                .setRatingsCount(Optional.ofNullable(vr.getRatingCounter()).orElse(0L))
                .setRatingsTotal(Optional.ofNullable(vr.getRatingTotal()).orElse(0L))
                .build();
    }
    
    /**
     * Mapping to generated GPRC beans.
     */
    public static GetUserRatingResponse maptoUserRatingResponse(VideoRatingsByUser vr) {
        return GetUserRatingResponse.newBuilder()
                .setVideoId(uuidToUuid(vr.getVideoid()))
                .setUserId(uuidToUuid(vr.getUserid()))
                .setRating(vr.getRating())
                .build();
    }
    
}
