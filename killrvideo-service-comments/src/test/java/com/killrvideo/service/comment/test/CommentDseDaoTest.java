package com.killrvideo.service.comment.test;

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
public class CommentDseDaoTest implements DseSchema {
    /*
    protected static CqlIdentifier       dseKeyspace      = CqlIdentifier.fromCql("killrvideo_test_comments");
    protected static int                 dseKeyspaceRF    = 1;
    protected static InetSocketAddress   dseContactPoint  = new InetSocketAddress("localhost", 9042);;
    protected static String              dseLocalDc       = "dc1";
    protected static DseSession          dseSession       = null;
    protected static GenericContainer<?> dseContainer     = null;
    
    protected static CommentDseDao commentDseDao = null;
    
    @SuppressWarnings("resource")
    protected static void startDseContainer() {
        dseContainer = new GenericContainer<>("datastax/dse-server:6.7.2")
                .withExposedPorts(9042)
                .withCommand("-s -g") 
                .withEnv("DS_LICENSE", "accept")
                .withEnv("DC", "dc1");
        dseContainer.start();
        
        dseContactPoint = new InetSocketAddress(
                dseContainer.getContainerIpAddress(), 
                dseContainer.getMappedPort(9042));
    }
    
    protected static void stopDseContainer() {
      if (dseContainer != null) {  
          dseContainer.stop();
      }
    }
    
    @BeforeAll
    public static void setupDse() {
        // startDseContainer();
        dseSession = DseSession.builder()
                .addContactEndPoint(new DefaultEndPoint(dseContactPoint))
                .withLocalDatacenter(dseLocalDc).build(); 
        createKeySpaceSimpleStrategy(dseSession, dseKeyspace.asInternal(), dseKeyspaceRF);
        dseSession.execute(stmtCreateTableCommentByUser(dseKeyspace));
        dseSession.execute(stmtCreateTableCommentByVideo(dseKeyspace));
        
        commentDseDao = new CommentDseDaoMapperBuilder(dseSession).build().commentDao(dseKeyspace);
    }
    
    @BeforeEach
    public void truncate() {
        truncateTable(dseSession, dseKeyspace, TABLENAME_COMMENTS_BY_USER_);
        truncateTable(dseSession, dseKeyspace, TABLENAME_COMMENTS_BY_VIDEO_);
    }
    
    @AfterAll
    public static void cleanUp() {
        //dropKeyspace(dseSession, dseKeyspace.asInternal());
        dseSession.close();
        stopDseContainer();
    }
    
    @Test
    @DisplayName("Upsert a new comment")
    public void shoud_create_record_when_table_empty() {
        
        // Given
        Assertions.assertTrue(isTableEmpty(dseSession, dseKeyspace, TABLENAME_COMMENTS_BY_USER_));
        Assertions.assertTrue(isTableEmpty(dseSession, dseKeyspace, TABLENAME_COMMENTS_BY_VIDEO_));
        
        // When
        Comment c1= new Comment();
        c1.setComment("Killrvideo Rocks!");
        c1.setUserid(UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781c"));
        c1.setVideoid(UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ed"));
        c1.setCommentid(UUID.fromString("79ff95e0-befc-11e9-9051-714df5616ec8"));
        commentDseDao.upsert(c1);
        
        // Then
        Assertions.assertFalse(isTableEmpty(dseSession, dseKeyspace, TABLENAME_COMMENTS_BY_USER_));
        Assertions.assertFalse(isTableEmpty(dseSession, dseKeyspace, TABLENAME_COMMENTS_BY_VIDEO_));
    }
    
    @Test
    @DisplayName("Upsert an existing comment")
    public void shoud_update_record_when_record_exist() {
        // Given
        Comment c1= new Comment();
        c1.setComment("Killrvideo Rocks!");
        c1.setUserid(UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781c"));
        c1.setVideoid(UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ed"));
        c1.setCommentid(UUID.fromString("79ff95e0-befc-11e9-9051-714df5616ec8"));
        commentDseDao.upsert(c1);
        c1.setComment("updated Comment");
        commentDseDao.upsert(c1);
        
        // When
        QueryCommentByUser queryUser = new QueryCommentByUser(UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781c"));
        ResultListPage<CommentByUserEntity> res = commentDseDao.findCommentsByUserId(queryUser);
        
        // Then
        Assertions.assertEquals(1, res.getResults().size());
        Assertions.assertEquals("updated Comment", res.getResults().get(0).getComment());
    }
    
    @Test
    @DisplayName("Delete an existing comment")
    public void shoud_delete_record_when_record_exist() {
        // Given
        Comment c1= new Comment();
        c1.setComment("Killrvideo Rocks!");
        c1.setUserid(UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781c"));
        c1.setVideoid(UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ed"));
        c1.setCommentid(UUID.fromString("79ff95e0-befc-11e9-9051-714df5616ec8"));
        commentDseDao.upsert(c1);
        
        // When
        commentDseDao.delete(c1);
        
        // Then
        Assertions.assertTrue(isTableEmpty(dseSession, dseKeyspace, TABLENAME_COMMENTS_BY_USER_));
        Assertions.assertTrue(isTableEmpty(dseSession, dseKeyspace, TABLENAME_COMMENTS_BY_VIDEO_));
    }
    
    @Test
    @DisplayName("Search by user id")
    public void should_find_record_when_searchByUser() {
        // Given
        Comment c1= new Comment();
        c1.setComment("Killrvideo Rocks!");
        c1.setUserid(UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781c"));
        c1.setVideoid(UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ed"));
        c1.setCommentid(UUID.fromString("79ff95e0-befc-11e9-9051-714df5616ec8"));
        commentDseDao.upsert(c1);
        // When
        QueryCommentByUser queryUser = new QueryCommentByUser(UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781c"));
        ResultListPage<CommentByUserEntity> res = commentDseDao.findCommentsByUserId(queryUser);
        // Then
        Assertions.assertEquals(1, res.getResults().size());
        Assertions.assertEquals(UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781c"), res.getResults().get(0).getUserid());
    }
    
    @Test
    @DisplayName("Search by video id")
    public void should_find_record_when_searchByVideo() {
        // Given
        Comment c1= new Comment();
        c1.setComment("Killrvideo Rocks!");
        c1.setUserid(UUID.fromString("2d32af9c-7889-4256-aedb-b458976f781c"));
        c1.setVideoid(UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ed"));
        c1.setCommentid(UUID.fromString("79ff95e0-befc-11e9-9051-714df5616ec8"));
        commentDseDao.upsert(c1);
        // When
        QueryCommentByVideo queryVideo = new QueryCommentByVideo(UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ed"));
        ResultListPage<CommentByVideoEntity> res = commentDseDao.findCommentsByVideoId(queryVideo);
        // Then
        Assertions.assertEquals(1, res.getResults().size());
        Assertions.assertEquals(UUID.fromString("6fd4df0a-74ca-4891-ae3b-0c16e37880ed"), res.getResults().get(0).getVideoid());
    }*/
    
    public static SimpleStatement stmtCreateTableCommentByUser(CqlIdentifier kspace) {
        return createTable(kspace, TABLENAME_COMMENTS_BY_USER_).ifNotExists()
                .withPartitionKey(COMMENTS_COLUMN_USERID, DataTypes.UUID)
                .withClusteringColumn(COMMENTS_COLUMN_COMMENTID, DataTypes.TIMEUUID)
                .withColumn(COMMENTS_COLUMN_COMMENT, DataTypes.TEXT)
                .withColumn(COMMENTS_COLUMN_VIDEOID, DataTypes.UUID)
                .withClusteringOrder(COMMENTS_COLUMN_COMMENTID, ClusteringOrder.DESC)
                .withComment("List comments on user page")
                .build();
    }
    
    public static SimpleStatement stmtCreateTableCommentByVideo(CqlIdentifier kspace) {
        return createTable(kspace, TABLENAME_COMMENTS_BY_VIDEO_).ifNotExists()
                .withPartitionKey(COMMENTS_COLUMN_VIDEOID, DataTypes.UUID)
                .withClusteringColumn(COMMENTS_COLUMN_COMMENTID, DataTypes.TIMEUUID)
                .withColumn(COMMENTS_COLUMN_COMMENT, DataTypes.TEXT)
                .withColumn(COMMENTS_COLUMN_USERID, DataTypes.UUID)
                .withClusteringOrder(COMMENTS_COLUMN_COMMENTID, ClusteringOrder.DESC)
                .withComment("List comments on user page")
                .build();
    }
}
