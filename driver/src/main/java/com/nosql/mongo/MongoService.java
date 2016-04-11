package com.nosql.mongo;

import com.nosql.Service;
import com.nosql.schema.MetaCollection;
import com.nosql.schema.MetaField;
import org.bson.types.ObjectId;

import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MongoService implements Service {

    private final JMongoClient client;
    private String uri;
    protected final HashMap<String,MetaCollection> metaCollections = new HashMap<String,MetaCollection>();

    // USE STATIC SO OPENING A NEW CONNECTION WILL REMEMBER THIS
    public static final List<String> createdDatabases = new ArrayList<String>();


    public MongoService(final String uri, final Properties prop) throws UnknownHostException {
        this.uri = uri;
        client = new JMongoClient( uri );
    }

    @Override
    public String getCurrentDatabaseName() {
        // SEE THIS TO SEE HOW DATABASE NAME IS USED : http://api.mongodb.org/java/current/com/mongodb/MongoClientURI.html
        return client.databaseName != null ? client.databaseName : "admin";
    }

    @Override
    public List<String> getDatabaseNames() {
        client.testConnectivity();

        final List<String> names = new ArrayList<String>();
        try {
            // THIS OFTEN THROWS EXCEPTION BECAUSE OF MISSING RIGHTS. IN THIS CASE WE ONLY ADD CURRENT KNOWN DB.
            for ( String c : client.listDatabaseNames() ){
                names.add( c );
            }
        } catch ( Throwable ex ){
            names.add( getCurrentDatabaseName() );
        }
        for ( String str : createdDatabases ){
            if ( !names.contains( str )){
                names.add( str );
            }
        }
        return names;
    }

    @Override
    public JMongoDatabase getDatabase(String dbName) {
        return client.getDatabase(dbName);
    }

    @Override
    public List<JMongoDatabase> getDatabases() {
        final List<JMongoDatabase> list = new ArrayList<JMongoDatabase>();

        for ( String dbName : getDatabaseNames() ){
            list.add( getDatabase(dbName));
        }
        return list;
    }

    @Override
    public Collection<MetaCollection> getMetaCollections(){
        return metaCollections.values();
    }


    @Override
    public void clear() {
        metaCollections.clear();
        referencesDiscovered = false;
    }


    @Override
    public String getVersion(){
        return "1.1";
    }


    @Override
    public MetaCollection getMetaCollection(String catalogName, String collectionName){
        if ( collectionName == null || collectionName.length() == 0 ) return null;
        int idx = collectionName.indexOf('.');
        if ( idx > -1 ) collectionName = collectionName.substring(0, idx );

        String key = catalogName + "." + collectionName;
        MetaCollection metaCollection = metaCollections.get( key );
        if ( metaCollection == null ){
            metaCollection = discoverCollection( catalogName, collectionName );
            if ( metaCollection != null ){
                metaCollections.put( key, metaCollection );
            }
        }
        return metaCollection;

    }

    @Override
    public String getURI() {
        return uri;
    }


    @Override
    public List<String> getCollectionNames(String catalog) {
        List<String> list = new ArrayList<String>();
        try {
            JMongoDatabase db = client.getDatabase(catalog);
            if ( db != null ){
                for ( String str : db.listCollectionNames() ){
                    list.add( str );
                }
            }
            list.remove("system.indexes");
            list.remove("system.users");
            list.remove("system.version");
        } catch ( Throwable ex ){
            System.out.println("Cannot list collection names for " + catalog + ". " + ex );
        }
        return list;
    }


    @Override
    public MetaCollection discoverCollection(String dbOrCatalog, String collectionName){
        final JMongoDatabase mongoDatabase = getDatabase(dbOrCatalog);
        if ( mongoDatabase != null ){
            try {
                final JMongoCollection mongoCollection = mongoDatabase.getCollection( collectionName );
                if ( mongoCollection != null ){
                    return new MetaCollection( mongoCollection, dbOrCatalog, collectionName );
                }
            } catch ( Throwable ex ){
                System.out.println("Error discovering collection " + dbOrCatalog + "." + collectionName + ". " + ex );
                ex.printStackTrace();
            }
        }
        return null;
    }


    public JMongoCollection getJMongoCollection( String databaseName, String collectionName ){
        final JMongoDatabase mongoDatabase = client.getDatabase( databaseName );
        if ( mongoDatabase != null ){
            return mongoDatabase.getCollection( collectionName );
        }
        return null;
    }


    private boolean referencesDiscovered = false;

    @Override
    public void discoverReferences(){
        if ( !referencesDiscovered){
            try {
                referencesDiscovered = true;
                final List<MetaField> unsolvedFields = new ArrayList<MetaField>();
                final List<MetaField> solvedFields = new ArrayList<MetaField>();
                for ( MetaCollection collection : metaCollections.values() ){
                    collection.collectFieldsWithObjectId(unsolvedFields);
                }
                if ( !unsolvedFields.isEmpty() ){
                    for ( MetaCollection collection : metaCollections.values() ){
                        final JMongoCollection mongoCollection = getJMongoCollection( collection.db, collection.name );
                        if ( mongoCollection != null ){
                            for ( MetaField metaField : unsolvedFields ){
                                for ( ObjectId objectId : metaField.objectIds){
                                    final Map<String,Object> query = new HashMap<String,Object>(); //new BasicDBObject();
                                    query.put("_id", objectId );
                                    if ( !solvedFields.contains( metaField ) && mongoCollection.find( query ).iterator().hasNext() ){
                                        solvedFields.add( metaField );
                                        metaField.createReferenceTo(collection);
                                        System.out.println("Found ref " + metaField.parentJson.name + " ( " + metaField.name + " ) ref " + collection.name );
                                    }
                                }
                            }
                        }
                    }
                }

            } catch ( Throwable ex ){
                ex.printStackTrace();
                System.out.println("Error in discover foreign keys " + ex );
            }
        }
    }



    @Override
    public String toString() {
        return client.toString();
    }

}
