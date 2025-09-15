package com.killrvideo.service.sugestedvideo.messaging;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.google.common.eventbus.Subscribe;

import killrvideo.ratings.events.RatingsEvents.UserRatedVideo;
import killrvideo.user_management.events.UserManagementEvents.UserCreated;
import killrvideo.video_catalog.events.VideoCatalogEvents.YouTubeVideoAdded;

/**
 * With Guava subscription is done with annotation Subscribe. Event are sent in the 
 * bus and correct method is invoked based on the event type (classname). 
 * 
 * @author Cedrick LUNVEN (@clunven)
 */
@Repository("killrvideo.rating.dao.messaging")
@Profile("messaging_memory")
public class SuggestedVideosMessagingMemoryDao extends SuggestedVideosMessagingDaoSupport {
    
    /** {@inheritDoc} */
    @Subscribe
    public void onYoutubeVideoAddingMessage(YouTubeVideoAdded youTubeVideoAdded) {
        super.onYoutubeVideoAddingMessage(youTubeVideoAdded);
    }
    
    /** {@inheritDoc} */
    @Subscribe
    public void onUserCreatingMessage(UserCreated userCreated) {
        super.onUserCreatingMessage(userCreated);
    }
    
    /** {@inheritDoc} */
    @Subscribe
    public void onVideoRatingMessage(UserRatedVideo userRatedVideo) {
        super.onVideoRatingMessage(userRatedVideo);
    }
    
}
