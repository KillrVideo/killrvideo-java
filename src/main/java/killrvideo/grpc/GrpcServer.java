package killrvideo.grpc;

import static java.lang.String.format;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.xqbase.etcd4j.EtcdClient;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import killrvideo.configuration.KillrVideoConfiguration;
import killrvideo.dao.event.CassandraMutationErrorHandler;
import killrvideo.service.RatingsService;
import killrvideo.service.SearchService;
import killrvideo.service.StatisticsService;
import killrvideo.service.SuggestedVideosService;
import killrvideo.service.UploadsService;
import killrvideo.service.UserManagementService;
import killrvideo.service.VideoCatalogService;

/**
 * Startup a GRPC server on expected port and register all services.
 *
 * @author DataStax evangelist team.
 */
@Component
public class GrpcServer {

    /** Some logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServer.class);

    /** Load configuration from Yaml file and environments variables. */
    @Inject
    private KillrVideoConfiguration config;
    
    /** Connectivity to ETCD Service discovery. */
    @Inject
    private EtcdClient etcdClient;
    
    /** Communication channel between service, for now GUAVA inmemory messaging. */
    @Inject
    private EventBus eventBus;
    
    @Inject
    private GrpcServiceComments commentService;

    @Inject
    private RatingsService ratingService;

    @Inject
    private SearchService searchService;

    @Inject
    private StatisticsService statisticsService;

    @Inject
    private UploadsService uploadsService;

    @Inject
    private UserManagementService userManagementService;

    @Inject
    private VideoCatalogService videoCatalogService;

    @Inject
    private SuggestedVideosService suggestedVideosService;

    @Inject
    private CassandraMutationErrorHandler cassandraMutationErrorHandler;   

    /**
     * GRPC Server to set up.
     */
    private Server server;
    
    /** Initiqlized once at startup use for ETCD. */
    private String applicationUID;
    
    @PostConstruct
    public void start() throws Exception {
        applicationUID = config.getApplicationName().trim()  + ":" + config.getApplicationInstanceId();
        LOGGER.info("Initializing Grpc Server...");
        // Binding Services
        final ServerServiceDefinition commentService        = this.commentService.bindService();
        final ServerServiceDefinition ratingService         = this.ratingService.bindService();
        final ServerServiceDefinition statisticsService     = this.statisticsService.bindService();
        final ServerServiceDefinition suggestedVideoService = this.suggestedVideosService.bindService();
        final ServerServiceDefinition uploadsService        = this.uploadsService.bindService();
        final ServerServiceDefinition userManagementService = this.userManagementService.bindService();
        final ServerServiceDefinition videoCatalogService   = this.videoCatalogService.bindService();
        final ServerServiceDefinition searchService         = this.searchService.bindService();
        
        // Initializing GRPC endpoint
        server = ServerBuilder.forPort(config.getApplicationPort())
                    .addService(commentService)
                    .addService(ratingService)
                    .addService(statisticsService)
                    .addService(suggestedVideoService)
                    .addService(uploadsService)
                    .addService(userManagementService)
                    .addService(videoCatalogService)
                    .addService(searchService)
                    .build();
    
        // Initialize Event bus
        eventBus.register(suggestedVideosService);
        eventBus.register(cassandraMutationErrorHandler);

        /**
         * Declare a shutdown hook otherwise the JVM
         * cannot be stop since the Grpc server
         * is listening on  a port forever
         */
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                LOGGER.info("Calling shutdown for GrpcServer");
                server.shutdown();
            }
        });

        // Start Grpc listener
        server.start();
        LOGGER.info("Grpc Server started on port: '{}'", config.getApplicationPort());
        
        // Service are now Bound an started, declare in ETCD
        final String applicationAdress = format("%s:%d", config.getApplicationHost(), config.getApplicationPort());
        LOGGER.info("Registering services in ETCD with address {}", applicationAdress);
        registerServicesToEtcd(applicationAdress, 
                commentService, ratingService, statisticsService,
                suggestedVideoService, uploadsService, userManagementService, 
                videoCatalogService, searchService);
        LOGGER.info("Services now registered in ETCD");
    }

    @PreDestroy
    public void stop() {
        eventBus.unregister(suggestedVideosService);
        eventBus.unregister(cassandraMutationErrorHandler);
        server.shutdown();
    }

    /**
     * Enter information into ETCD.
     *
     * @param serviceDefinitions
     *          services definitions from GRPC
     * @throws IOException
     *          exception when accessing ETCD
     */
    private void registerServicesToEtcd(String applicationAdress, ServerServiceDefinition... serviceDefinitions) 
    throws IOException {
        // Note that we don't use a lambda to ease Exception propagation
        for (ServerServiceDefinition service : serviceDefinitions) {
            final String shortName  = shortenServiceName(service.getServiceDescriptor().getName());
            final String serviceKey = format("/killrvideo/services/%s/%s", shortName, applicationUID);
            LOGGER.info(" + [{}] : key={}", shortName, serviceKey);
            etcdClient.set(serviceKey, applicationAdress);
        }
    }
    
    /**
     * Remove special caracters.
     */
    private String shortenServiceName(String fullServiceName) {
        return fullServiceName.replaceAll(".*\\.([^.]+)", "$1");
    }
}
