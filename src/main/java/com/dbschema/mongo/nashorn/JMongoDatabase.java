package com.dbschema.mongo.nashorn;

import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.CreateCollectionOptions;
import jdk.nashorn.api.scripting.AbstractJSObject;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.dbschema.mongo.nashorn.MemberFunction.*;


// https://wiki.openjdk.java.net/display/Nashorn/Nashorn+extensions
// http://sites.psu.edu/robertbcolton/2015/07/31/java-8-nashorn-script-engine/
public class JMongoDatabase extends AbstractJSObject {
  private final MongoDatabase mongoDatabase;
  private final MongoJSObject delegate;

  public JMongoDatabase(MongoDatabase mongoDatabase) {
    this.mongoDatabase = mongoDatabase;
    delegate = new MongoJSObject(Arrays.asList(
        func("getCollection",        this::getCollection, String.class),
        func("getCollection",        this::getCollection, String.class, Class.class),
        voidFunc("createCollection", mongoDatabase::createCollection, String.class),
        voidFunc("createCollection", mongoDatabase::createCollection, String.class, CreateCollectionOptions.class),
        voidFunc("createView",       mongoDatabase::createView, String.class, String.class, List.class),
        func("runCommand",           this::runCommand, String.class),
        func("runCommand",           this::runCommand, Map.class),
        func("runCommand",           mongoDatabase::runCommand, Bson.class, Class.class),
        func("runCommand",           mongoDatabase::runCommand, Bson.class, ReadPreference.class),
        func("runCommand",           mongoDatabase::runCommand, Bson.class, ReadPreference.class, Class.class),
        voidFunc("drop",             mongoDatabase::drop),
        func("listCollectionNames",  this::listCollectionNames),
        func("listCollections",      mongoDatabase::listCollections),
        func("listCollections",      mongoDatabase::listCollections, Class.class),
        func("version",              this::version),
        func("getCodecRegistry",     mongoDatabase::getCodecRegistry),
        func("getReadPreference",    mongoDatabase::getReadPreference),
        func("getWriteConcern",      mongoDatabase::getWriteConcern),
        func("withCodecRegistry",    this::withCodecRegistry, CodecRegistry.class),
        func("withReadPreference",   this::withReadPreference, ReadPreference.class),
        func("withWriteConcern",     this::withWriteConcern, WriteConcern.class)
    ));
  }

  public String getName() {
    return mongoDatabase.getName();
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
    mongoDatabase.withWriteConcern(writeConcern);
    return this;
  }

  public JMongoCollection<Document> getCollection(String s) {
    return new JMongoCollection<>(mongoDatabase.getCollection(s), s, mongoDatabase, Document.class);
  }

  public <TDocument> JMongoCollection<TDocument> getCollection(String s, Class<TDocument> tDocumentClass) {
    return new JMongoCollection<>(mongoDatabase.getCollection(s, tDocumentClass), s, mongoDatabase, tDocumentClass);
  }

  public Document runCommand(String str) {
    return mongoDatabase.runCommand(JMongoUtil.parse(str));
  }

  @SuppressWarnings("unchecked")
  public Document runCommand(Map<?, ?> map) {
    JMongoUtil.doConversions((Map<String, Object>) map);
    return mongoDatabase.runCommand(new Document((Map<String, Object>) map));
  }

  public MongoIterable<String> listCollectionNames() {
    return mongoDatabase.listCollectionNames();
  }

  private Iterable<String> version() {
    Document info = mongoDatabase.runCommand(new Document("buildinfo", null));
    String v = info.getString("version");
    return v == null ? null : Collections.singletonList(v);
  }

  public void createCollection(String s) {
    mongoDatabase.createCollection(s);
  }

  /**
   * I overwrite this methods to make possible to call database.collection.....
   * To perform like a collection is a member variable of the database.
   * Only getCollection(), drop(), runCommand() functions continue to work on this object.
   */
  @Override
  public boolean hasMember(String name) {
    return delegate.hasMember(name);
  }

  @Override
  public Object getMember(final String name) {
    AbstractJSObject member = delegate.getMember(name);
    return member != null ? member : getCollection(name);
  }
}
