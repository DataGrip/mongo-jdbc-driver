package com.nosql.mongo;

import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.ListCollectionsIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.CreateCollectionOptions;
import jdk.nashorn.api.scripting.AbstractJSObject;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;


//  https://wiki.openjdk.java.net/display/Nashorn/Nashorn+extensions
// http://sites.psu.edu/robertbcolton/2015/07/31/java-8-nashorn-script-engine/
public class JMongoDatabase extends AbstractJSObject {

    private final MongoDatabase mongoDatabase;

    public JMongoDatabase( MongoDatabase mongoDatabase ){

        this.mongoDatabase = mongoDatabase;
    }

    public String getName() {
        return mongoDatabase.getName();
    }

    public CodecRegistry getCodecRegistry() {
        return mongoDatabase.getCodecRegistry();
    }

    public ReadPreference getReadPreference() {
        return mongoDatabase.getReadPreference();
    }

    public WriteConcern getWriteConcern() {
        return mongoDatabase.getWriteConcern();
    }

    public JMongoDatabase withCodecRegistry(CodecRegistry codecRegistry) {
        mongoDatabase.withCodecRegistry(codecRegistry);
        return this;
    }

    public JMongoDatabase withReadPreference(ReadPreference readPreference) {
        mongoDatabase.withReadPreference(readPreference);
        return this;
    }

    public JMongoDatabase withWriteConcern(WriteConcern writeConcern) {
        mongoDatabase.withWriteConcern(writeConcern );
        return this;
    }

    public JMongoCollection<Document> getCollection(String s) {
        return new JMongoCollection<Document>(mongoDatabase.getCollection( s ));
    }

    public <TDocument> JMongoCollection<TDocument> getCollection(String s, Class<TDocument> tDocumentClass) {
        return new JMongoCollection<TDocument>( mongoDatabase.getCollection( s, tDocumentClass ) );
    }

    public Document runCommand(String str) {
        return mongoDatabase.runCommand( JMongoUtil.parse(str) ) ;
    }

    public Document runCommand( Map map ){
        JMongoUtil.doConversions(map);
        return mongoDatabase.runCommand(new Document(map));
    }

    public Document runCommand(Bson bson, ReadPreference readPreference) {
        return mongoDatabase.runCommand( bson, readPreference );
    }

    public <TResult> TResult runCommand(Bson bson, Class<TResult> tResultClass) {
        return mongoDatabase.runCommand(bson, tResultClass);
    }

    public <TResult> TResult runCommand(Bson bson, ReadPreference readPreference, Class<TResult> tResultClass) {
        return mongoDatabase.runCommand( bson, readPreference,  tResultClass);
    }



    public void drop() {
        mongoDatabase.drop();
    }

    public MongoIterable<String> listCollectionNames() {
        return mongoDatabase.listCollectionNames();
    }

    public ListCollectionsIterable<Document> listCollections() {
        return mongoDatabase.listCollections();
    }

    public <TResult> ListCollectionsIterable<TResult> listCollections(Class<TResult> tResultClass) {
        return mongoDatabase.listCollections(tResultClass);
    }

    public void createCollection( String s ) {
        mongoDatabase.createCollection( s );
    }

    public void createCollection(String s, CreateCollectionOptions createCollectionOptions) {
        mongoDatabase.createCollection(s, createCollectionOptions);
    }

    @Override
    public boolean hasMember(String name) {
        return "getCollection".equals( name ) || "drop".equals(name)|| "runCommand".equals(name);
    }

    @Override
    public Object getMember(String name) {
        if ( "getCollection".equals(name)){
            return new AbstractJSObject() {
                  @Override
                  public Object call(Object thiz, Object... args) {
                      return ( args.length == 1 ) ? getCollection(String.valueOf(args[0])) : null;
                  }
                  @Override
                  public boolean isFunction() {
                      return true;
                  }
              };
        }
        if ( "drop".equals(name)){
            return new AbstractJSObject() {
                  @Override
                  public Object call(Object thiz, Object... args) {
                      if ( args.length == 0 ) {
                          drop();
                      }
                      return null;
                  }
                  @Override
                  public boolean isFunction() {
                      return true;
                  }
              };
        }
        if ( "runCommand".equals(name)){
            return new AbstractJSObject() {
                @Override
                public Object call(Object thiz, Object... args) {
                    // THIS RUNS ONLY WITH STRING PARAMETRS- EVEN JSON LIKE THIS. I COULD NOT FIND A BETTER SOLUTION FOR IT
                    return ( args.length == 1 ) ? runCommand(String.valueOf(args[0])) : null;
                }
                  @Override
                  public boolean isFunction() {
                      return true;
                  }
              };
        }

        return getCollection( name );
    }


    /*
      Used in Groovy to expose collection as particular object

      //extends groovy.util.Proxy {
    @Override
    public Object getProperty( String name ){
        return getCollection( name );
    }
    */

}
