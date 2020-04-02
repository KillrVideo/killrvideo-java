package com.killrvideo.service.comment.dao.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.killrvideo.dse.dao.DseSchema;
import com.killrvideo.dse.utils.DseUtils;

/**
 * Bean standing for comment on video.
 *
 * @author DataStax Developer Advocates team.
 */
public class Comment implements Serializable, DseSchema {
    
    /** Serial. */
    private static final long serialVersionUID = 7675521710612951368L;
    
    @NotNull
    @CqlName(COMMENTS_COLUMN_USERID)
    protected UUID userid;
    
    @NotNull
    @CqlName(COMMENTS_COLUMN_VIDEOID)
    protected UUID videoid;

    @NotNull
    @ClusteringColumn
    @CqlName(COMMENTS_COLUMN_COMMENTID)
    protected UUID commentid;

    @Length(min = 1, message = "The comment must not be empty")
    @CqlName(COMMENTS_COLUMN_COMMENT)
    protected String comment;

    //@Computed("toTimestamp("+ COMMENTS_COLUMN_COMMENTID+ ")")
    //private Date dateOfComment;
    
    /**
     * Default constructor.
     */
    public Comment() {
    }
    
    /**
     * Default constructor.
     */
    public Comment(String comment) {
        this.comment = comment;
    }
    
    /**
     * Setter for attribute 'userid'.
     * @param userid
     * 		new value for 'userid '
     */
    public void setUserid(UUID userid) {
        this.userid = userid;
    }

    /**
     * Setter for attribute 'videoid'.
     * @param videoid
     * 		new value for 'videoid '
     */
    public void setVideoid(UUID videoid) {
        this.videoid = videoid;
    }

    /**
     * Getter for attribute 'commentid'.
     *
     * @return
     *       current value of 'commentid'
     */
    public UUID getCommentid() {
        return commentid;
    }

    /**
     * Setter for attribute 'commentid'.
     * @param commentid
     * 		new value for 'commentid '
     */
    public void setCommentid(UUID commentid) {
        this.commentid = commentid;
    }

    /**
     * Getter for attribute 'comment'.
     *
     * @return
     *       current value of 'comment'
     */
    public String getComment() {
        return comment;
    }

    /**
     * Setter for attribute 'comment'.
     * @param comment
     * 		new value for 'comment '
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Getter for attribute 'dateOfComment'.
     *
     * @return
     *       current value of 'dateOfComment'
     */
    public Date getDateOfComment() {
        return new Date(DseUtils.getTimeFromUUID(commentid));
    }

    
    /**
     * Getter for attribute 'userid'.
     *
     * @return
     *       current value of 'userid'
     */
    public UUID getUserid() {
        return userid;
    }
    
    /**
     * Getter for attribute 'videoid'.
     *
     * @return
     *       current value of 'videoid'
     */
    public UUID getVideoid() {
        return videoid;
    }
    

}
