package com.killrvideo.service.user.dao;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.killrvideo.service.user.dto.User;
import com.killrvideo.service.user.dto.UserCredentials;

/**
 * Implementation of User Operation in {@link UserDseDao} for bootcamp:
 * 
 * KISS:
 * - Using SimpleStatement Only
 * - Avoiding PrepareStatement
 * - Avoiding QueryBuilder to ensure same code as other languages
 * - Avoiding Dao and Mapper with Bean
 *
 * @author DataStax Developer Advocates team.
 * @author DataStax Curriculum team.
 */
public class UserDseDao {
    
    public static final String COLUMN_USERID       = "userid";
    public static final String COLUMN_LASTNAME     = "lastname";
    public static final String COLUMN_FIRSTNAME    = "firstname";
    public static final String COLUMN_EMAIL        = "email";
    public static final String COLUMN_CREATED_DATE = "userid";
    public static final String COLUMN_PASSWORD     = "created_date";
    
    /** Connectivity to Cassandra/DSE. */
    private CqlSession cqlSession;
    
    /** Constructor for the class, providing Session */
    public UserDseDao(CqlSession cqlSession) {
        this.cqlSession   = cqlSession;
    }
    
    /**
     * EXERCICE #1
     * 
     * Retrieving a record in table 'user_credentials' based on primary key 'email'
     * 
     * @param email
     *      value for the pk
     * @return
     *      expected statement
     */
    private SimpleStatement createStatemenToFindUserCredentials(String email) {
        return SimpleStatement.builder("select * from user_credentials where email=?")
                              .addPositionalValues(email).build();
    }
    
    /**
     * EXERCICE #2
     * 
     * Create a statement to insert record into table 'user_credentials'
     * 
     * @param userid
     *      user unique identifier
     * @param email
     *      user email adress (PK)
     * @param password
     *      user encoded password
     * @return
     *      expected statement
     */
    private SimpleStatement createStatementToInsertUserCredentials(UUID userid, String email, String password) {
        return SimpleStatement.builder(""
             + "INSERT INTO user_credentials (userid,email,\"password\") "
             + "VALUES (?,?,?) IF NOT EXISTS")
                            .addPositionalValues(userid, email, password)
                            .build();
    }
    
    /**
     * EXERCICE #3
     * 
     * Create a statement to insert record into table 'users'
     * 
     * @param user
     *      Java object wrapping all expected properties
     * @param password
     *      user encoded password
     * @return
     *      expected statement
     */
    private SimpleStatement createStatementToInserUser(User user) {
        return SimpleStatement
                .builder("INSERT INTO users (userid,firstname,lastname,email,created_date) "
                        + "VALUES (?,?,?,?,?) "
                        + "IF NOT EXISTS")
                .addPositionalValues(
                        user.getUserid(), user.getFirstname(), user.getLastname(), 
                        user.getEmail(), Instant.now())
                .build();
    }
    
    /**
     * EXERCICE #4
     * 
     * Create a statement search for users based on their uniau user identifier (PK)
     * 
     * @param listOfUserIds
     *     enumeration of searched user identifiers
     * @return
     *      expected statement
     */
    private SimpleStatement createStatementToSearchUsers(List<UUID> listOfUserIds) {
        return SimpleStatement
                .builder("SELECT * FROM users WHERE userid IN ?")
                .addPositionalValues(listOfUserIds)
                .build();
    }
    
    /* Execute Synchronously */
    public UserCredentials getUserCredential(String email) {
        ResultSet rs  = cqlSession.execute(createStatemenToFindUserCredentials(email));
        Row       row = rs.one(); // Request with Pk ensure unicity
        return mapAsUserCredential(row);                                             
    }
    
    /* Execute ASynchronously */
    public CompletionStage<UserCredentials> getUserCredentialAsync(String email) {
        return cqlSession.executeAsync(createStatemenToFindUserCredentials(email))
                         .thenApply(AsyncResultSet::one)
                         .thenApply(this::mapAsUserCredential);
    }

    /** {@inheritDoc} */
    public CompletionStage<Void> createUserAsync(User user, String hashedPassword) {
        
        CompletionStage<AsyncResultSet> resultInsertCredentials = cqlSession.executeAsync(
                createStatementToInsertUserCredentials(user.getUserid(), user.getEmail(), hashedPassword));
        
        CompletionStage<AsyncResultSet> resultInsertUser = resultInsertCredentials.thenCompose(rs -> {
          if (rs != null && rs.wasApplied()) {
              return cqlSession.executeAsync(createStatementToInserUser(user));
          }
          return resultInsertCredentials;
        });

        return resultInsertUser.thenAccept(rs -> {
            if (rs != null && !rs.wasApplied()) {
                String errMsg = "Exception creating user because it already exists";
                throw new CompletionException(errMsg, new IllegalArgumentException(errMsg));
            }
        });
    }

    /** {@inheritDoc} */
    public CompletionStage<List<User>> getUserProfilesAsync(List<UUID> userids) {
        return cqlSession.executeAsync(createStatementToSearchUsers(userids))
                .thenApply(AsyncResultSet::currentPage)
                .thenApply(rowList -> StreamSupport
                    .stream(rowList.spliterator(), false)
                    .filter(Objects::nonNull)
                    .map(this::mapAsUser)
                    .collect(Collectors.toList()));
    }
    
    /* Map from Row to expected Bean */
    protected UserCredentials mapAsUserCredential(Row row) {
        return new UserCredentials(
                row.getString("email"),
                row.getString("password"),
                row.getUuid("userid"));
    }
    
    protected User mapAsUser(Row row) {
        return new User(
                row.getUuid(COLUMN_USERID),
                row.getString("firstname"),
                row.getString("lastname"),
                row.getString("email"), 
                Date.from(row.getInstant("created_date")));
    }
    
}
