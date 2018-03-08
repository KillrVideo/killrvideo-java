package killrvideo.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.validation.Validation;
import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.eventbus.EventBus;

import killrvideo.async.KillrVideoThreadFactory;

/**
 * Configuration for KillrVideo application leveraging on DSE, ETCD and any external source.
 *
 * @author DataStax evangelist team.
 */
@Configuration
public class KillrVideoConfiguration {
   
    // --- Global Infos
    
    @Value("${killrvideo.application.name:KillrVideo}")
    private String applicationName;
    
    @Value("${killrvideo.application.instance.id: 0}")
    private int applicationInstanceId;
    
    @Value("${killrvideo.server.port: 8899}")
    private int applicationPort;
    
    @Value("#{environment.KILLRVIDEO_HOST_IP}")
    private String applicationHost;
    
    @Value("${killrvideo.cassandra.mutation-error-log: /tmp/killrvideo-mutation-errors.log}")
    private String mutationErrorLog;
    
    // --- ThreadPool Settings
   
    @Value("${killrvideo.threadpool.min.threads:5}")
    private int minThreads;
    
    @Value("${killrvideo.threadpool.max.threads:10}")
    private int maxThreads;
    
    @Value("${killrvideo.thread.ttl.seconds:60}")
    private int threadsTTLSeconds;
    
    @Value("${killrvideo.thread.queue.size:1000}")
    private int threadPoolQueueSize;
    
    // --- Bean definition
    
    @Bean
    public EventBus createEventBus() {
        return new EventBus("killrvideo_event_bus");
    }

    /**
     * Initialize the threadPool.
     *
     * @return
     *      current executor for this
     */
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService threadPool() {
        return new ThreadPoolExecutor(getMinThreads(), getMaxThreads(), 
                getThreadsTTLSeconds(), TimeUnit.SECONDS, 
                new LinkedBlockingQueue<>(getThreadPoolQueueSize()), 
                new KillrVideoThreadFactory());
    }

    @Bean
    public Validator getBeanValidator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * Getter for attribute 'applicationName'.
     *
     * @return
     *       current value of 'applicationName'
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Getter for attribute 'applicationInstanceId'.
     *
     * @return
     *       current value of 'applicationInstanceId'
     */
    public int getApplicationInstanceId() {
        return applicationInstanceId;
    }

    /**
     * Getter for attribute 'applicationPort'.
     *
     * @return
     *       current value of 'applicationPort'
     */
    public int getApplicationPort() {
        return applicationPort;
    }

    /**
     * Getter for attribute 'mutationErrorLog'.
     *
     * @return
     *       current value of 'mutationErrorLog'
     */
    public String getMutationErrorLog() {
        return mutationErrorLog;
    }

    /**
     * Getter for attribute 'minThreads'.
     *
     * @return
     *       current value of 'minThreads'
     */
    public int getMinThreads() {
        return minThreads;
    }

    /**
     * Getter for attribute 'maxThreads'.
     *
     * @return
     *       current value of 'maxThreads'
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * Getter for attribute 'threadsTTLSeconds'.
     *
     * @return
     *       current value of 'threadsTTLSeconds'
     */
    public int getThreadsTTLSeconds() {
        return threadsTTLSeconds;
    }

    /**
     * Getter for attribute 'threadPoolQueueSize'.
     *
     * @return
     *       current value of 'threadPoolQueueSize'
     */
    public int getThreadPoolQueueSize() {
        return threadPoolQueueSize;
    }

    /**
     * Getter for attribute 'applicationHost'.
     *
     * @return
     *       current value of 'applicationHost'
     */
    public String getApplicationHost() {
        return applicationHost;
    }

}
