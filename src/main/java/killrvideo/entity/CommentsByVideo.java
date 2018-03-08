package killrvideo.entity;

import static java.util.UUID.fromString;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.Computed;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import killrvideo.comments.CommentsServiceOuterClass;
import killrvideo.comments.CommentsServiceOuterClass.CommentOnVideoRequest;
import killrvideo.utils.TypeConverter;

/**
 * Pojo representing DTO for table 'comments_by_video'
 *
 * @author DataStax evangelist team.
 */
@Table(keyspace = Schema.KEYSPACE, name = Schema.TABLENAME_COMMENTS_BY_VIDEO)
public class CommentsByVideo implements Serializable {

    /** Serial. */
    private static final long serialVersionUID = -6365600205169278881L;

    @PartitionKey
    private UUID videoid;

    @ClusteringColumn
    private UUID commentid;

    @NotNull
    @Column
    private UUID userid;

    @Length(min = 1, message = "The comment must not be empty")
    @Column
    private String comment;

    /**
     * In order to properly use the @Computed annotation for dateOfComment
     * you must execute a query using the mapper with this entity, NOT QueryBuilder.
     * If QueryBuilder is used you must use a call to fcall() and pass the CQL function
     * needed to it directly.  Here is an example pulled from CommentsByVideo.getVideoComments().
     * fcall("toTimestamp", QueryBuilder.column("commentid")).as("comment_timestamp")
     * This will execute the toTimeStamp() function against the commentid column and return the
     * result with an alias of comment_timestamp.  Again, reference CommentService.getUserComments()
     * or CommentService.getVideoComments() for examples of how to implement.
     */
    @NotNull
    @Computed("toTimestamp(commentid)")
    private Date dateOfComment;

    /**
     * Default constructor (reflection)
     */
    public CommentsByVideo() {}

    /**
     * Constructor with all parameters.
     */
    public CommentsByVideo(UUID videoid, UUID commentid, UUID userid, String comment) {
        this.videoid    = videoid;
        this.commentid  = commentid;
        this.userid     = userid;
        this.comment    = comment;
    }

    /**
     * Constructor from GRPC generated request.
     */
    public CommentsByVideo(CommentOnVideoRequest request) {
        this.videoid    = fromString(request.getVideoId().getValue());
        this.commentid  = fromString(request.getCommentId().getValue());
        this.userid     = fromString(request.getUserId().getValue());
        this.comment    = request.getComment();
    }

    /**
     * Mapping to GRPC generated classes.
     */
    public CommentsServiceOuterClass.VideoComment toVideoComment() {
        return CommentsServiceOuterClass.VideoComment
                .newBuilder()
                .setComment(comment)
                .setCommentId(TypeConverter.uuidToTimeUuid(commentid))
                .setUserId(TypeConverter.uuidToUuid(userid))
                .setCommentTimestamp(TypeConverter.dateToTimestamp(dateOfComment))
                .build();
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
     * Getter for attribute 'userid'.
     *
     * @return
     *       current value of 'userid'
     */
    public UUID getUserid() {
        return userid;
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
        return dateOfComment;
    }

    /**
     * Setter for attribute 'dateOfComment'.
     * @param dateOfComment
     * 		new value for 'dateOfComment '
     */
    public void setDateOfComment(Date dateOfComment) {
        this.dateOfComment = dateOfComment;
    }
    
    
}
