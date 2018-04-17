package killrvideo.configuration;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;

import killrvideo.entity.LatestVideos;
import killrvideo.entity.User;
import killrvideo.entity.UserCredentials;
import killrvideo.entity.UserVideos;
import killrvideo.entity.Video;
import killrvideo.entity.VideoPlaybackStats;
import killrvideo.entity.VideoRating;
import killrvideo.entity.VideoRatingByUser;

/**
 * Setting up mapping between entities and Cassandra UDT.
 * 
 * @author DataStax evangelist team.
 */
@Configuration
public class DseMapperConfiguration {

    @Inject
    private MappingManager manager;

    @Bean
    public Mapper<User> userMapper() {
        return manager.mapper(User.class);
    }

    @Bean
    public Mapper< LatestVideos > latestVideosMapper() { 
        return manager.mapper(LatestVideos.class); 
    }

    @Bean
    public Mapper< UserCredentials > userCredentialsMapper() {
        return manager.mapper(UserCredentials.class);
    }

    @Bean
    public Mapper< UserVideos > userVideosMapper() { 
        return manager.mapper(UserVideos.class);
    }

    @Bean
    public Mapper< Video > videoMapper() { 
        return manager.mapper(Video.class); 
    }

    @Bean
    public Mapper< VideoPlaybackStats > videoPlaybackStatsMapper() { 
        return manager.mapper(VideoPlaybackStats.class); 
    }

    @Bean
    public Mapper < VideoRating > videoRatingMapper() { 
        return manager.mapper(VideoRating.class); 
    }

    @Bean
    public Mapper< VideoRatingByUser > videoRatingByUserMapper() { 
        return manager.mapper(VideoRatingByUser.class); 
    }
}
