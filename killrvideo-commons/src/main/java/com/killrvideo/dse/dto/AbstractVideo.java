package com.killrvideo.dse.dto;

import java.io.Serializable;

import org.hibernate.validator.constraints.Length;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.killrvideo.dse.dao.DseSchema;

/**
 * Bean representing shared attributes in videos.
 *
 * @author DataStax Developer Advocates team
 */
public abstract class AbstractVideo implements Serializable, DseSchema {

    /** Serial. */
    private static final long serialVersionUID = -4366554197274003003L;
    
    @CqlName(VIDEOS_COLUMN_NAME)
    @Length(min = 1, message = "The video name must not be empty")
    protected String name;

    @CqlName(VIDEOS_COLUMN_PREVIEW)
    protected String previewImageLocation;

    /**
     * Allow default initializations.
     */
    protected AbstractVideo() {}

    /**
     * Constructor used by sub entities.
     * 
     * @param name
     *            video name
     * @param preview
     *            video preview location
     */
    protected AbstractVideo(String name, String preview) {
        this.name = name;
        this.previewImageLocation = preview;
    }
    
    /**
     * Getter for attribute 'name'.
     *
     * @return current value of 'name'
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for attribute 'name'.
     * 
     * @param name
     *            new value for 'name '
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for attribute 'previewImageLocation'.
     *
     * @return current value of 'previewImageLocation'
     */
    public String getPreviewImageLocation() {
        return previewImageLocation;
    }

    /**
     * Setter for attribute 'previewImageLocation'.
     * 
     * @param previewImageLocation
     *            new value for 'previewImageLocation '
     */
    public void setPreviewImageLocation(String previewImageLocation) {
        this.previewImageLocation = previewImageLocation;
    }
}
