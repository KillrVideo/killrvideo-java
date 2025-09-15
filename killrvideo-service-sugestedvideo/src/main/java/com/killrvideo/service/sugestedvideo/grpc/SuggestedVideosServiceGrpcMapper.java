package com.killrvideo.service.sugestedvideo.grpc;

import static com.killrvideo.grpc.GrpcMappingUtils.uuidToUuid;
import static com.killrvideo.utils.ValidationUtils.initErrorString;
import static com.killrvideo.utils.ValidationUtils.validate;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.HashSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.killrvideo.dse.dto.Video;
import com.killrvideo.grpc.GrpcMappingUtils;

import io.grpc.stub.StreamObserver;
import killrvideo.suggested_videos.SuggestedVideosService.GetRelatedVideosRequest;
import killrvideo.suggested_videos.SuggestedVideosService.GetRelatedVideosResponse;
import killrvideo.suggested_videos.SuggestedVideosService.GetSuggestedForUserRequest;
import killrvideo.suggested_videos.SuggestedVideosService.GetSuggestedForUserResponse;
import killrvideo.suggested_videos.SuggestedVideosService.SuggestedVideoPreview;
import killrvideo.video_catalog.events.VideoCatalogEvents.YouTubeVideoAdded;

/**
 * Helper and mappers for DAO <=> GRPC Communications
 *
 * @author DataStax Developer Advocates Team
 */
@Component
public class SuggestedVideosServiceGrpcMapper {
    
    /**
     * Hide constructor of utility class.
     */
    private SuggestedVideosServiceGrpcMapper() {}
    
    public static Video mapVideoAddedtoVideoDTO(YouTubeVideoAdded videoAdded) {
        // Convert Stub to Dto, dao must not be related to interface GRPC
        Video video = new Video();
        video.setVideoid(UUID.fromString(videoAdded.getVideoId().toString()));
        video.setAddedDate(GrpcMappingUtils.timestampToInstant(videoAdded.getAddedDate()));
        video.setUserid(UUID.fromString(videoAdded.getUserId().toString()));
        video.setName(videoAdded.getName());
        video.setTags(new HashSet<String>(videoAdded.getTagsList()));
        video.setPreviewImageLocation(videoAdded.getPreviewImageLocation());
        video.setLocation(videoAdded.getLocation());
        return video;
    }
    
    public static void validateGrpcRequest_getRelatedVideo(Logger logger, GetRelatedVideosRequest request, StreamObserver<GetRelatedVideosResponse> streamObserver) {
        final StringBuilder errorMessage = initErrorString(request);
        boolean isValid = true;

        if (request.getVideoId() == null || isBlank(request.getVideoId().getValue())) {
            errorMessage.append("\t\tvideo id should be provided for get related videos request\n");
            isValid = false;
        }
        Assert.isTrue(validate(logger, streamObserver, errorMessage, isValid), "Invalid parameter for 'getRelatedVideo'");
    }
    
    public static void validateGrpcRequest_getUserSuggestedVideo(Logger logger, GetSuggestedForUserRequest request, StreamObserver<GetSuggestedForUserResponse> streamObserver) {
        final StringBuilder errorMessage = initErrorString(request);
        boolean isValid = true;
        if (request.getUserId() == null || isBlank(request.getUserId().getValue())) {
            errorMessage.append("\t\tuser id should be provided for get suggested for user request\n");
            isValid = false;
        }
        Assert.isTrue(validate(logger, streamObserver, errorMessage, isValid), "Invalid parameter for 'getSuggestedForUser'");
    }

    /**
     * Mapping to generated GPRC beans. (Suggested videos special)
     */
    public static SuggestedVideoPreview mapVideotoSuggestedVideoPreview(Video v) {
        return SuggestedVideoPreview.newBuilder()
                .setName(v.getName())
                .setVideoId(uuidToUuid(v.getVideoid()))
                .setUserId(uuidToUuid(v.getUserid()))
                .setPreviewImageLocation(v.getPreviewImageLocation())
                .setAddedDate(GrpcMappingUtils.instantToTimeStamp(v.getAddedDate()))
                .build();
    }
    
    
}
