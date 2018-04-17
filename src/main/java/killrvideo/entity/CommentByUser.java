package killrvideo.entity;

import java.util.UUID;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

/**
 * Specialization for USER.
 *
 * @author DataStax evangelist team.
 */
@Table(keyspace = Schema.KEYSPACE, name = Schema.TABLENAME_COMMENTS_BY_USER)
public class CommentByUser extends Comment {
    
    /** Serial. */
    private static final long serialVersionUID = 1453554109222565840L;

    /**
     * Getter for attribute 'userid'.
     *
     * @return
     *       current value of 'userid'
     */
    @PartitionKey
    public UUID getUserid() {
        return userid;
    }

}
