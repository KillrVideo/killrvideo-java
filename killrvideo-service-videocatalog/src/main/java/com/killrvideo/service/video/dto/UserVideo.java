package com.killrvideo.service.video.dto;

import java.time.Instant;
import java.util.UUID;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.killrvideo.dse.dao.DseSchema;
import com.killrvideo.dse.dto.Video;

/**
 * Pojo representing DTO for table 'user_videos'
 *
 * @author DataStax Developer Advocates team.
 */
@Entity
@CqlName(DseSchema.TABLENAME_USERS_VIDEO)
public class UserVideo extends VideoPreview {

    /** Serial. */
    private static final long serialVersionUID = -4689177834790056936L;
    
    @PartitionKey
    @CqlName(USERVIDEOS_COLUMN_USERID)
    private UUID userid;

    /**
     * Default Constructor allowing reflection.
     */
    public UserVideo() {}

    /**
     * Constructor workin with {@link Video}.
     */
    public UserVideo(Video video) {
        this(video.getUserid(), video.getVideoid(), video.getName(), 
             video.getPreviewImageLocation(), video.getAddedDate());
    }
    
    /**
     * Constructor without preview.
     */
    public UserVideo(UUID userid, UUID videoid, String name, Instant addedDate) {
        this(userid, videoid, name, null, addedDate);
    }

    /**
     * Full set constructor.
     */
    public UserVideo(UUID userid, UUID videoid, String name, String previewImageLocation, Instant addedDate) {
        super(name, previewImageLocation, addedDate, videoid);
        this.userid = userid;
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
