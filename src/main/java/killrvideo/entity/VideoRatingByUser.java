package killrvideo.entity;

import java.io.Serializable;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import killrvideo.ratings.RatingsServiceOuterClass.GetUserRatingResponse;
import killrvideo.utils.TypeConverter;

/**
 * Pojo representing DTO for table 'video_ratings_by_user'.
 *
 * @author DataStax evangelist team.
 */
@Table(keyspace = Schema.KEYSPACE, name = Schema.TABLENAME_VIDEOS_RATINGS_BYUSER)
public class VideoRatingByUser implements Serializable {

    /** Serial. */
    private static final long serialVersionUID = 7124040203261999049L;

    @PartitionKey
    private UUID videoid;

    @ClusteringColumn
    private UUID userid;

    @Column
    private int rating;

    /**
     * Default constructor (reflection)
     */
    public VideoRatingByUser() {}

    /**
     * Constructor with all parameters.
     */
    public VideoRatingByUser(UUID videoid, UUID userid, int rating) {
        this.videoid = videoid;
        this.userid = userid;
        this.rating = rating;
    }

    /**
     * Mapping to generated GPRC beans.
     */
    public GetUserRatingResponse toUserRatingResponse() {
        return GetUserRatingResponse
                .newBuilder()
                .setUserId(TypeConverter.uuidToUuid(userid))
                .setVideoId(TypeConverter.uuidToUuid(videoid))
                .setRating(rating)
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
     * Getter for attribute 'rating'.
     *
     * @return
     *       current value of 'rating'
     */
    public int getRating() {
        return rating;
    }

    /**
     * Setter for attribute 'rating'.
     * @param rating
     * 		new value for 'rating '
     */
    public void setRating(int rating) {
        this.rating = rating;
    }
    
}
