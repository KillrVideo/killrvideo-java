package com.killrvideo.messaging.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;

import killrvideo.common.CommonEvents.ErrorEvent;

/**
 * Common Kafka message handler.
 *
 * @author Cedrick LUNVEN (@clunven)
 */
@Repository("killrvideo.dao.messaging.kafka")
@Profile("messaging_kafka")
public class MessagingDaoKafka implements MessagingDao {
    
    /** Loger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(MessagingDaoKafka.class);
    
    /** Same producer can be used evrytime (as the topicName is stored in {@link ProducerRecord}.) */
    @Autowired
    protected KafkaProducer<String, byte[]> protobufProducer;
    
    /** Common error processing from topic topic-kv-errors. */
    @Autowired
    @Qualifier("kafka.consumer.error")
    private KafkaConsumer<String, byte[]> errorLogger;
    
    @Autowired
    private ErrorProcessor errorProcessor;
     
    /** Error Topic. */
    @Value("${killrvideo.messaging.topics.errors: topic-kv-errors}")
    private String topicErrors;
    
    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Object> sendEvent(String targetDestination, Object event) {
        LOGGER.info("Sending Event '{}' ..", event.getClass().getName());
        byte[] payload = serializePayload(event);
        LOGGER.info("Sending Event '{}' ..", event.getClass().getName());
        
        CompletableFuture<Object> cfv = new CompletableFuture<>();
        FutureCallback<RecordMetadata> myCallback = new FutureCallback<>() {
            public void onFailure(Throwable ex) { cfv.completeExceptionally(ex); }
            public void onSuccess(RecordMetadata rs) { cfv.complete(rs); } 
        };
        Futures.addCallback(
                JdkFutureAdapters.listenInPoolThread(
                        protobufProducer.send(new ProducerRecord<>(targetDestination, payload))), 
                        myCallback, 
                        Executors.newSingleThreadExecutor());
        return cfv;
    }
   
    // -- Common Error Handling --
    
    @PostConstruct
    public void registerErrorConsumer() {
        LOGGER.info("Start consuming events from topic '{}' ..", topicErrors);
        errorLogger.subscribe(Collections.singletonList(topicErrors));
        StreamSupport.stream(errorLogger.poll(Duration.ofSeconds(5)).spliterator(), false)
                    .map(ConsumerRecord::value)
                    .forEach(this::consumeErrorEvent);
    }
    
    /**
     * Reading topic Error so expecting 
     * @param eventErrorPayload
     */
    public void consumeErrorEvent(byte[] eventErrorPayload) {
        try {
            errorProcessor.handle(ErrorEvent.parseFrom(eventErrorPayload));
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("Did not process message in ERROR topic, cannot unserialize", e);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public String getErrorDestination() {
        return topicErrors;
    } 
    
    /**
     * Generic serialization for Protobuf entities.
     * 
     * @param entity
     *      current protobuf stub
     * @return
     *      bimnary payload
     */
    @SuppressWarnings({"rawtypes"})
    private <T> byte[] serializePayload(T entity) {
        // Evaluate as a Protobuf Object
        if (entity instanceof AbstractMessageLite) {
            ByteArrayOutputStream payload = new ByteArrayOutputStream();
            AbstractMessageLite eventProtobuf = (AbstractMessageLite) entity;
            try {
                // Serialization (Binary)
                eventProtobuf.writeTo(payload);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create Kafka message payload s", e);
            }
            // Create 
            return payload.toByteArray();
        } else {
            throw new IllegalArgumentException("Protobuf entity is expected here for last parameter."
                    + "It should inherit from " + AbstractMessageLite.class + " but was " + entity.getClass());
        }
    }
      
}
