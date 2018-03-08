package killrvideo.configuration;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xqbase.etcd4j.EtcdClient;

/**
 * Connectivity to ETCD.
 *
 * @author DataStax evangelist team.
 */
@Configuration
public class EtcdConfiguration {

    /** Initialize dedicated connection to ETCD system. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdConfiguration.class);
    
    /**
     *  Expected 'KILLRVIDEO_DOCKER_IP' env variable
     *  Then, if not present put 10.0.75.1 as a default value (dockerNAT assigns 10.0.75.1 on MAC)
     */
    @Value("#{environment.KILLRVIDEO_DOCKER_IP ?: '10.0.75.1'}")
    private String etcdServerHost;
    
    /** 
     * Retrieve expected from application.properties/application .yaml files
     * Then, if not find use default value 2379
     */
    @Value("${killrvideo.etcd.port: 2379}")
    private int etcdServerPort;
   
    @Bean
    public EtcdClient connectToEtcd() {
        final String etcdUrl = String.format("http://%s:%d", etcdServerHost, etcdServerPort);
        LOGGER.info("Initializing connection to ETCD Server");
        LOGGER.info(" + Contact '{}'", etcdUrl);
        EtcdClient etcdClient = new EtcdClient(URI.create(etcdUrl));
        LOGGER.info("Connection etablished to ETCD Server");
        return etcdClient;
    }
    
    /*@Bean
    public mousio.etcd4j.EtcdClient connectToEtcd2() {
        return new mousio.etcd4j.EtcdClient(
                URI.create(String.format("http://%s:%d", etcdServerHost, etcdServerPort)));
    }*/
    
}
