package com.killrvideo.service.user.grpc;

import static com.killrvideo.service.user.grpc.UserManagementServiceGrpcMapper.mapResponseVerifyCredentials;
import static com.killrvideo.service.user.grpc.UserManagementServiceGrpcMapper.mapUserRequest2User;
import static com.killrvideo.service.user.grpc.UserManagementServiceGrpcValidator.validateGrpcRequest_VerifyCredentials;
import static com.killrvideo.service.user.grpc.UserManagementServiceGrpcValidator.validateGrpcRequest_createUser;
import static com.killrvideo.service.user.grpc.UserManagementServiceGrpcValidator.validateGrpcRequest_getUserProfile;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.google.protobuf.Timestamp;
import com.killrvideo.messaging.dao.MessagingDao;
import com.killrvideo.service.user.dao.UserDseDao;
import com.killrvideo.service.user.dto.User;
import com.killrvideo.utils.HashUtils;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import killrvideo.user_management.UserManagementServiceGrpc.UserManagementServiceImplBase;
import killrvideo.user_management.UserManagementServiceOuterClass.CreateUserRequest;
import killrvideo.user_management.UserManagementServiceOuterClass.CreateUserResponse;
import killrvideo.user_management.UserManagementServiceOuterClass.GetUserProfileRequest;
import killrvideo.user_management.UserManagementServiceOuterClass.GetUserProfileResponse;
import killrvideo.user_management.UserManagementServiceOuterClass.VerifyCredentialsRequest;
import killrvideo.user_management.UserManagementServiceOuterClass.VerifyCredentialsResponse;
import killrvideo.user_management.events.UserManagementEvents.UserCreated;

/**
 * Create or update users.
 *
 * @author DataStax advocates Team
 */
@Service("killrvideo.service.grpc.user")
public class UserManagementServiceGrpc extends UserManagementServiceImplBase {
    
    /** Loger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(UserManagementServiceGrpc.class);
    
    /** Service Definition for Users. */
    private UserDseDao userDseDao;
    
    @Autowired
    private CqlSession cqlSession;
    
    @Autowired
    @Qualifier("killrvideo.keyspace")
    private CqlIdentifier dseKeySpace;
    
    @Autowired
    private MessagingDao messagingDao;
    
    @Value("${killrvideo.messaging.destinations.userCreated : topic-kv-userCreation}")
    private String topicUserCreated;
    
    @PostConstruct
    public void init() {
        userDseDao = new UserDseDao(cqlSession);
    }
    
     /** {@inheritDoc} */
    @Override
    public void createUser(
            final CreateUserRequest grpcReq, 
            final StreamObserver<CreateUserResponse> grpcResObserver) {
      
        // Validate Parameters
        validateGrpcRequest_createUser(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        // Mapping GRPC => Domain (Dao)
        User user = mapUserRequest2User(grpcReq);
        final String hashedPassword = HashUtils.hashPassword(grpcReq.getPassword().trim());
        
         // Invoke DAO Async
        userDseDao.createUserAsync(user, hashedPassword).whenComplete((result, error) -> {
            if (error != null ) {
                traceError("createUser", starts, error);
                grpcResObserver.onError(Status.INVALID_ARGUMENT.augmentDescription(error.getMessage())
                               .asRuntimeException());
            } else {
                traceSuccess("createUser", starts);
                messagingDao.sendEvent(topicUserCreated, UserCreated.newBuilder()
                        .setEmail(grpcReq.getEmail())
                        .setFirstName(grpcReq.getFirstName())
                        .setLastName(grpcReq.getLastName())
                        .setUserId(grpcReq.getUserId())
                        .setTimestamp(Timestamp.newBuilder().build()));
                grpcResObserver.onNext(CreateUserResponse.newBuilder().build());
                grpcResObserver.onCompleted();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void verifyCredentials(
            final VerifyCredentialsRequest grpcReq, 
            final StreamObserver<VerifyCredentialsResponse> grpcResObserver) {
        
        // Validate Parameters
        validateGrpcRequest_VerifyCredentials(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        // Mapping GRPC => Domain (Dao)
        String email = grpcReq.getEmail();
        
        // Map back as GRPC (if correct invalid credential otherwize)
        userDseDao.getUserCredentialAsync(email).whenComplete((credential, error) -> {
            if (error != null ) {
                traceError("verifyCredentials", starts, error);
                if (!HashUtils.isPasswordValid(grpcReq.getPassword(), credential.getPassword())) {
                    grpcResObserver.onError(Status.INVALID_ARGUMENT
                                   .withDescription("Email address or password are not correct").asRuntimeException());
                } else {
                    grpcResObserver.onError(Status.INTERNAL.withCause(error).asRuntimeException());
                }
            } else {
                traceSuccess("verifyCredentials", starts);
                grpcResObserver.onNext(mapResponseVerifyCredentials(credential.getUserid()));
                grpcResObserver.onCompleted();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void getUserProfile(
            final GetUserProfileRequest grpcReq, 
            final StreamObserver<GetUserProfileResponse> grpcResObserver) {
        
        // Validate Parameters
        validateGrpcRequest_getUserProfile(LOGGER, grpcReq, grpcResObserver);
        
        // Stands as stopwatch for logging and messaging 
        final Instant starts = Instant.now();
        
        // No user id provided, still a valid confition
        final GetUserProfileResponse.Builder builder = GetUserProfileResponse.newBuilder();
        if (grpcReq.getUserIdsCount() == 0 || CollectionUtils.isEmpty(grpcReq.getUserIdsList())) {
            traceSuccess("getUserProfile", starts);
            grpcResObserver.onNext(builder.build());
            grpcResObserver.onCompleted();
            LOGGER.debug("No user id provided");
            return;
        } else {
            
            // Mapping GRPC => Domain (Dao)
            List < UUID > listOfUserId = Arrays.asList(grpcReq
                    .getUserIdsList()
                    .stream()
                    .map(uuid -> UUID.fromString(uuid.getValue()))
                    .toArray(size -> new UUID[size]));
            
            // Mapping back to GRPC objects
            userDseDao.getUserProfilesAsync(listOfUserId).whenComplete((users, error) -> {
                if (error != null ) {
                    traceError("getUserProfile", starts, error);
                    grpcResObserver.onError(Status.INTERNAL.withCause(error).asRuntimeException());
                } else {
                    traceSuccess("getUserProfile", starts);
                    users.stream()
                         .map(UserManagementServiceGrpcMapper::mapUserToGrpcUserProfile)
                         .forEach(builder::addProfiles);    
                    grpcResObserver.onNext(builder.build());
                    grpcResObserver.onCompleted();
                }
            });
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
