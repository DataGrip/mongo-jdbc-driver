package com.dbschema.mongo.nashorn;

import com.dbschema.mongo.SQLAlreadyClosedException;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Properties;

import static com.dbschema.mongo.DriverPropertyInfoHelper.MAX_POOL_SIZE;
import static com.dbschema.mongo.DriverPropertyInfoHelper.MAX_POOL_SIZE_DEFAULT;
import static com.dbschema.mongo.Util.insertCredentials;


public class JMongoClient implements AutoCloseable {
  private boolean isClosed = false;
  private final MongoClient mongoClient;
  public final String databaseNameFromUrl;

  public JMongoClient(@NotNull String uri, @NotNull Properties prop, @Nullable String username, @Nullable String password) throws SQLException {
    try {
      uri = insertCredentials(uri, username, password);
      ConnectionString connectionString = new ConnectionString(uri);
      databaseNameFromUrl = connectionString.getDatabase();
      int maxPoolSize = getMaxPoolSize(prop);
      MongoClientSettings.Builder builder = MongoClientSettings.builder()
          .applyConnectionString(connectionString)
          .applyToConnectionPoolSettings(b -> b.maxSize(maxPoolSize));
      if ("true".equals(prop.getProperty("ssl"))) {
        builder.applyToSslSettings(s -> s.enabled(true));
      }
      this.mongoClient = MongoClients.create(builder.build());
    }
    catch (Exception e) {
      throw new SQLException(e);
    }
  }

  private int getMaxPoolSize(@NotNull Properties prop) {
    try {
      String str = prop.getProperty(MAX_POOL_SIZE);
      if (str != null) {
        int poolSize = Integer.parseInt(str);
        return poolSize > 0 ? poolSize : 1;
      }
    }
    catch (NumberFormatException e) {
      e.printStackTrace();
    }
    return MAX_POOL_SIZE_DEFAULT;
  }

  @Override
  public void close() throws SQLAlreadyClosedException {
    checkClosed();
    isClosed = true;
    mongoClient.close();
  }

  private void checkClosed() throws SQLAlreadyClosedException {
    if (isClosed) throw new SQLAlreadyClosedException(this.getClass().getSimpleName());
  }

  public MongoIterable<String> listDatabaseNames() throws SQLAlreadyClosedException {
    checkClosed();
    return mongoClient.listDatabaseNames();
  }

  public JMongoDatabase getDatabase(String databaseName) throws SQLAlreadyClosedException {
    checkClosed();
    return new JMongoDatabase(mongoClient.getDatabase(databaseName), mongoClient);
  }

  public void testConnectivity() throws SQLAlreadyClosedException {
    checkClosed();
    mongoClient.getClusterDescription();
  }
}
