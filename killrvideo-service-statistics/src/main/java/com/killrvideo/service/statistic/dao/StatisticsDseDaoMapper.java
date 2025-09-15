package com.killrvideo.service.statistic.dao;

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
public interface StatisticsDseDaoMapper {

    /**
     * Initialization of Dao {@link StatisticsDseDao}
     *
     * @param keyspace
     *      working keyspace name
     * @return
     *      instanciation with the mappers
     */
    @DaoFactory
    StatisticsDseDao statisticsDao(@DaoKeyspace CqlIdentifier keyspace);
    
}
