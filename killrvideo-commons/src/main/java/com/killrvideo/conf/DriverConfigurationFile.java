package com.killrvideo.conf;

import java.io.File;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.evanlennick.retry4j.CallExecutor;
import com.evanlennick.retry4j.config.RetryConfig;
import com.evanlennick.retry4j.config.RetryConfigBuilder;
import com.killrvideo.dse.graph.KillrVideoTraversalSource;

/**
 * The DSE (DataStax Enterprise) Driver configuration.
 *
 * The above properties should be typically declared in an {@code application.conf} file.
 * 
 * @author DataStax Developer Advocates team.
 */
@Configuration
@Profile("!unit-test & !integration-test")
public class DriverConfigurationFile {

    /** Initialize dedicated connection to ETCD system. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DriverConfigurationFile.class);
    
    /** Execution Profile. */
    public static final String EXECUTION_PROFILE_SEARCH = "search";
      
    // --- Retries ---
    
    @Value("${killrvideo.dse.maxNumberOfTries:50}")
    protected int maxNumberOfTries;
    
    @Value("${killrvideo.dse.delayBetweenTries:5}")
    protected int delayBetweenTries;
    
    @Value("#{environment.KILLRVIDEO_DSE_CONFIGURATION_FILE}")
    protected Optional<String> driverConfigurationFileEnvVar;
    
    @Value("${killrvideo.dse.configFile:application.conf}")
    protected String driverConfigurationFile;
    
    @Bean
    public DriverConfigLoader loadConfigurationFile() {
        // If the env variable is set we override default value
        if (!driverConfigurationFileEnvVar.isEmpty()) {
            driverConfigurationFile = driverConfigurationFileEnvVar.get();
        }
        LOGGER.info("Loading configuration from File '{}'", driverConfigurationFile);
        File configFile = new File(DriverConfigurationFile.class.getResource("/" + driverConfigurationFile).getFile());
        DriverConfigLoader loader = DriverConfigLoader.fromFile(configFile);
        DriverExecutionProfile basic = loader.getInitialConfig().getDefaultProfile();
        LOGGER.info("Configuration file has been parsed:");
        LOGGER.info("+ keyspace '{}'",      basic.getString(DefaultDriverOption.SESSION_KEYSPACE));
        LOGGER.info("+ ContactPoints '{}'", basic.getStringList(DefaultDriverOption.CONTACT_POINTS));
        return loader;
    }
    
    /**
     * Returns the keyspace to connect to. The keyspace specified here must exist.
     *
     * @return The {@linkplain CqlIdentifier keyspace} bean.
     */
    @Bean("killrvideo.keyspace")
    public CqlIdentifier keyspace(CqlSession cqlSession) {
        return cqlSession.getKeyspace().orElseThrow();
    }
   
    @Bean
    public CqlSession connect(DriverConfigLoader loader) {
        LOGGER.info("Initializing connection...");
        CqlSessionBuilder cqlSessionBuilder = CqlSession.builder().withConfigLoader(loader);
               
        // Connection Lambda
        final AtomicInteger atomicCount = new AtomicInteger(1);
        Callable<CqlSession> connect = () -> {
            return cqlSessionBuilder.build();
        };
        
        // Retry mechanism policy
        RetryConfig config = new RetryConfigBuilder()
                .retryOnAnyException()
                .withMaxNumberOfTries(maxNumberOfTries)
                .withDelayBetweenTries(delayBetweenTries, ChronoUnit.SECONDS)
                .withFixedBackoff()
                .build();
        
        long top = System.currentTimeMillis();
        
        // Let's go
        return new CallExecutor<CqlSession>(config)
                .afterFailedTry(s -> { 
                    LOGGER.info("Attempt #{}/{} [KO] -> waiting {} seconds for Cluster to start", atomicCount.getAndIncrement(),
                            maxNumberOfTries,  delayBetweenTries); })
                .onFailure(s -> {
                    LOGGER.error("Cannot connection to Cluster after {} attempts, exiting", maxNumberOfTries);
                    System.err.println("Can not conenction to Cluster after " + maxNumberOfTries + " attempts, exiting");
                    System.exit(500);
                 })
                .onSuccess(s -> {   
                    long timeElapsed = System.currentTimeMillis() - top;
                    LOGGER.info("[OK] Connection etablished to Cluster in {} millis.", timeElapsed);})
                .execute(connect).getResult();
    }
    
    /**
     * Graph Traversal for suggested videos.
     *
     * @param session
     *      current dse session.
     * @return
     *      traversal
     */
    @Bean
    public KillrVideoTraversalSource initializeGraphTraversalSource(CqlSession dseSession) {
        //System.out.println(dseSession.getMetadata().getNodes().values().iterator().next().getExtras().get("DSE_WORKLOADS"));
        //return new KillrVideoTraversalSource(DseGraph.g.getGraph());
        return EmptyGraph.instance().traversal(KillrVideoTraversalSource.class);
    }
     
}
