package com.killrvideo.dse.graph;

import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.UUID;

/**
 * Traversal Source for our KillrVideo graph DSL (Domain Specific Language)
 *
 * Initial DSL credit goes to Stephen Mallette who provided me with a working
 * KillrVideo DSL example that I used as the basis for the following code.
 * Be sure to check out his excellent blog post explaining DSL's
 * here -> https://www.datastax.com/dev/blog/gremlin-dsls-in-java-with-dse-graph
 */
public class KillrVideoTraversalSourceDsl extends GraphTraversalSource implements KillrVideoTraversalConstants {

    public KillrVideoTraversalSourceDsl(final Graph graph, final TraversalStrategies traversalStrategies) {
        super(graph, traversalStrategies);
    }

    public KillrVideoTraversalSourceDsl(final Graph graph) {
        super(graph);
    }
    
    public KillrVideoTraversalSourceDsl(final RemoteConnection connection) {
        super(connection);
    }

    /**
     * Applied filtering on VIDEO/VIDEOID vertices.
     */
    public GraphTraversal<Vertex, Vertex> videos(String... videoIds) {
    		return filteredTransversal(VERTEX_VIDEO, KEY_VIDEO_ID, videoIds);
    }
    
    /**
     * Applied filtering on USER/USERID vertices.
     */
    public GraphTraversal<Vertex, Vertex> users(String... userIds) {
    		return filteredTransversal(VERTEX_USER, KEY_USER_ID, userIds);
    }
    
    /**
     * Applied filtering on TAG vertices.
     */
    public GraphTraversal<Vertex, Vertex> tags(String... tags) {
    		return filteredTransversal(VERTEX_TAG, KEY_NAME, tags);
    }
    
    /**
     * Provides a traversal that filters on one or more nodes based on vertexNames.
     *
     * @param vertexName
     * 		alias for vertex
     * @param vertexId
     * 		identifiers
     * @param ids
     * 		identifiers to process
     * @return
     * 		expected traversal.
     */
    public GraphTraversal<Vertex, Vertex> filteredTransversal(String vertexName, String vertexId, String... ids) {
        GraphTraversal<Vertex, Vertex> traversal = this.clone().V();
        traversal = traversal.hasLabel(vertexName);
        if (null != ids) {
	        if (ids.length == 1) {
	            traversal = traversal.has(vertexId, ids);
	        } else if (ids.length > 1) {
	            traversal = traversal.has(vertexId, P.within(ids));
	        }
        }
        return traversal;
    }

    /**
     * Creates a video vertex if one does not exist and allows for updating
     * video vertex properties.
     */
    @SuppressWarnings("unchecked")
	public GraphTraversal<Vertex, Vertex> video(UUID videoId, String name, Date added_date, String description, String previewImageLocation) {
        Assert.notNull(videoId, "The videoId must not be null");
        Assert.notNull(added_date, "The added_date must not be null");
        Assert.hasLength(name, "The name must not be null or empty");
        Assert.hasLength(description, "The description must not be null or empty");
        Assert.hasLength(previewImageLocation, "The previewImageLocation must not be null or empty");
        
        GraphTraversal<Vertex, Vertex> traversal = this.clone().V();
        return traversal
                .has(VERTEX_VIDEO, KEY_VIDEO_ID, videoId)
                .fold()
                .coalesce(
                        __.unfold(),
                        __.addV(VERTEX_VIDEO).property(KEY_VIDEO_ID, videoId)
                )
                .property(KEY_ADDED_DATE, added_date)
                .property(KEY_NAME, name)
                .property(KEY_DESCRIPTION, description)
                .property(KEY_PREVIEW_IMAGE_LOCATION, previewImageLocation);
    }

    /**
     * Creates a user vertex if one does not exist and does not allow for updating
     * user vertex properties.  This was done to match current application design that
     * does not allow for altering user properties once created.
     */
    //:TODO Possibly update added_date to use epoch long per Seb's comment
    @SuppressWarnings("unchecked")
    public GraphTraversal<Vertex, Vertex> user(UUID userId, String email, Date added_date) {
        Assert.notNull(userId, "The userId must not be null");
        Assert.notNull(added_date, "The added_date must not be null");
        Assert.hasLength(email, "The email must not be null or empty");
        
        GraphTraversal<Vertex, Vertex> traversal = this.clone().V();
        return traversal
                .has(VERTEX_USER, KEY_USER_ID, userId)
                .fold()
                .coalesce(
                        __.unfold(),
                        __.addV(VERTEX_USER)
                                .property(KEY_USER_ID, userId)
                                .property(KEY_ADDED_DATE, added_date)
                                .property(KEY_EMAIL, email)
                );
    }

    /**
     * Creates a tag vertex if one does not exist and does not allow for updating
     * tag vertex properties.
     */
    @SuppressWarnings("unchecked")
    public GraphTraversal<Vertex, Vertex> tag(String name, Date tagged_date) {
        Assert.notNull(tagged_date, "The tagged_date must not be null");
        Assert.hasLength(name, "The name must not be null or empty");
        return this.clone().V()
                .has(VERTEX_TAG, KEY_NAME, name)
                .fold()
                .coalesce(
                        __.unfold(),
                        __.addV(VERTEX_TAG)
                                .property(KEY_NAME, name)
                                .property(KEY_TAGGED_DATE, tagged_date)
                );
    }

}
