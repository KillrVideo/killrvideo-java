package com.killrvideo.service.rating.dao;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.update;
import static com.datastax.oss.driver.api.querybuilder.relation.Relation.column;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.mapper.MapperContext;
import com.datastax.oss.driver.api.mapper.annotations.QueryProvider;
import com.datastax.oss.driver.api.mapper.entity.EntityHelper;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.killrvideo.dse.dao.DseSchema;
import com.killrvideo.dse.utils.DseUtils;
import com.killrvideo.service.rating.dto.VideoRatings;
import com.killrvideo.service.rating.dto.VideoRatingsByUser;


/**
 * Implementation of specific queries for {@link RatingDseDao} interface.
 *
 * @author DataStax Developer Advocates team.
 */
public class RatingDseDaoQueryProvider implements DseSchema {

    private final CqlSession dseSession;
    
    private final EntityHelper<VideoRatingsByUser> entityHelperVideoRatingByUser;
    
    private PreparedStatement psIncVideoRatingsCounters;
    
    private PreparedStatement psInsertVideoRatingsByUser;
    
    /**
     * Constructor invoked by the DataStax driver based on Annotation {@link QueryProvider} 
     * set on class {@link RatingDseDao}.
     * 
     * @param context
     *      context to extrat dse session
     * @param helperVideo
     *      entity helper to interact with bean {@link VideoRatings}
     * @param helperVideoByUser
     *      entity helper to interact with bean {@link VideoRatingsByUser}
     */
    public RatingDseDaoQueryProvider(MapperContext context,
            EntityHelper<VideoRatingsByUser> helperVideoByUser) {
        this.dseSession                    = context.getSession();
        this.entityHelperVideoRatingByUser = helperVideoByUser;
        this.psInsertVideoRatingsByUser    = dseSession.prepare(helperVideoByUser.insert().asCql());
        this.psIncVideoRatingsCounters = dseSession.prepare(update(TABLENAME_VIDEO_RATINGS)
                .increment(RATING_COLUMN_RATING_COUNTER_)
                .increment(RATING_COLUMN_RATING_TOTAL_, QueryBuilder.bindMarker(RATING_COLUMN_RATING_TOTAL_))
                .where(column(RATING_COLUMN_VIDEOID_).isEqualTo(bindMarker(RATING_COLUMN_VIDEOID_)))
                .build());
    }
        
    /** Javadoc in {@link RatingDseDao} */
    public CompletionStage<Void> rateVideo(UUID videoId, UUID userId, Integer rating) {
        return dseSession
                   // (1) Increments counters in video_ratings
                  .executeAsync(psIncVideoRatingsCounters
                    .bind()
                    .setLong(RATING_COLUMN_RATING_TOTAL_, rating)
                    .setUuid(RATING_COLUMN_VIDEOID_, videoId))
                   // (2) Then, add record in video_ratings_by_user
                  .thenApply(rs -> dseSession.executeAsync(
                     DseUtils.bind(psInsertVideoRatingsByUser, 
                     new VideoRatingsByUser(videoId, userId, rating),  
                     entityHelperVideoRatingByUser)))
                  // (3) Finally, mapped to null as CompletionStage<Void>
                  .thenApply(rs ->null);
    }
}
