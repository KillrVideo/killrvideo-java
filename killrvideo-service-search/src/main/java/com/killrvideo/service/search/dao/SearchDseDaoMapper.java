package com.killrvideo.service.search.dao;

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
public interface SearchDseDaoMapper {
    
    /**
     * Initialization of Dao {@link SearchDseDao}
     *
     * @param keyspace
     *      working keyspace name
     * @return
     *      instanciation with the mappers
     */
    @DaoFactory
    SearchDseDao searchDao(@DaoKeyspace CqlIdentifier keyspace);
    
}
