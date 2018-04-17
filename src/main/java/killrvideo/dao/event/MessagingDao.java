package killrvideo.dao.event;

import java.time.Instant;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.google.common.eventbus.EventBus;

import killrvideo.comments.CommentsServiceOuterClass.CommentOnVideoRequest;
import killrvideo.comments.events.CommentsEvents.UserCommentedOnVideo;
import killrvideo.utils.TypeConverter;

@Repository
public class MessagingDao {
    
    /** Loger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(MessagingDao.class);
    
    @Inject
    private EventBus eventBus;
    
    /**
     * Publish error Message.
     *
     * @param request
     *      current request to serialize
     * @param error
     *      raised exception during treatment
     */
    public void publishExceptionEvent(Object request, Throwable error) {
        LOGGER.error("Exception commenting on video {} }", error);
        eventBus.post(new CassandraMutationError(request, error));
    }
    
    public void publishCommentCreateEvent(CommentOnVideoRequest request, Instant commentCreationDate) {
        eventBus.post(UserCommentedOnVideo.newBuilder()
                .setCommentId(request.getCommentId())
                .setVideoId(request.getVideoId())
                .setUserId(request.getUserId())
                .setCommentTimestamp(TypeConverter.instantToTimeStamp(commentCreationDate))
                .build());
    }

}
