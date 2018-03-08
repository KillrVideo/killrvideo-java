package killrvideo.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

/**
 * Pojo representing DTO for table 'comments_by_video'
 *
 * @author DataStax evangelist team.
 */
@Table(keyspace = Schema.KEYSPACE, name = Schema.TABLENAME_ENCODING_JOBS_NOTIFICATION)
public class EncodingJobNotification implements Serializable {

    /** Serial. */
    private static final long serialVersionUID = -7458495256206978510L;

    @PartitionKey
    private UUID videoid;

    @ClusteringColumn(1)
    @Column(name = "status_date")
    private Date statusDate;

    @ClusteringColumn(2)
    private String etag;

    @Column
    private String jobid;

    @Column
    private String newstate;

    @Column
    private String oldstate;

    /**
     * Default constructor.
     */
    public EncodingJobNotification() {}

    /**
     * Constructor with all parameters.
     */
    public EncodingJobNotification(UUID videoid, Date statusDate, String etag, String jobid, String newstate, String oldstate) {
        this.videoid = videoid;
        this.statusDate = statusDate;
        this.etag = etag;
        this.jobid = jobid;
        this.newstate = newstate;
        this.oldstate = oldstate;
    }

    /**
     * Getter for attribute 'videoid'.
     *
     * @return
     *       current value of 'videoid'
     */
    public UUID getVideoid() {
        return videoid;
    }

    /**
     * Setter for attribute 'videoid'.
     * @param videoid
     * 		new value for 'videoid '
     */
    public void setVideoid(UUID videoid) {
        this.videoid = videoid;
    }

    /**
     * Getter for attribute 'statusDate'.
     *
     * @return
     *       current value of 'statusDate'
     */
    public Date getStatusDate() {
        return statusDate;
    }

    /**
     * Setter for attribute 'statusDate'.
     * @param statusDate
     * 		new value for 'statusDate '
     */
    public void setStatusDate(Date statusDate) {
        this.statusDate = statusDate;
    }

    /**
     * Getter for attribute 'etag'.
     *
     * @return
     *       current value of 'etag'
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Setter for attribute 'etag'.
     * @param etag
     * 		new value for 'etag '
     */
    public void setEtag(String etag) {
        this.etag = etag;
    }

    /**
     * Getter for attribute 'jobid'.
     *
     * @return
     *       current value of 'jobid'
     */
    public String getJobid() {
        return jobid;
    }

    /**
     * Setter for attribute 'jobid'.
     * @param jobid
     * 		new value for 'jobid '
     */
    public void setJobid(String jobid) {
        this.jobid = jobid;
    }

    /**
     * Getter for attribute 'newstate'.
     *
     * @return
     *       current value of 'newstate'
     */
    public String getNewstate() {
        return newstate;
    }

    /**
     * Setter for attribute 'newstate'.
     * @param newstate
     * 		new value for 'newstate '
     */
    public void setNewstate(String newstate) {
        this.newstate = newstate;
    }

    /**
     * Getter for attribute 'oldstate'.
     *
     * @return
     *       current value of 'oldstate'
     */
    public String getOldstate() {
        return oldstate;
    }

    /**
     * Setter for attribute 'oldstate'.
     * @param oldstate
     * 		new value for 'oldstate '
     */
    public void setOldstate(String oldstate) {
        this.oldstate = oldstate;
    }
    
}
