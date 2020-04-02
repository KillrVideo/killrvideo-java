package com.killrvideo.service.user.grpc;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.slf4j.Logger;
import org.springframework.util.Assert;

import io.grpc.stub.StreamObserver;
import killrvideo.common.CommonTypes;
import killrvideo.user_management.UserManagementServiceOuterClass.CreateUserRequest;
import killrvideo.user_management.UserManagementServiceOuterClass.GetUserProfileRequest;
import killrvideo.user_management.UserManagementServiceOuterClass.VerifyCredentialsRequest;

import static com.killrvideo.utils.ValidationUtils.initErrorString;
import static com.killrvideo.utils.ValidationUtils.validate;

/**
 * Validate GRPC parameters.
 *
 * @author DataStax advocates Team
 */
public class UserManagementServiceGrpcValidator {
    
    /** Hide constructor for utility class. */
    private UserManagementServiceGrpcValidator() {}
    
    /**
     * Validate create user.
     *
     * @param logger
     * @param request
     * @param streamObserver
     */
    public static void validateGrpcRequest_createUser(Logger logger,  CreateUserRequest request, StreamObserver<?> streamObserver) {
        final StringBuilder errorMessage = initErrorString(request);
        boolean isValid = true;
        if (request.getUserId() == null || isBlank(request.getUserId().getValue())) {
            errorMessage.append("\t\tuser id should be provided for create user request\n");
            isValid = false;
        }
        if (isBlank(request.getPassword())) {
            errorMessage.append("\t\tpassword should be provided for create user request\n");
            isValid = false;
        }
        if (isBlank(request.getEmail())) {
            errorMessage.append("\t\temail should be provided for create user request\n");
            isValid = false;
        }
        Assert.isTrue(validate(logger, streamObserver, errorMessage, isValid), "Invalid parameter for 'createUser'");
    }
    
    public static void validateGrpcRequest_VerifyCredentials(Logger logger, VerifyCredentialsRequest request, StreamObserver<?> streamObserver) {
        final StringBuilder errorMessage = initErrorString(request);
        boolean isValid = true;

        if (isBlank(request.getEmail())) {
            errorMessage.append("\t\temail should be provided for verify credentials request\n");
            isValid = false;
        }

        if (isBlank(request.getPassword())) {
            errorMessage.append("\t\tpassword should be provided for verify credentials request\n");
            isValid = false;
        }
        Assert.isTrue(validate(logger, streamObserver, errorMessage, isValid), "Invalid parameter for 'verifyCredentials'");
    }
    
    public static void validateGrpcRequest_getUserProfile(Logger logger, GetUserProfileRequest request, StreamObserver<?> streamObserver) {
        final StringBuilder errorMessage = initErrorString(request);
        boolean isValid = true;
        if (request.getUserIdsCount() > 20) {
            errorMessage.append("\t\tcannot get more than 20 user profiles at once for get user profile request\n");
            isValid = false;
        }
        for (CommonTypes.Uuid uuid : request.getUserIdsList()) {
            if (uuid == null || isBlank(uuid.getValue())) {
                errorMessage.append("\t\tprovided UUID values cannot be null or blank for get user profile request\n");
                isValid = false;
            }
        }
        Assert.isTrue(validate(logger, streamObserver, errorMessage, isValid), "Invalid parameter for 'getUserProfile'");
    }

}
