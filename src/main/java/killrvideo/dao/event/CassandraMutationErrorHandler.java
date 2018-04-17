package killrvideo.dao.event;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

import killrvideo.configuration.KillrVideoConfiguration;

/**
 * Catch exceptions and create a {@link CassandraMutationError} in EventBus.
 *
 * @author DataStax evangelist team.
 */
@Component
public class CassandraMutationErrorHandler {

    /** LOGGER for the class. */
    private static Logger LOGGER = LoggerFactory.getLogger(CassandraMutationErrorHandler.class);

    @Inject
    private KillrVideoConfiguration config;

    private PrintWriter errorLogFile;

    @PostConstruct
    public void openErrorLogFile() throws FileNotFoundException {
        this.errorLogFile = new PrintWriter(config.getMutationErrorLog());
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
    public void handle(CassandraMutationError mutationError) {
        final String errorLog = mutationError.buildErrorLog();
        LOGGER.debug(String.format("Recording mutation error %s", errorLog));
        errorLogFile.append(errorLog).append("\n***********************\n");
        errorLogFile.flush();
    }

    @PreDestroy
    public void closeErrorLogFile() {
        this.errorLogFile.close();
    }

}
