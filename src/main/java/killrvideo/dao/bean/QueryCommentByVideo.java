package killrvideo.dao.bean;

import java.util.Optional;
import java.util.UUID;

/**
 * Query to search comments for a User.
 *
 * @author DataStax evangelist team.
 */
public class QueryCommentByVideo extends QueryDefinition {
    
    /** Serial. */
    private static final long serialVersionUID = 7721676513515347779L;

    /** User uniaue identifier. */
    private UUID videoId = null;
    
    /** Comment offset if specified (Optional) */
    private Optional< UUID > commentId = Optional.empty();

    /**
     * Getter for attribute 'commentId'.
     *
     * @return
     *       current value of 'commentId'
     */
    public Optional<UUID> getCommentId() {
        return commentId;
    }

    /**
     * Setter for attribute 'commentId'.
     * @param commentId
     * 		new value for 'commentId '
     */
    public void setCommentId(Optional<UUID> commentId) {
        this.commentId = commentId;
    }

    /**
     * Getter for attribute 'videoId'.
     *
     * @return
     *       current value of 'videoId'
     */
    public UUID getVideoId() {
        return videoId;
    }

    /**
     * Setter for attribute 'videoId'.
     * @param videoId
     * 		new value for 'videoId '
     */
    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }
    
}
