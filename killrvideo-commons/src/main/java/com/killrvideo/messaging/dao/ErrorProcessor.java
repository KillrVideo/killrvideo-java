package com.killrvideo.messaging.dao;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

import killrvideo.common.CommonEvents.ErrorEvent;

/**
 * Catch exceptions and create a {@link CassandraMutationError} in EventBus.
 *
 * @author DataStax Developer Advocates team.
 */
@Component
public class ErrorProcessor {

    /** LOGGER for the class. */
    private static Logger LOGGER = LoggerFactory.getLogger(ErrorProcessor.class);
    
    @Value("${killrvideo.dse.mutation-error-log:/tmp/killrvideo-mutation-errors.log}")
    private String mutationErrorLog;
    
    private PrintWriter errorLogFile;

    @PostConstruct
    public void openErrorLogFile() throws IOException {
        File logFile = new File(getMutationErrorLog());
        if (!logFile.exists()) {
            logFile.createNewFile();
        }
        this.errorLogFile = new PrintWriter(getMutationErrorLog());
    }

    /**
     * Here we just record the original Grpc request so that we can replay
     * them later.
     *
     * An alternative impl can just push the request to a message queue or
     * event bus so that it can be handled by another micro-service
     *
     */
    @Subscribe
    public void handle(final ErrorEvent errorEvent) {
        String msg = String.format("Recording mutation error %s", errorEvent.getErrorMessage() + errorEvent.getErrorStack());
        LOGGER.error(msg);
        errorLogFile.append(msg).append("\n***********************\n");
        errorLogFile.flush();
    }

    /**
     * Closing log file.
     */
    @PreDestroy
    public void closeErrorLogFile() {
        this.errorLogFile.close();
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

}
