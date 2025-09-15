package com.killrvideo.messaging.dao;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.google.common.eventbus.EventBus;

/**
 * Wrapping any kind of messages.
 *
 * @author DataStax Developer Advocates team.
 */
@Repository("killrvideo.dao.messaging.memory")
@Profile("messaging_memory")
public class MessagingDaoInMemory implements MessagingDao {
    
    /** Loger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(MessagingDaoInMemory.class);
    
    @Autowired
    private EventBus eventBus;
    
    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Object> sendEvent(String targetDestination, Object event) {
        if (event != null) {
            LOGGER.debug("Publishing '{}' to '{}' ", event.getClass().getSimpleName(), targetDestination);
            eventBus.post(event);
        }
        return CompletableFuture.supplyAsync(() -> { 
            eventBus.post(event);
            return null;
        });
    }

    @Override
    public String getErrorDestination() {
        return "ERROR";
    }   
    
}
