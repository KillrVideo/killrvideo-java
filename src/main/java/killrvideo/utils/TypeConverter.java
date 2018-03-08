package killrvideo.utils;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.google.protobuf.Timestamp;

import killrvideo.common.CommonTypes.TimeUuid;
import killrvideo.common.CommonTypes.Uuid;
import killrvideo.graph.KillrVideoTraversal;

public class TypeConverter {

    public static Timestamp instantToTimeStamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano()).build();
    }
    
    public static Timestamp epochTimeToTimeStamp(long epoch) {
        return Timestamp.newBuilder().setSeconds(epoch).build();
    }

    public static Timestamp dateToTimestamp(Date date) {
        return instantToTimeStamp(date.toInstant());
    }

    public static Date dateFromTimestamp(Timestamp timestamp) {
        return Date.from(Instant.ofEpochSecond(timestamp.getSeconds()));
    }

    public static TimeUuid uuidToTimeUuid(UUID uuid) {
        return TimeUuid.newBuilder()
                .setValue(uuid.toString())
                .build();
    }

    public static Uuid uuidToUuid(UUID uuid) {
        return Uuid.newBuilder()
                .setValue(uuid.toString())
                .build();
    }

    /**
     * This method is useful when debugging as an easy way to get a traversal
     * string to use in gremlin or DSE Studio from bytecode.
     * @param traversal
     * @return
     */
    public static String bytecodeToTraversalString(KillrVideoTraversal<Vertex, Vertex> traversal) {
        return org.apache.tinkerpop.gremlin.groovy.jsr223.GroovyTranslator.of("g").translate(traversal.getBytecode());
    }
}
