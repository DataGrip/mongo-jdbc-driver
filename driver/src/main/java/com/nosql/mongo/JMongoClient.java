package com.nosql.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.MongoIterable;
import org.bson.Document;

import java.util.List;


public class JMongoClient {

    private final MongoClient mongoClient;
    public final String databaseName;

    public JMongoClient( String uri ){
        // TODO HERE CAN FIND DATABASE FROM URI
        final MongoClientURI clientURI = new MongoClientURI(uri);
        this.databaseName = clientURI.getDatabase();
        this.mongoClient = new MongoClient(clientURI );
    }

    public MongoClientOptions getMongoClientOptions() {
        return mongoClient.getMongoClientOptions();
    }

    public List<MongoCredential> getCredentialsList() {
        return getCredentialsList();
    }

    public MongoIterable<String> listDatabaseNames() {
        return mongoClient.listDatabaseNames();
    }

    public ListDatabasesIterable<Document> listDatabases() {
        return mongoClient.listDatabases();
    }

    public <T> ListDatabasesIterable<T> listDatabases(Class<T> clazz) {
        return mongoClient.listDatabases(clazz);
    }

    public JMongoDatabase getDatabase(String databaseName) {
        return new JMongoDatabase( mongoClient.getDatabase(databaseName));
    }

    public void testConnectivity(){
        mongoClient.getAddress();
    }
}
