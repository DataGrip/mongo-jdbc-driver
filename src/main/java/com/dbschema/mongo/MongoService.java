package com.dbschema.mongo;

import com.dbschema.mongo.nashorn.JMongoClient;
import com.dbschema.mongo.nashorn.JMongoCollection;
import com.dbschema.mongo.nashorn.JMongoDatabase;
import com.dbschema.mongo.schema.MetaCollection;
import com.mongodb.MongoSecurityException;
import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
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

  public JMongoDatabase getDatabase(String dbName) throws SQLAlreadyClosedException {
    checkClosed();
    return client.getDatabase(dbName);
  }

  public List<JMongoDatabase> getDatabases() throws SQLAlreadyClosedException {
    final List<JMongoDatabase> list = new ArrayList<>();

    for (String dbName : getDatabaseNames()) {
      list.add(getDatabase(dbName));
    }
    return list;
  }

  @NotNull
  public String getVersion() throws SQLException {
    checkClosed();
    JMongoDatabase db = client.getDatabase("test");
    try {
      Document info = db.runCommand(new Document("buildInfo", 1));
      String version = info.getString("version");
      return version == null ? "UNKNOWN" : version;
    }
    catch (MongoSecurityException e) {
      throw new SQLException(e);
    }
  }


  public MetaCollection getMetaCollection(@NotNull String catalogName, String collectionName) throws SQLAlreadyClosedException {
    if (collectionName == null || collectionName.length() == 0) return null;
    return discoverCollection(catalogName, collectionName);
  }

  public String getURI() {
    return uri;
  }


  public List<String> getCollectionNames(String catalog) throws SQLAlreadyClosedException {
    checkClosed();
    List<String> list = new ArrayList<>();
    try {
      JMongoDatabase db = client.getDatabase(catalog);
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


  public MetaCollection discoverCollection(String dbOrCatalog, String collectionName) throws SQLAlreadyClosedException {
    final JMongoDatabase mongoDatabase = getDatabase(dbOrCatalog);
    if (mongoDatabase != null) {
      try {
        final JMongoCollection mongoCollection = mongoDatabase.getCollection(collectionName);
        if (mongoCollection != null) {
          return new MetaCollection(mongoCollection, dbOrCatalog, collectionName, fetchDocumentsForMeta);
        }
      }
      catch (Throwable ex) {
        System.err.println("Error discovering collection " + dbOrCatalog + "." + collectionName + ". " + ex);
        ex.printStackTrace();
      }
    }
    return null;
  }


  private JMongoCollection getJMongoCollection(String databaseName, String collectionName) throws SQLAlreadyClosedException {
    checkClosed();
    final JMongoDatabase mongoDatabase = client.getDatabase(databaseName);
    if (mongoDatabase != null) {
      return mongoDatabase.getCollection(collectionName);
    }
    return null;
  }

  @Override
  public String toString() {
    return client.toString();
  }

  public MongoClient getMongoClient() {
    return client.getMongoClient();
  }

}
