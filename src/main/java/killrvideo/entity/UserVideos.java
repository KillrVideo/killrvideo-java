package killrvideo.entity;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import killrvideo.utils.TypeConverter;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.VideoPreview;

/**
 * Pojo representing DTO for table 'user_videos'
 *
 * @author DataStax evangelist team.
 */
@Table(keyspace = Schema.KEYSPACE, name = Schema.TABLENAME_USER_VIDEOS)
public class UserVideos extends AbstractVideoList {

    /** Serial. */
    private static final long serialVersionUID = -4689177834790056936L;
    
    @PartitionKey
    private UUID userid;

    /**
     * Deafult Constructor allowing reflection.
     */
    public UserVideos() {}

    /**
     * Constructor without preview.
     */
    public UserVideos(UUID userid, UUID videoid, String name, Date addedDate) {
        this(userid, videoid, name, null, addedDate);
    }

    /**
     * Full set constructor.
     */
    public UserVideos(UUID userid, UUID videoid, String name, String previewImageLocation, Date addedDate) {
        super(name, previewImageLocation, addedDate, videoid);
        this.userid = userid;
    }

    /**
     * Mapping to generated GPRC beans.
     */
    public VideoPreview toVideoPreview() {
        return VideoPreview
                .newBuilder()
                .setAddedDate(TypeConverter.dateToTimestamp(getAddedDate()))
                .setName(getName())
                .setPreviewImageLocation(Optional
                        .ofNullable(previewImageLocation)
                        .orElse("N/A"))
                .setUserId(TypeConverter.uuidToUuid(getUserid()))
                .setVideoId(TypeConverter.uuidToUuid(getVideoid()))
                .build();
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
    
    
}
