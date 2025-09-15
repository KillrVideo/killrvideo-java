package com.killrvideo.service.rating.dao;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.QueryProvider;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import com.killrvideo.service.rating.dto.VideoRatings;
import com.killrvideo.service.rating.dto.VideoRatingsByUser;

/**
 * Operations against DSE (Cassandra Part) to implements ratings
 * 
 * @author DataStax Developer Advocates team.
 */
@Dao
public interface RatingDseDao {
    
    /**
     * Create a rating.
     *
     * @param videoId
     *      current videoId
     * @param userId
     *      current userid
     * @param rating
     *      current rating
     */
    @QueryProvider(providerClass = RatingDseDaoQueryProvider.class, 
                   entityHelpers = { VideoRatingsByUser.class})
    CompletionStage<Void> rateVideo( UUID videoId, UUID userId, Integer rating);
    
    /**
     * VideoId matches the partition key set in the VideoRating class.
     * 
     * @param videoId
     *      unique identifier for video.
     * @return
     *      find rating
     */
    @Select
    CompletionStage< Optional < VideoRatings > > findRating(UUID videoId);
    
    /**
     * Find rating from videoid and userid.
     *
     * @param videoId
     *      current videoId
     * @param userid
     *      current user unique identifier.
     * @return
     *      video rating is exist.
     */
    @Select
    CompletionStage< Optional < VideoRatingsByUser > > findUserRating(UUID videoId, UUID userid);

}
