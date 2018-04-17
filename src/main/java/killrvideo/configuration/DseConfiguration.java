package killrvideo.configuration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.policies.AddressTranslator;
import com.datastax.driver.dse.DseCluster.Builder;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.dse.auth.DsePlainTextAuthProvider;
import com.datastax.driver.dse.graph.GraphOptions;
import com.datastax.driver.dse.graph.GraphProtocol;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.dse.graph.api.DseGraph;
import com.evanlennick.retry4j.CallExecutor;
import com.evanlennick.retry4j.config.RetryConfig;
import com.evanlennick.retry4j.config.RetryConfigBuilder;
import com.xqbase.etcd4j.EtcdClient;
import com.xqbase.etcd4j.EtcdClientException;

import killrvideo.graph.KillrVideoTraversalSource;

/**
 * Connectivity to DSE (cassandra, graph, search, analytics).
 *
 * @author DataStax evangelist team.
 */
@Configuration
public class DseConfiguration {

	/** Internal logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DseConfiguration.class);
    
    @Value("${killrvideo.cassandra.clustername: 'killrvideo'}")
    public String dseClusterName;
    
    @Value("${killrvideo.graph.timeout: 30000}")
    public Integer graphTimeout;
    
    @Value("${killrvideo.graph.recommendation.name: 'killrvideo_video_recommendations'}")
    public String graphRecommendationName;
    
    @Value("#{environment.KILLRVIDEO_DSE_USERNAME}")
    public Optional < String > dseUsername;
   
    @Value("#{environment.KILLRVIDEO_DSE_PASSWORD}")
    public Optional < String > dsePassword;
    
    @Value("#{environment.KILLRVIDEO_DOCKER_IP}")
    public Optional < String > dockerIp;
    
    @Value("#{environment.KILLRVIDEO_HOST_IP}")
    public Optional < String > serverIp;
    
    @Value("${killrvideo.cassandra.maxNumberOfTries: 10}")
    private int maxNumberOfTries  = 10;
    
    @Value("${killrvideo.cassandra.delayBetweenTries: 2}")
    private int delayBetweenTries = 2;
    
    @Inject
    private EtcdClient etcdClient;
    
    @Bean
    public DseSession initializeDSE() {
         long top = System.currentTimeMillis();
         LOGGER.info("Initializing connection to DSE Cluster...");
         Builder clusterConfig = new Builder();
         // Use to test limit condition
         // clusterConfig.addContactPointsWithPorts(asSocketInetAdress("localhost:9000").get());
         populateContactPoints(clusterConfig);
         populateAuthentication(clusterConfig);
         populateGraphOptions(clusterConfig);
         
         final AtomicInteger atomicCount = new AtomicInteger(1);
         Callable<DseSession> connectionToDse = () -> {
             return clusterConfig.build().connect();
         };
         
         RetryConfig config = new RetryConfigBuilder()
                 .retryOnAnyException()
                  //.retryOnSpecificExceptions(NoHostAvailableException.class)
                 .withMaxNumberOfTries(maxNumberOfTries)
                 .withDelayBetweenTries(delayBetweenTries, ChronoUnit.SECONDS)
                 .withFixedBackoff()
                 .build();
         
         return new CallExecutor<DseSession>(config)
                 .afterFailedTry(s -> { 
                     LOGGER.info("Attempt #{}/{} failed.. trying in {} seconds, waiting Dse to Start", atomicCount.getAndIncrement(),
                             maxNumberOfTries,  delayBetweenTries); })
                 .onFailure(s -> {
                     LOGGER.error("Cannot connection to DSE after {} attempts, exiting", maxNumberOfTries);
                     System.err.println("Can not conenction to DSE after " + maxNumberOfTries + " attempts, exiting");
                     System.exit(500);
                  })
                 .onSuccess(s -> {   
                     long timeElapsed = System.currentTimeMillis() - top;
                     LOGGER.info("Connection etablished to DSE Cluster in {} millis.", timeElapsed);})
                 .execute(connectionToDse).getResult();
    }
    
    @Bean
    public MappingManager initializeMappingManager(DseSession session) {
        // TODO on peut passer le keyspace en dynamique
        // 
        return new MappingManager(session);
    }

    @Bean
    public KillrVideoTraversalSource initialGraphTraversalSource(DseSession session) {
        return DseGraph.traversal(session, KillrVideoTraversalSource.class);
    }
    
    /**
     * Retrieve cluster nodes adresses (eg:node1:9042) from ETCD and initialize the `contact points`,
     * endpoints of Cassandra cluster nodes.
     * 
     * @note 
     * [Initializing Contact Points with Java Driver]
     * 
     * (1) The default port is 9042. If you keep using the default port you
     * do not need to use or `withPort()` or `addContactPointsWithPorts()`, only `addContactPoint()`.
     * 
     * (2) Best practice is to use the SAME PORT for each node and to  setup the port through `withPort()`.
     * 
     * (3) Never, ever use `addContactPointsWithPorts` in clusters : it will ony set port FOR THE FIRST NODE. 
     * DON'T USE, EVEN IF ALL NODE USE SAME PORT. It purpose is only for tests and standalone servers.
     * 
     * (4) What if I have a cluster and nodes do not use the same port (eg: node1:9043, node2:9044, node3:9045) ?
     * You need to use {@link AddressTranslator} as defined below and reference with `withAddressTranslator(translator);`
     * 
     * <code>
     * public class MyClusterAddressTranslator implements AddressTranslator {
     *  @Override
     *  public void init(Cluster cluster) {}
     *  @Override
     *  public void close() {}
     *  
     *  @Override
     *  public InetSocketAddress translate(InetSocketAddress incoming) {
     *     // Given the configuration
     *     Map<String, Integer> clusterNodes = new HashMap<String, Integer>() { {
     *       put("node1", 9043);put("node2", 9044);put("node3", 9045);
     *      }};
     *      String targetHostName = incoming.getHostName();
     *      if (clusterNodes.containsKey(targetHostName)) {
     *          return new InetSocketAddress(targetHostName, clusterNodes.get(targetHostName));
     *      }
     *      throw new IllegalArgumentException("Cannot translate URL " + incoming + " hostName not found");
     *   }
     * }
     * </code>
     * 
     * @param clusterConfig
     *      current configuration
     */
    private void populateContactPoints(Builder clusterConfig)  {
        try {
            // Convert as list of inetAdress removing invalid format
            List < InetSocketAddress > clusterNodeAdresses = 
                    etcdClient.listDir("/killrvideo/services/cassandra")
                      .stream()
                      .map(node -> this.asSocketInetAdress(node.value))
                      .filter(node -> node.isPresent())
                      .map(node -> node.get())
                      .collect(Collectors.toList());

            if (clusterNodeAdresses.isEmpty()) {
                throw new IllegalStateException("Cannot find any valid contact points in ETCD, "
                        + "please check key /killrvideo/services/cassandra");
            }
            
            // Init port for all 
            clusterConfig.withPort(clusterNodeAdresses.get(0).getPort());
            
            //Add allContact points
            clusterNodeAdresses.stream()
                      .map(adress -> adress.getHostName())
                      .forEach(clusterConfig::addContactPoint);
            
        } catch (EtcdClientException e) {
            /**
             * If ETCD is not setup yet we must retry.
             */
            throw new IllegalArgumentException("Cannot retrieve cassandra cluster information from ETCD", e);
        }
    }
    
    /**
     * Check to see if we have username and password from the environment
     * This is here because we have a dual use scenario.  One for developers and others
     * who download KillrVideo and run within a local Docker container and the other
     * who might need (like us for example) to connect KillrVideo up to an external
     * cluster that requires authentication.
     */
    private void populateAuthentication(Builder clusterConfig) {
        if (dseUsername.isPresent() && dsePassword.isPresent() 
                                    && dseUsername.get().length() > 0) {
            AuthProvider cassandraAuthProvider = new DsePlainTextAuthProvider(dseUsername.get(), dsePassword.get());
            clusterConfig.withAuthProvider(cassandraAuthProvider);
            String obfuscatedPassword = new String(new char[dsePassword.get().length()]).replace("\0", "*");
            LOGGER.info(" + Using supplied DSE username: '%s' and password: '%s' from environment variables", 
                        dseUsername.get(), obfuscatedPassword);
        } else {
            LOGGER.info(" + Connection is not authenticated (no username/password)");
        }
    }
    
    private void populateGraphOptions(Builder clusterConfig) {
        GraphOptions go = new GraphOptions();
        go.setGraphName(graphRecommendationName);
        go.setReadTimeoutMillis(graphTimeout);
        go.setGraphSubProtocol(GraphProtocol.GRAPHSON_2_0);
        clusterConfig.withGraphOptions(go);
    }
    
    /**
     * Convert information in ETCD as real adress {@link InetSocketAddress} if possible.
     *
     * @param contactPoint
     *      network node adress information like hostname:port
     * @return
     *      java formatted inet adress
     */
    private Optional<InetSocketAddress> asSocketInetAdress(String contactPoint) {
        Optional<InetSocketAddress> target = Optional.empty();
        try {
            if (contactPoint != null && contactPoint.length() > 0) {
                String[] chunks = contactPoint.split(":");
                if (chunks.length == 2) {
                    LOGGER.info(" + Adding node '{}' to the Cassandra cluster definition", contactPoint);
                    return Optional.of(new InetSocketAddress(InetAddress.getByName(chunks[0]), Integer.parseInt(chunks[1])));
                }
            }
        } catch (NumberFormatException e) {
            LOGGER.warn(" + Cannot read contactPoint - "
                    + "Invalid Port Numer, entry '" + contactPoint + "' will be ignored", e);
        } catch (UnknownHostException e) {
            LOGGER.warn(" + Cannot read contactPoint - "
                    + "Invalid Hostname, entry '" + contactPoint + "' will be ignored", e);
        }
        return target;
    }
   
}
