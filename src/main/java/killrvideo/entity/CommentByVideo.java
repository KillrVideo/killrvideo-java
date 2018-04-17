package killrvideo.entity;

import java.util.UUID;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

/**
 * Specialization for VIDEO.
 *
 * @author DataStax evangelist team.
 */
@Table(keyspace = Schema.KEYSPACE, name = Schema.TABLENAME_COMMENTS_BY_VIDEO)
public class CommentByVideo extends Comment {
    
    /** Serial. */
    private static final long serialVersionUID = -6738790629520080307L;

    /**
     * Getter for attribute 'videoid'.
     *
     * @return
     *       current value of 'videoid'
     */
    @PartitionKey
    public UUID getVideoid() {
        return videoid;
    }
    
    
}
