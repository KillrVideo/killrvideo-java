package com.killrvideo.service.video.dto;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.killrvideo.dse.dao.DseSchema;
import com.killrvideo.dse.dto.Video;

/**
 * Pojo representing DTO for table 'latest_videos'
 *
 * @author DataStax Developer Advocates team.
 */
@Entity
@CqlName(DseSchema.TABLENAME_LATEST_VIDEO)
public class LatestVideo extends VideoPreview {

    /** Serial. */
   private static final long serialVersionUID = -8527565276521920973L;
    
    @PartitionKey
    @CqlName(LATESTVIDEOS_COLUMN_YYYYMMDD)
    private String yyyymmdd;

    @CqlName(LATESTVIDEOS_COLUMN_USERID)
    private UUID userid;

    /**
     * Default constructor.
     */
    public LatestVideo() {}

    /**
     * Initialization from a video bean.
     * 
     * @param v
     *      incoming video
     */
    public LatestVideo(Video v) {
        super(v.getName(), v.getPreviewImageLocation(), v.getAddedDate(), v.getVideoid());
        this.userid = v.getUserid();
        this.yyyymmdd = FORMATTER_DAY.format(Date.from(v.getAddedDate()));
    }
    
    /**
     * Constructor with all parameters.
     */
    public LatestVideo(String yyyymmdd, UUID userid, UUID videoid, String name, String previewImageLocation, Instant addedDate) {
        super(name, previewImageLocation, addedDate, videoid);
        this.yyyymmdd = yyyymmdd;
        this.userid = userid;
    }
    
    /**
     * Getter for attribute 'yyyymmdd'.
     *
     * @return
     *       current value of 'yyyymmdd'
     */
    public String getYyyymmdd() {
        return yyyymmdd;
    }

    /**
     * Setter for attribute 'yyyymmdd'.
     * @param yyyymmdd
     * 		new value for 'yyyymmdd '
     */
    public void setYyyymmdd(String yyyymmdd) {
        this.yyyymmdd = yyyymmdd;
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
