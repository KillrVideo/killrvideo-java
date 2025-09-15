package com.killrvideo.service.search.grpc;

import com.killrvideo.dse.dto.Video;
import com.killrvideo.grpc.GrpcMappingUtils;

import killrvideo.search.SearchServiceOuterClass.SearchResultsVideoPreview;
import killrvideo.search.SearchServiceOuterClass.SearchResultsVideoPreview.Builder;


/**
 * Helper and mappers for DAO <=> GRPC Communications
 *
 * @author DataStax Developer Advocates Team
 */
public class SearchServiceGrpcMapper {
    
    /**
     * Hide constructor.
     */
    private SearchServiceGrpcMapper() {}
    
    /**
     * Mapping to generated GPRC beans (Search result special).
     */
    public static SearchResultsVideoPreview maptoResultVideoPreview(Video v) {
        Builder builder = SearchResultsVideoPreview.newBuilder();
        builder.setName(v.getName());
        builder.setVideoId(GrpcMappingUtils.uuidToUuid(v.getVideoid()));
        builder.setUserId(GrpcMappingUtils.uuidToUuid(v.getUserid()));
        if (v.getPreviewImageLocation() != null)  {
            builder.setPreviewImageLocation(v.getPreviewImageLocation());
        }
        if (v.getAddedDate() != null)  {
            builder.setAddedDate(GrpcMappingUtils.instantToTimeStamp(v.getAddedDate()));
        }
        return builder.build();
    }
    
    
    

    
}
