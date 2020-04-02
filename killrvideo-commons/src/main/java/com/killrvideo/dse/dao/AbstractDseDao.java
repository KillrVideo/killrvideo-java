package com.killrvideo.dse.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfig;

/**
 * Mutualization of code for DAO.
 *
 * @author DataStax Developer Advocates team.
 */
public abstract class AbstractDseDao implements DseSchema {
   
    @Autowired
    protected CqlSession cqlSession;
   
    @Autowired
    @Qualifier("killrvideo.keyspace")
    protected CqlIdentifier keyspaceName;
    
    protected DriverConfig dseDriverConfig;
    
    /**
     * Default constructor.
     */
    public AbstractDseDao() {}
    
    /**
     * Allow explicit intialization for test purpose.
     */
    public AbstractDseDao(CqlSession dseSession) {
        this.cqlSession      = dseSession;
        this.dseDriverConfig = dseSession.getContext().getConfig();
        if (!dseSession.getKeyspace().isEmpty()) {
            keyspaceName = dseSession.getKeyspace().get();
        }
    }
    
    /**
     * Utility for validations.
     */
    protected void assertNotNull(String mName, String pName, Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Assertion failed: param " + pName + " is required for method " + mName);
        }
    }

}
