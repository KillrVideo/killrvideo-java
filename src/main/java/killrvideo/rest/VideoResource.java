package killrvideo.rest;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import killrvideo.dao.bean.CommentListResult;
import killrvideo.dao.bean.QueryCommentByVideo;
import killrvideo.dao.dse.CommentDseDao;

/**
 * Proposal to work on Video (API).
 *
 * @author DataStax evangelist team.
 */
@RestController
@RequestMapping("/api/v1/video")
public class VideoResource {
    
    /** Loger for that class. */
    private static Logger LOGGER = LoggerFactory.getLogger(VideoResource.class);
    
    @Autowired
    private CommentDseDao dseDao;
    
    /*
    @RequestMapping(method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String findVideoById(@PathVariable(value = "{videoid}") String videoId) {
        return "OK";
    }*/
    
    @RequestMapping(method=RequestMethod.GET, value="/{videoid}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommentListResult getVideoComments(@PathVariable String videoid) {
        LOGGER.debug("Listing of video comments");
        QueryCommentByVideo q = new QueryCommentByVideo();
        q.setVideoId(UUID.fromString(videoid));
        return dseDao.findCommentsByVideoId(q);
    }

}
