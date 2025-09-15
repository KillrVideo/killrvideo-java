package com.killrvideo.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom ThreadFactory.
 *
 * @author DataStax Developer Advocates team.
 */
public class KillrVideoThreadFactory implements ThreadFactory {

	/** Create dedicated logger to trace ERRORS. */
    private static final Logger LOGGER = LoggerFactory.getLogger("killrvideo-default-executor");
    
    /** Counter keeping track of thread number in executor. */
    private final AtomicInteger threadNumber = new AtomicInteger(10);
    
    /**
     * Default constructor required for reflection.
     */
    public KillrVideoThreadFactory() {
    }

    /** {@inheritDoc} */
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("killrvideo-default-executor-" + this.threadNumber.incrementAndGet());
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
        return thread;
    }
    
    /**
     * Overriding error handling providing logging.
     */
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (t, e) -> {
        LOGGER.error("Uncaught asynchronous exception : " + e.getMessage(), e);
    };


}
