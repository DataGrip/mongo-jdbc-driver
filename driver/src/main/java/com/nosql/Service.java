package com.nosql;

import com.nosql.mongo.JMongoDatabase;
import com.nosql.schema.MetaCollection;

import java.util.Collection;
import java.util.List;

public interface Service {

    public String getURI();

    public String getCurrentDatabaseName();

	public JMongoDatabase getDatabase(String dbName);

    public MetaCollection discoverCollection(String catalog, String collectionName );

	public List<String> getDatabaseNames();

    public List<JMongoDatabase> getDatabases();

    public void discoverReferences();

    public void clear();

    public List<String> getCollectionNames(String catalog);

    public MetaCollection getMetaCollection(String catalogName, String collectionName);

    public Collection<MetaCollection> getMetaCollections();

    public String getVersion();


}