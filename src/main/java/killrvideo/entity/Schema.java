package killrvideo.entity;

/**
 * Information related to SCHEMA : use to 'decorate' POJO in Mapper, then prepareStatements.
 *
 * @author DataStax evangelist team.
 */
public interface Schema {

    /** Core KeySpace. */
    String KEYSPACE = "killrvideo";
    
    /** Table Names in Keyspace previously defined. */
    String TABLENAME_COMMENTS_BY_USER              = "comments_by_user";
    String TABLENAME_COMMENTS_BY_VIDEO             = "comments_by_video";
    
    String TABLENAME_ENCODING_JOBS_NOTIFICATION    = "encoding_job_notifications";
    String TABLENAME_LATEST_VIDEOS                 = "latest_videos";
    
    String TABLENAME_USERS                         = "users";
    String TABLENAME_USER_CREDENTIALS              = "user_credentials";
    String TABLENAME_USER_VIDEOS                   = "user_videos";
    
    String TABLENAME_VIDEOS                        = "videos";
    String TABLENAME_VIDEOS_RATINGS                = "video_ratings";
    String TABLENAME_VIDEOS_RATINGS_BYUSER         = "video_ratings_by_user";

    String TABLENAME_PLAYBACK_STATS                = "video_playback_stats";
    
    String TABLENAME_VIDEO_RECOMMENDATIONS         = "video_recommendations";
    String TABLENAME_VIDEO_RECOMMENDATIONS_BYVIDEO = "video_recommendations_by_video";
    
}
