package com.killrvideo.service.comment.dao.dto;

import java.util.UUID;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.killrvideo.dse.dao.DseSchema;

/**
 * Specialization for VIDEO.
 *
 * @author DataStax Developer Advocates team.
 */
@Entity
@CqlName(DseSchema.TABLENAME_COMMENTS_BY_VIDEO)
public class CommentByVideoEntity extends Comment {
    
    /** Serial. */
    private static final long serialVersionUID = -6738790629520080307L;
    
    public CommentByVideoEntity() {
    }
    
    public CommentByVideoEntity(Comment c) {
        this.commentid  = c.getCommentid();
        this.userid     = c.getUserid();
        this.videoid    = c.getVideoid();
        this.comment    = c.getComment();
    }

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
