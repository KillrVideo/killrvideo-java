package com.killrvideo.grpc;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.killrvideo.conf.GrpcConfiguration;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;

/**
 * Support class to build GRPC Server per service.
 *
 * @author Cedrick LUNVEN (@clunven)
 */
public abstract class AbstractSingleServiceGrpcServer {
    
    /** Some logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSingleServiceGrpcServer.class);
   
    @Autowired
    protected GrpcConfiguration grpcConfig;
    
    /** GRPC Server to start. */
    protected Server grpcServer;
    
    /** Service Name. */
    protected abstract String getServiceName();
    
    /** Lock target port. */
    protected abstract int getDefaultPort();
    
    /** Service Definition. */
    protected abstract ServerServiceDefinition getService();
   
    /**
     * Start
     */
    @PostConstruct
    public void startGrpcServer() throws Exception {
        LOGGER.info("Initializing Comment Service");
        grpcServer = ServerBuilder.forPort(grpcConfig.getGrpcPort())
                              .addService(getService())
                              .build();
        Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    stopGrpcServer();
                }
        });
        grpcServer.start();
        LOGGER.info("[OK] Grpc Server started on port: '{}'", grpcConfig.getGrpcPort());
    }
    
    @PreDestroy
    public void stopGrpcServer() {
        LOGGER.info("Stopping GrpcServer...");
        grpcServer.shutdown();
        LOGGER.info("[OK] Grpc Server stopped");
    }
    
}
