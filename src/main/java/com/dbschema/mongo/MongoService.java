package com.dbschema.mongo;

import com.dbschema.mongo.nashorn.JMongoClient;
import com.dbschema.mongo.schema.MetaCollection;
import com.mongodb.MongoSecurityException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class MongoService implements AutoCloseable {
  private boolean isClosed = false;
  private final JMongoClient client;
  private final String uri;
  private final int fetchDocumentsForMeta;

  // USE STATIC SO OPENING A NEW CONNECTION WILL REMEMBER THIS
  public static final List<String> createdDatabases = new ArrayList<>();


  public MongoService(@NotNull String uri, @NotNull Properties prop, @Nullable String username,
                      @Nullable String password, int fetchDocumentsForMeta) throws SQLException {
    this.uri = uri;
    this.fetchDocumentsForMeta = fetchDocumentsForMeta;
    client = new JMongoClient(uri, prop, username, password);
  }

  public JMongoClient getClient() {
    return client;
  }

  @Override
  public void close() throws SQLAlreadyClosedException {
    checkClosed();
    isClosed = true;
    client.close();
  }

  private void checkClosed() throws SQLAlreadyClosedException {
    if (isClosed) throw new SQLAlreadyClosedException(this.getClass().getSimpleName());
  }

  public String getDatabaseNameFromUrl() throws SQLAlreadyClosedException {
    checkClosed();
    return client.databaseNameFromUrl != null ? client.databaseNameFromUrl : "test";
  }

  public List<String> getDatabaseNames() throws SQLAlreadyClosedException {
    checkClosed();

    final List<String> names = new ArrayList<>();
    try {
      // THIS OFTEN THROWS EXCEPTION BECAUSE OF MISSING RIGHTS. IN THIS CASE WE ONLY ADD CURRENT KNOWN DB.
      for (String c : client.listDatabaseNames()) {
        names.add(c);
      }
    }
    catch (Throwable ex) {
      names.add(getDatabaseNameFromUrl());
    }
    for (String str : createdDatabases) {
      if (!names.contains(str)) {
        names.add(str);
      }
    }
    return names;
  }

  public MongoDatabase getDatabase(String dbName) throws SQLAlreadyClosedException {
    checkClosed();
    return client.getDatabase(dbName);
  }

  @NotNull
  public List<MongoDatabase> getDatabases(MongoNamePattern dbName) throws SQLAlreadyClosedException {
    checkClosed();
    String plain = dbName.asPlain();
    if (plain != null) {
      return Collections.singletonList(client.getDatabase(plain));
    }
    List<MongoDatabase> databases = new ArrayList<>();
    for (String databaseName : client.getMongoClient().listDatabaseNames()) {
      if (dbName.matches(databaseName)) {
        databases.add(client.getMongoClient().getDatabase(databaseName));
      }
    }
    return databases;
  }

  public List<MongoDatabase> getDatabases() throws SQLAlreadyClosedException {
    final List<MongoDatabase> list = new ArrayList<>();

    for (String dbName : getDatabaseNames()) {
      list.add(getDatabase(dbName));
    }
    return list;
  }

  @NotNull
  public String getVersion() throws SQLException {
    checkClosed();
    MongoDatabase db = client.getDatabase("test");
    try {
      Document info = db.runCommand(new Document("buildInfo", 1));
      String version = info.getString("version");
      return version == null ? "UNKNOWN" : version;
    }
    catch (MongoSecurityException e) {
      throw new SQLException(e);
    }
  }


  @NotNull
  public List<MetaCollection> getMetaCollections(@Nullable String databasePattern, @Nullable String collectionPattern) throws SQLAlreadyClosedException {
    MongoNamePattern collectionName = MongoNamePattern.create(collectionPattern);
    List<MongoDatabase> databases = getDatabases(MongoNamePattern.create(databasePattern));
    List<MetaCollection> collections = new ArrayList<>();
    for (MongoDatabase database : databases) {
      try {
        String plainCollectionName = collectionName.asPlain();
        if (plainCollectionName != null) {
          MongoCollection<Document> collection = database.getCollection(plainCollectionName);
          collections.add(new MetaCollection(collection, fetchDocumentsForMeta));
        }
        else {
          for (String name : database.listCollectionNames()) {
            if (collectionName.matches(name)) {
              MongoCollection<Document> collection = database.getCollection(name);
              collections.add(new MetaCollection(collection, fetchDocumentsForMeta));
            }
          }
        }
      }
      catch (Throwable ex) {
        System.err.println("Error discovering collection " + database + " " + collectionName + ". " + ex);
        ex.printStackTrace();
      }
    }
    return collections;
  }

  public String getURI() {
    return uri;
  }


  public List<String> getCollectionNames(String catalog) throws SQLAlreadyClosedException {
    checkClosed();
    List<String> list = new ArrayList<>();
    try {
      MongoDatabase db = client.getDatabase(catalog);
      if (db != null) {
        for (String str : db.listCollectionNames()) {
          list.add(str);
        }
      }
      list.remove("system.indexes");
      list.remove("system.users");
      list.remove("system.version");
    }
    catch (Throwable ex) {
      System.err.println("Cannot list collection names for " + catalog + ". " + ex);
    }
    return list;
  }

  @Override
  public String toString() {
    return client.toString();
  }

  public MongoClient getMongoClient() {
    return client.getMongoClient();
  }

}
