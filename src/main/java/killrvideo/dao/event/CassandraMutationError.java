package killrvideo.dao.event;

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
    public final Object request;
    
    /**
     * Related exception in code.
     */
    public final Throwable throwable;

    /**
     * Default constructor.
     */
    public CassandraMutationError(Object request, Throwable throwable) {
        this.request = request;
        this.throwable = throwable;
    }

    /**
     * Display as an error message.
     */
    public String buildErrorLog() {
        StringBuilder builder = new StringBuilder();
        if (request != null) {
            builder.append(request.toString()).append("\n");
        }
        if (throwable != null ) {
            builder.append(throwable.getMessage()).append("\n");
            builder.append(ExceptionUtils.mergeStackTrace(throwable));
        }
        return builder.toString();
    }
}
