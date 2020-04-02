package com.killrvideo.service.rating.test;

import static com.datastax.oss.driver.api.querybuilder.SchemaBuilder.createTable;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.killrvideo.dse.dao.DseSchema;

/**
 * Integration Test for Comment Services.
 */
@SuppressWarnings("unused")
public class RatingDseDaoTest implements DseSchema {
    /*
    protected static CqlIdentifier       dseKeyspace      = CqlIdentifier.fromCql("killrvideo_test_rating");
    protected static int                 dseKeyspaceRF    = 1;
    protected static InetSocketAddress   dseContactPoint  = new InetSocketAddress("localhost", 9042);;
    protected static String              dseLocalDc       = "dc1";
    protected static DseSession          dseSession       = null;
    protected static GenericContainer<?> dseContainer     = null;
    
    protected static RatingDseDao ratingDseDao = null;
    
    @BeforeAll
    public static void setupDse() {
        dseSession = DseSession.builder()
                .addContactEndPoint(new DefaultEndPoint(dseContactPoint))
                .withLocalDatacenter(dseLocalDc).build(); 
        createKeySpaceSimpleStrategy(dseSession, dseKeyspace.asInternal(), dseKeyspaceRF);
        dseSession.execute(stmtCreateTableVideoRating(dseKeyspace));
        dseSession.execute(stmtCreateTableVideoRatingByUser(dseKeyspace));
        ratingDseDao = new RatingDseDaoMapperBuilder(dseSession).build().ratingDao(dseKeyspace);
    }
    
    @BeforeEach
    public void truncate() {
        truncateTable(dseSession, dseKeyspace, TABLENAME_VIDEO_RATINGS_);
        truncateTable(dseSession, dseKeyspace, TABLENAME_VIDEO_RATINGS_BYUSER_);
    }
    
    @AfterAll
    public static void cleanUp() {
        dropKeyspace(dseSession, dseKeyspace.asInternal());
        dseSession.close();
    }
    
    @Test
    @DisplayName("Rate a videos")
    public void should_create_record_when_table_empty() 
    throws InterruptedException, ExecutionException {
        // Given
        Assertions.assertTrue(isTableEmpty(dseSession, dseKeyspace, TABLENAME_VIDEO_RATINGS_));
        Assertions.assertTrue(isTableEmpty(dseSession, dseKeyspace, TABLENAME_VIDEO_RATINGS_BYUSER_));
        
        // When
        CompletionStage<Void> cs1 = ratingDseDao.rateVideo(
                UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ed"), 
                UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781c"), 5);
        CompletionStage<Void> cs2 = ratingDseDao.rateVideo(
                UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ee"), 
                UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781c"), 4);
        
        // Then, wait up to request completed
        cs1.toCompletableFuture().get();
        cs2.toCompletableFuture().get();
        Assertions.assertFalse(isTableEmpty(dseSession, dseKeyspace, TABLENAME_VIDEO_RATINGS_));
        Assertions.assertFalse(isTableEmpty(dseSession, dseKeyspace, TABLENAME_VIDEO_RATINGS_BYUSER_));
    }
    
    @Test
    @DisplayName("Find rating with invalid videoid is empty")
    public void should_return_optionalEmpty_if_invalid_id() 
    throws InterruptedException, ExecutionException {
        Assertions.assertTrue(ratingDseDao.findRating(UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ef"))
                                          .toCompletableFuture().get()
                                          .isEmpty());
    }
    
    @Test
    @DisplayName("Find rating for user with invalid videoid/userid is empty")
    public void should_return_optionalEmpty_if_invalid_couple() 
    throws InterruptedException, ExecutionException {
        Assertions.assertTrue(ratingDseDao.findUserRating(
                        UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ee"),
                        UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781e"))
                                          .toCompletableFuture().get()
                                          .isEmpty());
    }
    
    @Test
    @DisplayName("Find rating based on ids")
    public void should_findRating_of_exists() 
    throws InterruptedException, ExecutionException {
        // Given
        UUID videoid = UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ed");
        UUID userid  = UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781c");
        ratingDseDao.rateVideo(videoid, userid, 5).toCompletableFuture().get();
        // When
        Optional<VideoRatings>       ratings      = ratingDseDao.findRating(videoid).toCompletableFuture().get();
        Optional<VideoRatingsByUser> ratingByUser = ratingDseDao.findUserRating(videoid, userid).toCompletableFuture().get();
        // Then
        Assertions.assertFalse(ratings.isEmpty());
        Assertions.assertFalse(ratingByUser.isEmpty());
        Assertions.assertEquals(5, ratings.get().getRatingCounter().intValue());
        Assertions.assertEquals(5, ratingByUser.get().getRating());
    }
    
    */
    
    public static SimpleStatement stmtCreateTableVideoRating(CqlIdentifier kspace) {
        return createTable(kspace, TABLENAME_VIDEO_RATINGS_).ifNotExists()
                .withPartitionKey(RATING_COLUMN_VIDEOID_, DataTypes.UUID)
                .withColumn(RATING_COLUMN_RATING_COUNTER_, DataTypes.COUNTER)
                .withColumn(RATING_COLUMN_RATING_TOTAL_, DataTypes.COUNTER)
                .build();
    }
    
    public static SimpleStatement stmtCreateTableVideoRatingByUser(CqlIdentifier kspace) {
        return createTable(kspace, TABLENAME_VIDEO_RATINGS_BYUSER_).ifNotExists()
                .withPartitionKey(RATING_COLUMN_VIDEOID_, DataTypes.UUID)
                .withClusteringColumn(RATING_COLUMN_USERID_, DataTypes.UUID)
                .withColumn(RATING_COLUMN_RATING_, DataTypes.INT)
                .withClusteringOrder(RATING_COLUMN_USERID_, ClusteringOrder.ASC)
                .build();
    }
}
