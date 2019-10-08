package com.dbschema.mongo.java;

import com.dbschema.mongo.java.JMongoDatabase;
import com.dbschema.mongo.schema.MetaCollection;

import java.util.Collection;
import java.util.List;

public interface Service {

  String getURI();

  String getDatabaseNameFromUrl();

  JMongoDatabase getDatabase(String dbName);

  MetaCollection discoverCollection(String catalog, String collectionName);

  List<String> getDatabaseNames();

  List<JMongoDatabase> getDatabases();

  void discoverReferences();

  List<String> getCollectionNames(String catalog);

  MetaCollection getMetaCollection(String catalogName, String collectionName);

  Collection<MetaCollection> getMetaCollections();

  String getVersion();


}