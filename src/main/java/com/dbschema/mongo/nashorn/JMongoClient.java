package com.dbschema.mongo.nashorn;

import com.dbschema.mongo.SQLAlreadyClosedException;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.UuidRepresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.dbschema.mongo.DriverPropertyInfoHelper.*;
import static com.dbschema.mongo.Util.insertCredentials;


public class JMongoClient implements AutoCloseable {
  private boolean isClosed = false;
  private final MongoClient mongoClient;
  public final String databaseNameFromUrl;

  public JMongoClient(@NotNull String uri, @NotNull Properties prop, @Nullable String username, @Nullable String password) throws SQLException {
    try {
      boolean automaticEncoding = ENCODE_CREDENTIALS_DEFAULT;
      if (prop.getProperty(ENCODE_CREDENTIALS) != null) {
        automaticEncoding = Boolean.parseBoolean(prop.getProperty(ENCODE_CREDENTIALS));
      }
      uri = insertCredentials(uri, username, password, automaticEncoding);
      ConnectionString connectionString = new ConnectionString(uri);
      databaseNameFromUrl = connectionString.getDatabase();
      int maxPoolSize = getMaxPoolSize(prop);
      MongoClientSettings.Builder builder = MongoClientSettings.builder()
          .applyConnectionString(connectionString)
          .applyToConnectionPoolSettings(b -> b.maxSize(maxPoolSize));
      if ("true".equals(prop.getProperty("ssl"))) {
        builder.applyToSslSettings(s -> s.enabled(true));
      }
      if (connectionString.getUuidRepresentation() == null) {
        String uuidRepresentation = prop.getProperty(UUID_REPRESENTATION, UUID_REPRESENTATION_DEFAULT);
        builder.uuidRepresentation(createUuidRepresentation(uuidRepresentation));
      }
      if (connectionString.getServerSelectionTimeout() == null) {
        int timeout = Integer.parseInt(prop.getProperty(SERVER_SELECTION_TIMEOUT, SERVER_SELECTION_TIMEOUT_DEFAULT));
        builder.applyToClusterSettings(b -> b.serverSelectionTimeout(timeout, TimeUnit.MILLISECONDS));
      }
      if (connectionString.getConnectTimeout() == null) {
        int timeout = Integer.parseInt(prop.getProperty(CONNECT_TIMEOUT, CONNECT_TIMEOUT_DEFAULT));
        builder.applyToSocketSettings(b -> b.connectTimeout(timeout, TimeUnit.MILLISECONDS));
      }
      this.mongoClient = MongoClients.create(builder.build());
    }
    catch (Exception e) {
      throw new SQLException(e);
    }
  }

  private static UuidRepresentation createUuidRepresentation(String value) {
    if (value.equalsIgnoreCase("unspecified")) {
      return UuidRepresentation.UNSPECIFIED;
    }
    if (value.equalsIgnoreCase("javaLegacy")) {
      return UuidRepresentation.JAVA_LEGACY;
    }
    if (value.equalsIgnoreCase("csharpLegacy")) {
      return UuidRepresentation.C_SHARP_LEGACY;
    }
    if (value.equalsIgnoreCase("pythonLegacy")) {
      return UuidRepresentation.PYTHON_LEGACY;
    }
    if (value.equalsIgnoreCase("standard")) {
      return UuidRepresentation.STANDARD;
    }
    throw new IllegalArgumentException("Unknown uuid representation: " + value);
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

  public MongoDatabase getDatabase(String databaseName) throws SQLAlreadyClosedException {
    checkClosed();
    return mongoClient.getDatabase(databaseName);
  }

  @NotNull
  public MongoClient getMongoClient() {
    return mongoClient;
  }
}
