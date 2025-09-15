package com.killrvideo.service.user.grpc;

import java.util.Date;
import java.util.UUID;

import com.killrvideo.grpc.GrpcMappingUtils;
import com.killrvideo.service.user.dto.User;

import killrvideo.user_management.UserManagementServiceOuterClass.CreateUserRequest;
import killrvideo.user_management.UserManagementServiceOuterClass.UserProfile;
import killrvideo.user_management.UserManagementServiceOuterClass.VerifyCredentialsResponse;

/**
 * Mapping from interfaces GRPC to DTO
 *
 * @author DataStax advocates Team
 */
public class UserManagementServiceGrpcMapper {
    
    /** Hide constructor for utility class. */
    private UserManagementServiceGrpcMapper() {
    }
    
    public static User mapUserRequest2User(CreateUserRequest grpcReq) {
        User user = new User();
        user.setEmail(grpcReq.getEmail());
        user.setCreatedAt(new Date());
        user.setFirstname(grpcReq.getFirstName());
        user.setLastname(grpcReq.getLastName());
        user.setUserid(UUID.fromString(grpcReq.getUserId().getValue()));
        return user;
    }
    
    public static UserProfile mapUserToGrpcUserProfile(User user) {
       return UserProfile.newBuilder()
                    .setEmail(user.getEmail())
                    .setFirstName(user.getFirstname())
                    .setLastName(user.getLastname())
                    .setUserId(GrpcMappingUtils.uuidToUuid(user.getUserid()))
                    .build();
    }
    
    public static VerifyCredentialsResponse mapResponseVerifyCredentials(UUID userid) {
        return VerifyCredentialsResponse.newBuilder().setUserId(GrpcMappingUtils.uuidToUuid(userid)).build();
    }

}
