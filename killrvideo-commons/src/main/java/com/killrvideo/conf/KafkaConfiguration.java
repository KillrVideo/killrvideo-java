package com.killrvideo.conf;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Use Kafka to exchange messages between services. 
 *
 * @author Cedrick LUNVEN (@clunven)
 */
@Configuration
@Profile("messaging_kafka")
public class KafkaConfiguration {
    
    /** Initialize dedicated connection to ETCD system. */
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConfiguration.class);
  
    @Value("${killrvideo.kafka.port: 8082}")
    private int kafkaPort;
    
    @Value("${killrvideo.kafka.brokers:kafka}")
    private String kafkaBrokers;
    
    @Value("#{environment.KILLRVIDEO_KAFKA_BROKERS}")
    private Optional<String> kafkaBrokersEnvironmentVar;
    
    @Value("${killrvideo.kafka.consumerGroup: killrvideo }")
    private String consumerGroup;
    
    @Value("${killrvideo.kafka.ack: 1 }")
    private String producerAck;
    
    private String connectionURL;
    
    /**
     * Should we init connection with Env VAR or values in `application.yaml`
     *
     * @return
     *      target kafka adress
     */
    private String getKafkaServerConnectionUrl() {
        if (null == connectionURL ) {
            if (!kafkaBrokersEnvironmentVar.isEmpty() && !kafkaBrokersEnvironmentVar.get().isBlank()) {
                kafkaBrokers = kafkaBrokersEnvironmentVar.get();
                LOGGER.info(" + Reading broker from KILLRVIDEO_KAFKA_BROKERS");
            }
            connectionURL = String.join(",", Arrays.asList(kafkaBrokers.split(","))
                                  .stream().map(ip -> ip + ":" + kafkaPort)
                                  .collect(Collectors.toList()));
        }
        return connectionURL;
    }
    
    @Bean("kafka.producer")
    public KafkaProducer<String, byte[]> jsonProducer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG,      getKafkaServerConnectionUrl());
        props.put(KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ACKS_CONFIG,                   producerAck);
        return new KafkaProducer<String, byte[]>(props);
    }

    @Bean("kafka.consumer.videoRating")
    public KafkaConsumer<String, byte[]> videoRatingConsumer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG,        getKafkaServerConnectionUrl());
        props.put(GROUP_ID_CONFIG,                 consumerGroup);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class.getName());
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        return new KafkaConsumer<String,byte[]>(props);
    }
    
    @Bean("kafka.consumer.userCreating")
    public KafkaConsumer<String, byte[]> userCreatingConsumer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG,        getKafkaServerConnectionUrl());
        props.put(GROUP_ID_CONFIG,                 consumerGroup);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class.getName());
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        return new KafkaConsumer<String,byte[]>(props);
    }
    
    @Bean("kafka.consumer.videoCreating")
    public KafkaConsumer<String, byte[]> videoCreatingConsumer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG,        getKafkaServerConnectionUrl());
        props.put(GROUP_ID_CONFIG,                 consumerGroup);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class.getName());
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        return new KafkaConsumer<String,byte[]>(props);
    }
    
    @Bean("kafka.consumer.error")
    public KafkaConsumer<String, byte[]> errorConsumer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG,        getKafkaServerConnectionUrl());
        props.put(GROUP_ID_CONFIG,                 consumerGroup);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class.getName());
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        return new KafkaConsumer<String,byte[]>(props);
    }
    
}
