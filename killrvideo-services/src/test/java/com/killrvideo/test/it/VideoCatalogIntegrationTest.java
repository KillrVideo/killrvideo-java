package com.killrvideo.test.it;

import java.util.UUID;

import com.killrvideo.KillrvideoServicesGrpcClient;
import com.killrvideo.grpc.GrpcMappingUtils;

import killrvideo.user_management.UserManagementServiceOuterClass.VerifyCredentialsRequest;
import killrvideo.user_management.UserManagementServiceOuterClass.VerifyCredentialsResponse;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetLatestVideoPreviewsRequest;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetLatestVideoPreviewsResponse;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.SubmitYouTubeVideoRequest;

public class VideoCatalogIntegrationTest {
    
    public static void main(String[] args) throws Exception {        
        KillrvideoServicesGrpcClient client = new KillrvideoServicesGrpcClient("localhost", 50101);
        callLastestVideos(client);
    }
    
   
    public static void callLastestVideos(KillrvideoServicesGrpcClient client)
    throws Exception {
        // Asking for the latest 2 videos
        GetLatestVideoPreviewsRequest req = GetLatestVideoPreviewsRequest.newBuilder()
                .setPageSize(2).build();
        GetLatestVideoPreviewsResponse res = 
                client.videoCatalogServiceGrpcClient.getLatestVideoPreviews(req);
        Thread.sleep(1000);
        
        System.out.println(res.getVideoPreviewsList());
    }
    
    public static void callAddNewVideo(KillrvideoServicesGrpcClient client) {
        SubmitYouTubeVideoRequest myNewVideoYoutube = SubmitYouTubeVideoRequest.newBuilder()
                .addTags("Cassandra")
                .setDescription("MyVideo")
                .setName(" My Sample Video")
                .setUserId(GrpcMappingUtils.uuidToUuid(UUID.randomUUID()))
                .setVideoId(GrpcMappingUtils.uuidToUuid(UUID.randomUUID()))
                .setYouTubeVideoId("EBMriswzd94")
                .build();
        client.videoCatalogServiceGrpcClient.submitYouTubeVideo(myNewVideoYoutube);
    }
    
    public static void callVerifiyCredentials(KillrvideoServicesGrpcClient client)
            throws Exception {
        VerifyCredentialsRequest creRequest = VerifyCredentialsRequest.newBuilder()
                .setEmail("a.a@a.com")
                .setPassword("aaa")
                .build();
               
        VerifyCredentialsResponse res = client.userServiceGrpcClient.verifyCredentials(creRequest);
        System.out.println(res.getUserId());
    }
    

}
