package com.killrvideo.dse.utils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.datastax.oss.driver.api.mapper.entity.EntityHelper;
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;

/**
 * Utility class for DSE.
 *
 * @author DataStax Developer Advocates Team
 */
public class DseUtils {
    
    /** Internal logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DseUtils.class);
    private static final String UTF8_ENCODING         = "UTF-8";
    private static final String NEW_LINE              = System.getProperty("line.separator");
    private static final long   INT_SINCE_UUID_EPOCH  = 0x01b21dd213814000L;
    
    public static long getTimeFromUUID(UUID uuid) {
      return (uuid.timestamp() - INT_SINCE_UUID_EPOCH) / 10000;
    }
    
    /**
     * Helper to create a KeySpace.
     *
     * @param keyspacename
     *      target keyspaceName
     */
    public static void createKeySpaceSimpleStrategy(CqlSession cqlSession, String keyspacename, int replicationFactor) {
        cqlSession.execute(SchemaBuilder.createKeyspace(keyspacename)
                  .ifNotExists()
                  .withSimpleStrategy(replicationFactor)
                  .build());
        useKeySpace(cqlSession, keyspacename);
    }
    
    public static boolean isTableEmpty(CqlSession cqlSession, CqlIdentifier keyspace, CqlIdentifier tablename) {
        return 0 == cqlSession.execute(QueryBuilder.selectFrom(keyspace, tablename).all().build()).getAvailableWithoutFetching();
    }
    
    public static void useKeySpace(CqlSession cqlSession, String keyspacename) {
        cqlSession.execute("USE " + keyspacename);
    }
    
    public static void dropKeyspace(CqlSession cqlSession, String keyspacename) {
        cqlSession.executeAsync(SchemaBuilder.dropKeyspace(keyspacename).ifExists().build());
    }
    
    public static void truncateTable(CqlSession cqlSession, CqlIdentifier keyspace, CqlIdentifier tableName) {
        cqlSession.execute(QueryBuilder.truncate(keyspace, tableName).build());
    }
    
    /**
     * Allows to execute a CQL File.
     *
     * @param dseSession
     *      current dse Session
     * @param fileName
     *      cql file name to execute
     * @throws FileNotFoundException
     *      cql file has not been found.
     */
    public static void executeCQLFile(CqlSession cqlSession, String fileName)
    throws FileNotFoundException {
        long top = System.currentTimeMillis();
        LOGGER.info("Processing file: " + fileName);
        Arrays.stream(loadFileAsString(fileName).split(";")).forEach(statement -> {
            String query = statement.replaceAll(NEW_LINE, "").trim();
            try {
                if (query.length() > 0) {
                    cqlSession.execute(query);
                    LOGGER.info(" + Executed. " + query);
                }
            } catch (InvalidQueryException e) {
                LOGGER.warn(" + Query Ignore. " + query, e);
            }
        });
        LOGGER.info("Execution done in {} millis.", System.currentTimeMillis() - top);
    }
    
    /**
     * Utils method to load a file as String.
     *
     * @param fileName
     *            target file Name.
     * @return target file content as String
     * @throws FileNotFoundException 
     */
    private static String loadFileAsString(String fileName)
    throws FileNotFoundException {
        InputStream in = DseUtils.class.getResourceAsStream(fileName);
        if (in == null) {
            // Fetch absolute classloader path
            in =  DseUtils.class.getClassLoader().getResourceAsStream(fileName);
        }
        if (in == null) {
            // Thread
            in =  Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        }
        if (in == null) {
            throw new FileNotFoundException("Cannot load file " + fileName + " please check");
        }
        Scanner currentScan = null;
        StringBuilder strBuilder = new StringBuilder();
        try {
            currentScan = new Scanner(in, UTF8_ENCODING);
            while (currentScan.hasNextLine()) {
                strBuilder.append(currentScan.nextLine());
                strBuilder.append(NEW_LINE);
            }
        } finally {
            if (currentScan != null) {
                currentScan.close();
            }
        }
        return strBuilder.toString();
    }
    
    public static <T> BoundStatement bind(PreparedStatement preparedStatement, T entity, EntityHelper<T> entityHelper) {
        BoundStatementBuilder boundStatement = preparedStatement.boundStatementBuilder();
        entityHelper.set(entity, boundStatement, NullSavingStrategy.DO_NOT_SET);
        return boundStatement.build();
    }
}
