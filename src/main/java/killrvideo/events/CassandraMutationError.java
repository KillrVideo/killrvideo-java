package killrvideo.events;

import com.google.protobuf.GeneratedMessageV3;

import killrvideo.utils.ExceptionUtils;

/**
 * Error bean to be published in BUS.
 *
 * @author DataStax evangelist team.
 */
public class CassandraMutationError {

    /**
     * Failed Protobuf requests.
     */
    public final GeneratedMessageV3 request;
    
    /**
     * Related exception in code.
     */
    public final Throwable throwable;

    /**
     * Default constructor.
     */
    public CassandraMutationError(GeneratedMessageV3 request, Throwable throwable) {
        this.request = request;
        this.throwable = throwable;
    }

    /**
     * Display as an error message.
     */
    public String buildErrorLog() {
        StringBuilder builder = new StringBuilder();
        builder.append(request.toString()).append("\n");
        builder.append(throwable.getMessage()).append("\n");
        builder.append(ExceptionUtils.mergeStackTrace(throwable));
        return builder.toString();
    }
}
