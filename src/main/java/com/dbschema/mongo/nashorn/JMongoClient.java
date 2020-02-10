package com.dbschema.mongo.nashorn;

import com.dbschema.mongo.SQLAlreadyClosedException;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;

import static com.dbschema.mongo.Util.insertCredentials;


public class JMongoClient implements AutoCloseable {
  private boolean isClosed = false;
  private final MongoClient mongoClient;
  public final String databaseNameFromUrl;

  public JMongoClient(@NotNull String uri, @NotNull Properties prop, @Nullable String username, @Nullable String password) {
    uri = insertCredentials(uri, username, password);
    ConnectionString connectionString = new ConnectionString(uri);
    databaseNameFromUrl = connectionString.getDatabase();
    MongoClientSettings.Builder builder = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .applyToConnectionPoolSettings(b -> b.maxSize(3));
    if ("true".equals(prop.getProperty("ssl"))) {
      builder.applyToSslSettings(s -> s.enabled(true));
    }
    this.mongoClient = MongoClients.create(builder.build());
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
