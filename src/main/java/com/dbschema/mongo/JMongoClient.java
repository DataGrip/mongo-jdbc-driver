package com.dbschema.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoIterable;

import java.util.Properties;

import static com.dbschema.mongo.JMongoUtil.nullize;


public class JMongoClient {

    private final MongoClient mongoClient;
    public final String databaseName;

    public JMongoClient(String uri, Properties prop)
    {
        ConnectionString connectionString = new ConnectionString(uri);
        databaseName = nullize(connectionString.getDatabase());
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyConnectionString(connectionString);
        if (prop != null && prop.getProperty("user") != null && prop.getProperty("password") != null) {
            builder.credential(
                    MongoCredential.createCredential(prop.getProperty("user"),
                            databaseName == null ? "admin" : databaseName,
                            prop.getProperty("password").toCharArray())
            );
        }
        this.mongoClient = MongoClients.create(builder.build());
    }

    public MongoIterable<String> listDatabaseNames()
    {
        return mongoClient.listDatabaseNames();
    }

    public JMongoDatabase getDatabase(String databaseName)
    {
        return new JMongoDatabase(mongoClient.getDatabase(databaseName));
    }

    public void testConnectivity()
    {
        mongoClient.getClusterDescription();
    }
}
