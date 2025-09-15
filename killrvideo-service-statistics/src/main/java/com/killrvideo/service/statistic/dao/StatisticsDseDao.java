package com.killrvideo.service.statistic.dao;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Query;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import com.killrvideo.service.statistic.dto.VideoPlaybackStats;

/**
 * Service Definitions for Statistic Microservice.
 *
 * @author DataStax Developer Advocates team.
 */
@Dao
public interface StatisticsDseDao {
    
    @Select
    CompletionStage<VideoPlaybackStats> getNumberOfPlaysAsync(UUID videoId);
    
    @Query("UPDATE video_playback_stats SET views+=1 WHERE videoid=:videoid")
    CompletionStage<Void> recordPlaybackStartedAsync(UUID videoId);
    
}
