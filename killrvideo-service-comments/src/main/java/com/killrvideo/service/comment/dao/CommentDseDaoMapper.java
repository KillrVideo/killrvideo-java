package com.killrvideo.service.comment.dao;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.DaoKeyspace;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;

/**
 * Annotated as {@link Mapper} will generate working {@link Dao}.
 * 
 * @author DataStax Developer Advocates team.
 */
@Mapper
public interface CommentDseDaoMapper {

    /**
     * Initialization of Dao {@link CommentDseDao}
     *
     * @param keyspace
     *      working keyspace name
     * @return
     *      instanciation with the mappers
     */
    @DaoFactory
    CommentDseDao commentDao(@DaoKeyspace CqlIdentifier keyspace);
    
}
