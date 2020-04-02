package com.killrvideo.dse.dao;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

import com.datastax.oss.driver.api.core.CqlIdentifier;

/**
 * Information related to SCHEMA : use to 'decorate' POJO in Mapper, then prepareStatements.
 *
 * @author DataStax Developer Advocates team.
 */
public interface DseSchema {
    
    SimpleDateFormat  FORMATTER_DAY                = new SimpleDateFormat("yyyyMMdd");
    DateTimeFormatter DATEFORMATTER                = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    CqlIdentifier SOLR_QUERY                       = CqlIdentifier.fromCql("solr_query");
    
    // user_credentials
    String TABLENAME_USER_CREDENTIALS              = "user_credentials";
    String USERCREDENTIAL_COLUMN_USERID            = "userid" ;
    String USERCREDENTIAL_COLUMN_PASSWORD          = "\"password\"";
    String USERCREDENTIAL_COLUMN_EMAIL             = "email";
    CqlIdentifier TABLENAME_USER_CREDENTIALS_      = CqlIdentifier.fromCql(TABLENAME_USER_CREDENTIALS);
    CqlIdentifier USERCREDENTIAL_COLUMN_USERID_    = CqlIdentifier.fromCql(USERCREDENTIAL_COLUMN_USERID);
    CqlIdentifier USERCREDENTIAL_COLUMN_PASSWORD_  = CqlIdentifier.fromCql(USERCREDENTIAL_COLUMN_PASSWORD);
    CqlIdentifier USERCREDENTIAL_COLUMN_EMAIL_     = CqlIdentifier.fromCql(USERCREDENTIAL_COLUMN_EMAIL);
    
    // users
    String TABLENAME_USERS                    = "users";
    String USER_COLUMN_USERID                 = "userid";
    String USER_COLUMN_FIRSTNAME              = "firstname";
    String USER_COLUMN_LASTNAME               = "lastname";
    String USER_COLUMN_EMAIL                  = "email";
    String USER_COLUMN_CREATE                 = "created_date";
    CqlIdentifier TABLENAME_USERS_            = CqlIdentifier.fromCql(TABLENAME_USERS);
    CqlIdentifier USER_COLUMN_USERID_         = CqlIdentifier.fromCql(USER_COLUMN_USERID);
    CqlIdentifier USER_COLUMN_FIRSTNAME_      = CqlIdentifier.fromCql(USER_COLUMN_FIRSTNAME);
    CqlIdentifier USER_COLUMN_LASTNAME_       = CqlIdentifier.fromCql(USER_COLUMN_LASTNAME);
    CqlIdentifier USER_COLUMN_EMAIL_          = CqlIdentifier.fromCql(USER_COLUMN_EMAIL);
    CqlIdentifier USER_COLUMN_CREATE_         = CqlIdentifier.fromCql(USER_COLUMN_CREATE);
    
    // videos
    String TABLENAME_VIDEOS                   = "videos";
    String VIDEOS_COLUMN_VIDEOID              = "videoid";
    String VIDEOS_COLUMN_USERID               = "userid";
    String VIDEOS_COLUMN_NAME                 = "name";
    String VIDEOS_COLUMN_DESCRIPTION          = "description";
    String VIDEOS_COLUMN_LOCATION             = "location";
    String VIDEOS_COLUMN_LOCATIONTYPE         = "location_type";
    String VIDEOS_COLUMN_PREVIEW              = "preview_image_location";
    String VIDEOS_COLUMN_TAGS                 = "tags";
    String VIDEOS_COLUMN_ADDED_DATE           = "added_date";
    CqlIdentifier TABLENAME_VIDEOS_           = CqlIdentifier.fromCql(TABLENAME_VIDEOS);
    CqlIdentifier VIDEOS_COLUMN_VIDEOID_      = CqlIdentifier.fromCql(VIDEOS_COLUMN_VIDEOID);
    CqlIdentifier VIDEOS_COLUMN_USERID_       = CqlIdentifier.fromCql(VIDEOS_COLUMN_USERID);
    CqlIdentifier VIDEOS_COLUMN_NAME_         = CqlIdentifier.fromCql(VIDEOS_COLUMN_NAME);
    CqlIdentifier VIDEOS_COLUMN_DESCRIPTION_  = CqlIdentifier.fromCql(VIDEOS_COLUMN_DESCRIPTION);
    CqlIdentifier VIDEOS_COLUMN_LOCATION_     = CqlIdentifier.fromCql(VIDEOS_COLUMN_LOCATION);
    CqlIdentifier VIDEOS_COLUMN_LOCATIONTYPE_ = CqlIdentifier.fromCql(VIDEOS_COLUMN_LOCATIONTYPE);
    CqlIdentifier VIDEOS_COLUMN_PREVIEW_      = CqlIdentifier.fromCql(VIDEOS_COLUMN_PREVIEW);
    CqlIdentifier VIDEOS_COLUMN_TAGS_         = CqlIdentifier.fromCql(VIDEOS_COLUMN_TAGS);
    CqlIdentifier VIDEOS_COLUMN_ADDED_DATE_   = CqlIdentifier.fromCql(VIDEOS_COLUMN_ADDED_DATE);
    
    // user_videos
    String TABLENAME_USERS_VIDEO               = "user_videos";
    String USERVIDEOS_COLUMN_USERID            = "userid";
    CqlIdentifier TABLENAME_USERS_VIDEO_       = CqlIdentifier.fromCql(TABLENAME_USERS_VIDEO);
    CqlIdentifier USERVIDEOS_COLUMN_USERID_    = CqlIdentifier.fromCql(USERVIDEOS_COLUMN_USERID);
    
    // latest_videos
    String TABLENAME_LATEST_VIDEO               = "latest_videos";
    String LATESTVIDEOS_COLUMN_YYYYMMDD         = "yyyymmdd";
    String LATESTVIDEOS_COLUMN_VIDEOID          = "videoid";
    String LATESTVIDEOS_COLUMN_USERID           = "userid";
    CqlIdentifier TABLENAME_LATEST_VIDEO_       = CqlIdentifier.fromCql(TABLENAME_LATEST_VIDEO);
    CqlIdentifier LATESTVIDEOS_COLUMN_YYYYMMDD_ = CqlIdentifier.fromCql(LATESTVIDEOS_COLUMN_YYYYMMDD);
    CqlIdentifier LATESTVIDEOS_COLUMN_VIDEOID_  = CqlIdentifier.fromCql(LATESTVIDEOS_COLUMN_VIDEOID);
    CqlIdentifier LATESTVIDEOS_COLUMN_USERID_   = CqlIdentifier.fromCql(LATESTVIDEOS_COLUMN_USERID);
    
    
    // comments_by_video + comments_by_user
    String TABLENAME_COMMENTS_BY_USER           = "comments_by_user";
    String TABLENAME_COMMENTS_BY_VIDEO          = "comments_by_video";
    String COMMENTS_COLUMN_VIDEOID              = "videoid";
    String COMMENTS_COLUMN_USERID               = "userid";
    String COMMENTS_COLUMN_COMMENTID            = "commentid";
    String COMMENTS_COLUMN_COMMENT              = "comment";
    CqlIdentifier TABLENAME_COMMENTS_BY_USER_   = CqlIdentifier.fromCql(TABLENAME_COMMENTS_BY_USER);
    CqlIdentifier TABLENAME_COMMENTS_BY_VIDEO_  = CqlIdentifier.fromCql(TABLENAME_COMMENTS_BY_VIDEO);
    CqlIdentifier COMMENTS_COLUMN_VIDEOID_      = CqlIdentifier.fromCql(COMMENTS_COLUMN_VIDEOID);
    CqlIdentifier COMMENTS_COLUMN_USERID_       = CqlIdentifier.fromCql(COMMENTS_COLUMN_USERID);
    CqlIdentifier COMMENTS_COLUMN_COMMENTID_    = CqlIdentifier.fromCql(COMMENTS_COLUMN_COMMENTID);
    CqlIdentifier COMMENTS_COLUMN_COMMENT_      = CqlIdentifier.fromCql(COMMENTS_COLUMN_COMMENT);
    
    // video_ratings + video_ratings_by_user
    String TABLENAME_VIDEO_RATINGS              = "video_ratings";
    String TABLENAME_VIDEO_RATINGS_BYUSER       = "video_ratings_by_user";
    String RATING_COLUMN_RATING                 = "rating";
    String RATING_COLUMN_RATING_COUNTER         = "rating_counter";
    String RATING_COLUMN_RATING_TOTAL           = "rating_total";
    String RATING_COLUMN_VIDEOID                = "videoid";
    String RATING_COLUMN_USERID                 = "userid";
    CqlIdentifier TABLENAME_VIDEO_RATINGS_        = CqlIdentifier.fromCql(TABLENAME_VIDEO_RATINGS);
    CqlIdentifier TABLENAME_VIDEO_RATINGS_BYUSER_ = CqlIdentifier.fromCql(TABLENAME_VIDEO_RATINGS_BYUSER);
    CqlIdentifier RATING_COLUMN_RATING_COUNTER_   = CqlIdentifier.fromCql(RATING_COLUMN_RATING_COUNTER);
    CqlIdentifier RATING_COLUMN_RATING_TOTAL_     = CqlIdentifier.fromCql(RATING_COLUMN_RATING_TOTAL);
    CqlIdentifier RATING_COLUMN_VIDEOID_          = CqlIdentifier.fromCql(RATING_COLUMN_VIDEOID);
    CqlIdentifier RATING_COLUMN_USERID_           = CqlIdentifier.fromCql(RATING_COLUMN_USERID);
    CqlIdentifier RATING_COLUMN_RATING_           = CqlIdentifier.fromCql(RATING_COLUMN_RATING);
    
    // video_playback_stats
    String TABLENAME_PLAYBACK_STATS  = "video_playback_stats";
    String COLUMN_PLAYBACK_VIDEOID   = "videoid";
    String COLUMN_PLAYBACK_VIEWS     = "views";
    CqlIdentifier TABLENAME_PLAYBACK_STATS_  = CqlIdentifier.fromCql(TABLENAME_PLAYBACK_STATS);
    CqlIdentifier COLUMN_PLAYBACK_VIDEOID_   = CqlIdentifier.fromCql(COLUMN_PLAYBACK_VIDEOID);
    CqlIdentifier COLUMN_PLAYBACK_VIEWS_     = CqlIdentifier.fromCql(COLUMN_PLAYBACK_VIEWS);
    
    
}
