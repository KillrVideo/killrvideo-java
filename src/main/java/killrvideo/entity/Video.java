package killrvideo.entity;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import killrvideo.search.SearchServiceOuterClass.SearchResultsVideoPreview;
import killrvideo.suggested_videos.SuggestedVideosService.SuggestedVideoPreview;
import killrvideo.utils.EmptyCollectionIfNull;
import killrvideo.utils.TypeConverter;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.GetVideoResponse;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.VideoLocationType;
import killrvideo.video_catalog.VideoCatalogServiceOuterClass.VideoPreview;

/**
 * Pojo representing DTO for table 'videos'.
 *
 * @author DataStax evangelist team.
 */
@Table(keyspace = Schema.KEYSPACE, name = Schema.TABLENAME_VIDEOS)
public class Video extends AbstractVideo {

    /** Serial. */
    private static final long serialVersionUID = 7035802926837646137L;

    @PartitionKey
    private UUID videoid;

    @NotNull
    @Column
    private UUID userid;

    @Length(min = 1, message = "description must not be empty")
    @Column
    private String description;

    @Length(min = 1, message = "location must not be empty")
    @Column
    private String location;

    @Column(name = "location_type")
    private int locationType;

    @Column
    @EmptyCollectionIfNull
    private Set<String> tags;

    @NotNull
    @Column(name = "added_date")
    private Date addedDate;

    /**
     * Default Constructor allowing reflection.
     */
    public Video() {}
    
    /**
     * Constructor wihout location nor preview.
     */
    public Video(UUID videoid, UUID userid, String name, String description, int locationType, Set<String> tags, Date addedDate) {
        this(videoid, userid, name, description, null, locationType, null, tags, addedDate);
    }

    /**
     * All attributes constructor.
     */
    public Video(UUID videoid, UUID userid, String name, String description, String location, int locationType, String previewImageLocation, Set<String> tags, Date addedDate) {
        super(name, previewImageLocation);
        this.videoid = videoid;
        this.userid = userid;
        this.description = description;
        this.location = location;
        this.locationType = locationType;
        this.tags = tags;
        this.addedDate = addedDate;
    }
    
    /**
     * Mapping to generated GPRC beans (Full detailed)
     */
    public GetVideoResponse toVideoResponse() {
        final GetVideoResponse videoResponse = GetVideoResponse
                .newBuilder()
                .setAddedDate(TypeConverter.dateToTimestamp(addedDate))
                .setDescription(description)
                .setLocation(location)
                .setLocationType(VideoLocationType.forNumber(getLocationType()))
                .setName(name)
                .setUserId(TypeConverter.uuidToUuid(userid))
                .setVideoId(TypeConverter.uuidToUuid(videoid))
                .addAllTags(tags)
                .build();

        return videoResponse;
    }

    /**
     * Mapping to generated GPRC beans (Summary).
     */
    public VideoPreview toVideoPreview() {
        return VideoPreview.newBuilder()
                .setAddedDate(TypeConverter.dateToTimestamp(addedDate))
                .setName(name)
                .setPreviewImageLocation(previewImageLocation)
                .setUserId(TypeConverter.uuidToUuid(userid))
                .setVideoId(TypeConverter.uuidToUuid(videoid))
                .build();
    }

    /**
     * Mapping to generated GPRC beans (Search result special).
     */
    public SearchResultsVideoPreview toResultVideoPreview() {
        return SearchResultsVideoPreview
                .newBuilder()
                .setAddedDate(TypeConverter.dateToTimestamp(addedDate))
                .setName(name)
                .setPreviewImageLocation(previewImageLocation)
                .setUserId(TypeConverter.uuidToUuid(userid))
                .setVideoId(TypeConverter.uuidToUuid(videoid))
                .build();
    }

    /**
     * Mapping to generated GPRC beans. (Suggested videos special)
     */
    public SuggestedVideoPreview toSuggestedVideoPreview() {
        return SuggestedVideoPreview
                .newBuilder()
                .setVideoId(TypeConverter.uuidToUuid(videoid))
                .setAddedDate(TypeConverter.dateToTimestamp(addedDate))
                .setName(name)
                .setPreviewImageLocation(previewImageLocation)
                .setUserId(TypeConverter.uuidToUuid(userid))
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
     * Getter for attribute 'description'.
     *
     * @return
     *       current value of 'description'
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter for attribute 'description'.
     * @param description
     * 		new value for 'description '
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter for attribute 'location'.
     *
     * @return
     *       current value of 'location'
     */
    public String getLocation() {
        return location;
    }

    /**
     * Setter for attribute 'location'.
     * @param location
     * 		new value for 'location '
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Getter for attribute 'locationType'.
     *
     * @return
     *       current value of 'locationType'
     */
    public int getLocationType() {
        return locationType;
    }

    /**
     * Setter for attribute 'locationType'.
     * @param locationType
     * 		new value for 'locationType '
     */
    public void setLocationType(int locationType) {
        this.locationType = locationType;
    }

    /**
     * Getter for attribute 'tags'.
     *
     * @return
     *       current value of 'tags'
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Setter for attribute 'tags'.
     * @param tags
     * 		new value for 'tags '
     */
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    /**
     * Getter for attribute 'addedDate'.
     *
     * @return
     *       current value of 'addedDate'
     */
    public Date getAddedDate() {
        return addedDate;
    }

    /**
     * Setter for attribute 'addedDate'.
     * @param addedDate
     * 		new value for 'addedDate '
     */
    public void setAddedDate(Date addedDate) {
        this.addedDate = addedDate;
    }
    
    
}
