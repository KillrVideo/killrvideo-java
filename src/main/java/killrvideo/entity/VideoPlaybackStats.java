package killrvideo.entity;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import killrvideo.statistics.StatisticsServiceOuterClass.PlayStats;
import killrvideo.utils.TypeConverter;

/**
 * Pojo representing DTO for table 'video_playback_stats'.
 *
 * @author DataStax evangelist team.
 */
@Table(keyspace = Schema.KEYSPACE, name = Schema.TABLENAME_PLAYBACK_STATS)
public class VideoPlaybackStats implements Serializable {

    /** Serial. */
    private static final long serialVersionUID = -8636413035520458200L;

    @PartitionKey
    private UUID videoid;

    /**
     * "views" column is a counter type in the underlying DSE database.  As of driver version 3.2 there
     * is no "@Counter" annotation that I know of.  No worries though, just use the incr() function
     * while using the QueryBuilder.  Something similar to with(QueryBuilder.incr("views")).
     */
    @Column
    private Long views;

    /**
     * Mapping to generated GPRC beans.
     */
    public PlayStats toPlayStats() {
        return PlayStats.newBuilder()
                .setVideoId(TypeConverter.uuidToUuid(videoid))
                .setViews(Optional.ofNullable(views).orElse(0L))
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
     * Getter for attribute 'views'.
     *
     * @return
     *       current value of 'views'
     */
    public Long getViews() {
        return views;
    }

    /**
     * Setter for attribute 'views'.
     * @param views
     * 		new value for 'views '
     */
    public void setViews(Long views) {
        this.views = views;
    }
    
    
}
