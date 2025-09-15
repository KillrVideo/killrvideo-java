package com.killrvideo.service.comment.dao.dto;

import java.util.UUID;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.killrvideo.dse.dao.DseSchema;

/**
 * Specialization for USER.
 *
 * @author DataStax Developer Advocates team.
 */
@Entity
@CqlName(DseSchema.TABLENAME_COMMENTS_BY_USER)
public class CommentByUserEntity extends Comment {
    
    /** Serial. */
    private static final long serialVersionUID = 1453554109222565840L;
    
    /**
     * Default constructor.
     */
    public CommentByUserEntity() {}
    
    /**
     * Copy constructor.
     *
     * @param c
     */
    public CommentByUserEntity(Comment c) {
        this.commentid  = c.getCommentid();
        this.userid     = c.getUserid();
        this.videoid    = c.getVideoid();
        this.comment    = c.getComment();
    }

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
