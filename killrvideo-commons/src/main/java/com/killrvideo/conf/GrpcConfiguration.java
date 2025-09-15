package com.killrvideo.conf;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfiguration {
    
    @Value("#{environment.KILLRVIDEO_GRPC_PORT}")
    private Optional<Integer> grpcPortEnvironmentVar;
    
    /** Listening Port for GRPC. */
    @Value("${killrvideo.grpc-server.port:50101}")
    private int grpcPort;
    
    public int getGrpcPort() {
        if (!grpcPortEnvironmentVar.isEmpty()) {
            grpcPort = grpcPortEnvironmentVar.get();
        }
        return grpcPort;
    }

}
