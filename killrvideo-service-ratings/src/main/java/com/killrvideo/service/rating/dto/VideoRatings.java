package com.killrvideo.service.rating.dto;

import java.io.Serializable;
import java.util.UUID;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.killrvideo.dse.dao.DseSchema;

/**
 * Pojo representing DTO for table 'video_ratings'.
 * 
 * @author DataStax Developer Advocates team.
 */
@Entity
public class VideoRatings implements Serializable, DseSchema {

    /** Serial. */
    private static final long serialVersionUID = -8874199914791405808L;
    
    @PartitionKey
    @CqlName(RATING_COLUMN_VIDEOID)
    private UUID videoid;

    @CqlName(RATING_COLUMN_RATING_COUNTER)
    private Long ratingCounter;

    @CqlName(RATING_COLUMN_RATING_TOTAL)
    private Long ratingTotal;

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
     * Getter for attribute 'ratingCounter'.
     *
     * @return
     *       current value of 'ratingCounter'
     */
    public Long getRatingCounter() {
        return ratingCounter;
    }

    /**
     * Setter for attribute 'ratingCounter'.
     * @param ratingCounter
     * 		new value for 'ratingCounter '
     */
    public void setRatingCounter(Long ratingCounter) {
        this.ratingCounter = ratingCounter;
    }

    /**
     * Getter for attribute 'ratingTotal'.
     *
     * @return
     *       current value of 'ratingTotal'
     */
    public Long getRatingTotal() {
        return ratingTotal;
    }

    /**
     * Setter for attribute 'ratingTotal'.
     * @param ratingTotal
     * 		new value for 'ratingTotal '
     */
    public void setRatingTotal(Long ratingTotal) {
        this.ratingTotal = ratingTotal;
    }

}
