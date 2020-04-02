package com.killrvideo.service.video.grpc;

import static com.killrvideo.grpc.GrpcMappingUtils.instantToTimeStamp;
import static com.killrvideo.grpc.GrpcMappingUtils.uuidToUuid;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.common.collect.Sets;
import com.killrvideo.dse.dao.DseSchema;
import com.killrvideo.dse.dto.CustomPagingState;
import com.killrvideo.dse.dto.Video;
import com.killrvideo.service.video.dto.LatestVideo;
import com.killrvideo.service.video.dto.LatestVideosPage;
import com.killrvideo.service.video.dto.UserVideo;

import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetLatestVideoPreviewsResponse;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetVideoResponse;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.SubmitYouTubeVideoRequest;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.VideoLocationType;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.VideoPreview;

/**
 * Utility mapping GRPC.
 *
 * @author Cedrick LUNVEN (@clunven)
 */
public class VideoCatalogServiceGrpcMapper {
    
    /** Hide constructor of utility class. */
    private VideoCatalogServiceGrpcMapper() {
    }

    public static Video mapSubmitYouTubeVideoRequestAsVideo(SubmitYouTubeVideoRequest request) {
        Video targetVideo = new Video();
        targetVideo.setVideoid(UUID.fromString(request.getVideoId().getValue()));
        targetVideo.setUserid(UUID.fromString(request.getUserId().getValue()));
        targetVideo.setName(request.getName());
        targetVideo.setLocation(request.getYouTubeVideoId());
        targetVideo.setDescription(request.getDescription());
        targetVideo.setPreviewImageLocation("//img.youtube.com/vi/"+ targetVideo.getLocation() + "/hqdefault.jpg");
        targetVideo.setTags(Sets.newHashSet(request.getTagsList().iterator()));
        targetVideo.setLocationType(VideoLocationType.YOUTUBE.ordinal());
        return targetVideo;
    }
    
    /**
     * Mapping to GRPC generated classes.
     */
    public static VideoPreview mapLatestVideotoVideoPreview(LatestVideo lv) {
        VideoPreview.Builder builder = VideoPreview.newBuilder()
                .setAddedDate(instantToTimeStamp(lv.getAddedDate()))
                .setName(lv.getName())
                .setPreviewImageLocation(Optional.ofNullable(lv.getPreviewImageLocation()).orElse("N/A"));
        if (null != lv.getUserid()) {
            builder.setUserId(uuidToUuid(lv.getUserid()));
        }
        if (null != lv.getVideoid()) {
            builder.setVideoId(uuidToUuid(lv.getVideoid()));
        }
        return builder.build();
    }
    
    public static GetLatestVideoPreviewsResponse mapLatestVideoToGrpcResponse(LatestVideosPage returnedPage) {
        return GetLatestVideoPreviewsResponse.newBuilder()
                .addAllVideoPreviews(
                        returnedPage.getListOfPreview().stream()
                        .map(VideoCatalogServiceGrpcMapper::mapLatestVideotoVideoPreview)
                        .collect(Collectors.toList()))
                .setPagingState(returnedPage.getNextPageState())
                .build();
    }
    
    /**
     * Mapping to generated GPRC beans.
     */
    public static VideoPreview mapFromVideotoVideoPreview(Video v) {
        return VideoPreview.newBuilder()
                .setAddedDate(instantToTimeStamp(v.getAddedDate()))
                .setName(v.getName())
                .setPreviewImageLocation(Optional.ofNullable(v.getPreviewImageLocation()).orElse("N/A"))
                .setUserId(uuidToUuid(v.getUserid()))
                .setVideoId(uuidToUuid(v.getVideoid()))
                .build();
    }
    
    /**
     * Mapping to generated GPRC beans.
     */
    public static VideoPreview mapFromUserVideotoVideoPreview(UserVideo v) {
        return VideoPreview.newBuilder()
                .setAddedDate(instantToTimeStamp(v.getAddedDate()))
                .setName(v.getName())
                .setPreviewImageLocation(Optional.ofNullable(v.getPreviewImageLocation()).orElse("N/A"))
                .setUserId(uuidToUuid(v.getUserid()))
                .setVideoId(uuidToUuid(v.getVideoid()))
                .build();
    }
    
    /**
     * Mapping to generated GPRC beans (Full detailed)
     */
    public static GetVideoResponse mapFromVideotoVideoResponse(Video v) {
        return GetVideoResponse.newBuilder()
                .setAddedDate(instantToTimeStamp(v.getAddedDate()))
                .setDescription(v.getDescription())
                .setLocation(v.getLocation())
                .setLocationType(VideoLocationType.forNumber(v.getLocationType()))
                .setName(v.getName())
                .setUserId(uuidToUuid(v.getUserid()))
                .setVideoId(uuidToUuid(v.getVideoid()))
                .addAllTags(v.getTags())
                .build();
    }
    
    /**
     * Build the first paging state if one does not already exist and return an object containing 3 elements
     * representing the initial state (List<String>, Integer, String).
     * @return CustomPagingState
     */
    public static CustomPagingState buildFirstCustomPagingState() {
        return new CustomPagingState()
                .currentBucket(0)
                .cassandraPagingState(null)
                .listOfBuckets(LongStream.rangeClosed(0L, 7L).boxed()
                        .map(Instant.now().atZone(ZoneId.systemDefault())::minusDays)
                        .map(x -> x.format(DseSchema.DATEFORMATTER))
                        .collect(Collectors.toList()));
    }

   
}
