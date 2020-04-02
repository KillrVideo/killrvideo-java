package com.killrvideo.service.search.grpc;

import static com.killrvideo.utils.ValidationUtils.initErrorString;
import static com.killrvideo.utils.ValidationUtils.validate;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.slf4j.Logger;
import org.springframework.util.Assert;

import io.grpc.stub.StreamObserver;
import killrvideo.search.SearchServiceOuterClass.GetQuerySuggestionsRequest;
import killrvideo.search.SearchServiceOuterClass.SearchVideosRequest;

/**
 * Check parameters in Search GRPC Service.
 *
 * @author DataStax Developer Advocates team.
 */
public class SearchServiceGrpcValidator  {

    /**
     * Hide constructor.
     */
    private SearchServiceGrpcValidator() {
    }
    
    public static void validateGrpcRequest_GetQuerySuggestions(Logger logger, GetQuerySuggestionsRequest request, StreamObserver<?> streamObserver) {
        final StringBuilder errorMessage = initErrorString(request);
        boolean isValid = true;
        if (isBlank(request.getQuery())) {
            errorMessage.append("\t\tquery string should be provided for get video suggestions request\n");
            isValid = false;
        }
        if (request.getPageSize() <= 0) {
            errorMessage.append("\t\tpage size should be strictly positive for get video suggestions request\n");
            isValid = false;
        }
        Assert.isTrue(validate(logger, streamObserver, errorMessage, isValid), 
                "Invalid parameter for 'getQuerySuggestions'");
    }
    
    /**
     * Validation for search.
     */
    public static void validateGrpcRequest_SearchVideos(Logger logger, SearchVideosRequest request, StreamObserver<?> streamObserver) {
        final StringBuilder errorMessage = initErrorString(request);
        boolean isValid = true;
        if (isBlank(request.getQuery())) {
            errorMessage.append("\t\tquery string should be provided for search videos request\n");
            isValid = false;
        }
        if (request.getPageSize() <= 0) {
            errorMessage.append("\t\tpage size should be strictly positive for search videos request\n");
            isValid = false;
        }
        Assert.isTrue(validate(logger, streamObserver, errorMessage, isValid), "Invalid parameter for 'searchVideos'");
    }
    
    
   
    
}
