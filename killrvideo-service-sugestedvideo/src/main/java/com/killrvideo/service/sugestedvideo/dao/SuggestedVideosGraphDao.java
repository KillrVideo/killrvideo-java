package com.killrvideo.service.sugestedvideo.dao;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import com.datastax.dse.driver.api.core.graph.FluentGraphStatement;
import com.datastax.dse.driver.api.core.graph.GraphNode;
import com.datastax.dse.driver.api.core.graph.GraphStatement;
import com.datastax.oss.driver.api.core.CqlSession;
import com.killrvideo.dse.dto.Video;
import com.killrvideo.dse.graph.KillrVideoTraversal;
import com.killrvideo.dse.graph.KillrVideoTraversalConstants;
import com.killrvideo.dse.graph.KillrVideoTraversalSource;
import com.killrvideo.dse.graph.__;

/**
 * Implementations of operation for Videos.
 *
 * @author DataStax Developer Advocates team.
 */
@Repository
public class SuggestedVideosGraphDao implements KillrVideoTraversalConstants {
    
    /** Logger for the class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SuggestedVideosGraphDao.class);
   
    @Autowired
    private KillrVideoTraversalSource traversalSource;
    
    @Autowired
    private CqlSession cqlSession;
    
    /**
     * Search for videos.
     *
     * @param userid
     *      current userid,
     * @return
     *         Async Page
     */
    public CompletionStage< List<Video> > getSuggestedVideosForUser(UUID userid) {
        Assert.notNull(userid, "videoid is required to update statistics");
        KillrVideoTraversal<?,?> graphTraversal = 
                traversalSource.users(userid.toString()).recommendByUserRating(5, 4, 1000, 5);
        return cqlSession
               .executeAsync(FluentGraphStatement.newInstance(graphTraversal))
               .thenApply(agrs -> agrs.currentPage())
               .thenApply(iter -> StreamSupport.stream(iter.spliterator(), false))
               .thenApply(stream -> stream.map(this::mapGraphNode2Video).collect(Collectors.toList()));
    }
    
    /**
     * Subscription is done in dedicated service 
     * {@link EventConsumerService}. (killrvideo-messaging)
     * 
     * Below we are using our KillrVideoTraversal DSL (Domain Specific Language)
     * to create our video vertex, then within add() we connect up the user responsible
     * for uploading the video with the "uploaded" edge, and then follow up with
     * any and all tags using the "taggedWith" edge.  Since we may have multiple
     * tags make sure to loop through and get them all in there.
     *
     * Also note the use of add().  Take a look at Stephen's blog here
     * -> https://www.datastax.com/dev/blog/gremlin-dsls-in-java-with-dse-graph for more information.
     * This essentially allows us to chain multiple commands (uploaded and (n * taggedWith) in this case)
     * while "preserving" our initial video traversal position. Since the video vertex passes
     * through each step we do not need to worry about traversing back to video for each step
     * in the chain.
     * 
     * May be relevant to have a full sample traversal:
     * g.V().has("video","videoId", 6741b34e-03c7-4d83-bf55-deed496d6e03)
     *  .fold()
     *  .coalesce(__.unfold(),
     *   __.addV("video")
     *     .property("videoId",6741b34e-03c7-4d83-bf55-deed496d6e03))
     *     .property("added_date",Thu Aug 09 11:00:44 CEST 2018)
     *     .property("name","Paris JHipster Meetup #9")
     *     .property("description","xxxxxx")
     *     .property("preview_image_location","//img.youtube.com/vi/hOTjLOPXg48/hqdefault.jpg")
     *     // Add Edge
     *     .sideEffect(
     *        __.as("^video").coalesce(
     *           __.in("uploaded")
     *             .hasLabel("user")
     *             .has("userId",8a70e329-59f8-4e2e-aae8-1788c94e8410),
     *           __.V()
     *             .has("user","userId",8a70e329-59f8-4e2e-aae8-1788c94e8410)
     *             .addE("uploaded")
     *             .to("^video").inV())
     *      )
     *      // Tag with X (multiple times)
     *      .sideEffect(
     *        __.as("^video").coalesce(
     *          __.out("taggedWith")
     *            .hasLabel("tag")
     *            .has("name","X"),
     *          __.coalesce(
     *            __.V().has("tag","name","X"),
     *            __.addV("tag")
     *              .property("name","X")
     *               .property("tagged_date",Thu Aug 09 11:00:44 CEST 2018)
     *              ).addE("taggedWith").from("^video").inV())
     *       )
     *       // Tag with FF4j
     *       .sideEffect(
     *         __.as("^video").coalesce(
     *           __.out("taggedWith")
     *             .hasLabel("tag")
     *             .has("name","ff4j"),
     *           __.coalesce(
     *            __.V().has("tag","name","ff4j"),
     *            __.addV("tag")
     *              .property("name","ff4j")
     *              .property("tagged_date",Thu Aug 09 11:00:44 CEST 2018))
     *              .addE("taggedWith").from("^video").inV()))
     */
    public void updateGraphNewVideo(Video video) {
        final KillrVideoTraversal<?,?> traversal =
          // Add video Node
          traversalSource
              .video(video.getVideoid(), video.getName(), 
                     new Date(), video.getDescription(), video.getPreviewImageLocation())
              .add(__.uploaded(video.getUserid()));  // Add Uploaded Edge
          // Add Tags Nodes and edges
          video.getTags().stream()
             .forEach(tag -> traversal.add(__.taggedWith(tag,  new Date())));

        /** 
         *  Now that our video is successfully applied lets
         *  insert that video into our graph for the recommendation engine 
         */
         cqlSession.executeAsync(FluentGraphStatement.newInstance(traversal))
                  .toCompletableFuture()
                  .whenComplete((graphResultSet, ex) -> {
            if (graphResultSet != null) {
                LOGGER.debug("Added video vertex, uploaded, and taggedWith edges: " + graphResultSet.currentPage());
            }  else {
                LOGGER.warn("Error handling YouTubeVideoAdded for graph: " + ex);
            }
        });
    }
    
    /**
     * Subscription is done in dedicated service 
     * {@link EventConsumerService}. (killrvideo-messaging)
     * 
     * This will create a user vertex in our graph if it does not already exist.
     * 
     * @param user
     *      current user
     */
    public void updateGraphNewUser(UUID userId, String email, Date userCreation) {
        KillrVideoTraversal<?, ?> graphTraversal = traversalSource.user(userId, email, userCreation);
        GraphStatement<?> graphStatement = FluentGraphStatement.newInstance(graphTraversal);
        cqlSession.executeAsync(graphStatement)
                  .toCompletableFuture()
                  .whenComplete((graphResultSet, ex) -> {
            if (graphResultSet != null) {
                LOGGER.debug("Added user vertex: " + graphResultSet.one());
            } else {
                LOGGER.warn("Error creating user vertex: " + ex);
            }
        });
    }
    
    /**
     * Subscription is done in dedicated service 
     * {@link EventConsumerService}. (killrvideo-messaging)
     * 
     * Note that if either the user or video does not exist in the graph
     * the rating will not be applied nor will the user or video be
     * automatically created in this case.  This assumes both the user and video
     * already exist.
     */
    public void updateGraphNewUserRating(String videoId, UUID userId, int rate) {
        final KillrVideoTraversal<?,?> graphTraversal = traversalSource.videos(videoId).add(__.rated(userId, rate));
        GraphStatement<?> graphStatement = FluentGraphStatement.newInstance(graphTraversal);
        cqlSession.executeAsync(graphStatement)
                  .toCompletableFuture()
                  .whenComplete((graphResultSet, ex) -> {
            if (graphResultSet != null) {
                LOGGER.debug("Added rating between user and video: " + graphResultSet.one());
            } else {
                LOGGER.warn("Error Adding rating between user and video: " + ex);
            }
        });
    }
    
    /**
     * Parse graph result to bean.
     *
     * @param node
     *      graphNode 
     * @return
     *      video bean
     */
    private Video mapGraphNode2Video(GraphNode node) {
        Vertex v = node.getByKey(VERTEX_VIDEO).asVertex();
        VertexProperty<Instant> pAddedDate = v.property("added_date");
        VertexProperty<String>  pName      = v.property("name");
        VertexProperty<String>  pImgLoca   = v.property("preview_image_location");
        VertexProperty<UUID>    pVideoId   = v.property("videoId");
        Vertex u = node.getByKey(VERTEX_USER).asVertex();
        VertexProperty<UUID>    pUserId    = u.property("userid");
        Video video = new Video();
        video.setAddedDate(pAddedDate.value());
        video.setName(pName.value());
        video.setPreviewImageLocation(pImgLoca.value());
        video.setVideoid(pVideoId.value());
        video.setUserid(pUserId.value());
        return video;
    }
}
