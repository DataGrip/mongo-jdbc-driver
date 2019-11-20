package com.dbschema.mongo.nashorn;


import com.dbschema.mongo.resultSet.ListResultSet;
import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import jdk.nashorn.api.scripting.AbstractJSObject;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.dbschema.mongo.nashorn.JMongoUtil.toBson;
import static com.dbschema.mongo.nashorn.MemberFunction.func;
import static com.dbschema.mongo.nashorn.MemberFunction.voidFunc;

public class JMongoCollection<TDocument> extends AbstractJSObject {
  private final MongoCollection<TDocument> nativeCollection;
  private final String name;
  private final MongoDatabase mongoDatabase;
  private final MongoJSObject delegate;

  public JMongoCollection(MongoCollection<TDocument> nativeCollection, String name, MongoDatabase mongoDatabase, Class<TDocument> tDocumentClass) {
    this.nativeCollection = nativeCollection;
    this.name = name;
    this.mongoDatabase = mongoDatabase;
    delegate = new MongoJSObject(Arrays.asList(
        voidFunc("insert",     this::insert, String.class),
        voidFunc("insert",     this::insert, Map.class),
        voidFunc("insert",     this::insert, String.class),
        voidFunc("insertOne",  this::insertOne, String.class),
        voidFunc("insertOne",  this::insertOne, String.class),
        voidFunc("insertOne",  this::insertOne, tDocumentClass),
        voidFunc("insertMany", nativeCollection::insertMany, List.class),
        voidFunc("insertMany", nativeCollection::insertMany, List.class, InsertManyOptions.class),
        voidFunc("drop",       nativeCollection::drop),
        voidFunc("dropIndex",  nativeCollection::dropIndex, String.class),
        voidFunc("dropIndex",  nativeCollection::dropIndex, Bson.class),
        voidFunc("dropIndexes",nativeCollection::dropIndexes),
        voidFunc("renameCollection", this::renameCollection, String.class),
        voidFunc("renameCollection", nativeCollection::renameCollection, MongoNamespace.class),
        voidFunc("renameCollection", nativeCollection::renameCollection, MongoNamespace.class, RenameCollectionOptions.class),
        func("count",      this::count, Map.class),
        func("count",      this::count, String.class, CountOptions.class),
        func("count",      this::count),
        func("count",      this::count, Bson.class),
        func("count",      this::count, Bson.class, CountOptions.class),
        func("find",       this::find, String.class),
        func("find",       this::find, String.class, String.class),
        func("find",       this::find, String.class, Class.class),
        func("find",       this::find, Map.class),
        func("find",       this::find, Map.class, Map.class),
        func("find",       this::find, Bson.class, Bson.class),
        func("find",             this::find),
        func("find",             this::find, Class.class),
        func("find",             this::find, Bson.class),
        func("find",             this::find, Bson.class, Class.class),
        func("delete",     this::delete, String.class),
        func("delete",     this::delete, Map.class),
        func("deleteOne",  this::deleteOne, String.class),
        func("deleteOne",  nativeCollection::deleteOne, Bson.class),
        func("deleteOne",  this::deleteOne, Map.class),
        func("deleteMany", this::deleteMany, String.class),
        func("deleteMany", nativeCollection::deleteMany, Bson.class),
        func("replaceOne", nativeCollection::replaceOne, Bson.class, tDocumentClass),
        func("replaceOne", this::replaceOne, Bson.class, tDocumentClass, UpdateOptions.class),
        func("replaceOne", this::replaceOne, String.class, tDocumentClass),
        func("replaceOne", this::replaceOne, String.class, tDocumentClass, UpdateOptions.class),
        func("update",     this::update, Map.class, Map.class),
        func("update",     this::update, Map.class, Map.class, UpdateOptions.class),
        func("updateOne",  nativeCollection::updateOne, Bson.class, Bson.class),
        func("updateOne",  nativeCollection::updateOne, Bson.class, Bson.class, UpdateOptions.class),
        func("updateOne",  this::updateOne, Map.class, Map.class),
        func("updateOne",  this::updateOne, String.class, String.class),
        func("updateOne",  this::updateOne, Map.class, Map.class, UpdateOptions.class),
        func("updateOne",  this::updateOne, String.class, String.class, UpdateOptions.class),
        func("updateMany", nativeCollection::updateMany, Bson.class, Bson.class),
        func("updateMany", nativeCollection::updateMany, Bson.class, Bson.class, UpdateOptions.class),
        func("updateMany", this::updateMany, Map.class, Map.class),
        func("updateMany", this::updateMany, String.class, String.class),
        func("updateMany", this::updateMany, Map.class, Map.class, UpdateOptions.class),
        func("updateMany", this::updateMany, String.class, String.class, UpdateOptions.class),
        func("findOneAndDelete",  nativeCollection::findOneAndDelete, Bson.class),
        func("findOneAndDelete",  nativeCollection::findOneAndDelete, Bson.class, FindOneAndDeleteOptions.class),
        func("findOneAndDelete",  this::findOneAndDelete, String.class),
        func("findOneAndDelete",  this::findOneAndDelete, Map.class),
        func("findOneAndDelete",  this::findOneAndDelete, String.class, FindOneAndDeleteOptions.class),
        func("findOneAndReplace", this::findOneAndReplace, String.class, tDocumentClass),
        func("findOneAndReplace", this::findOneAndReplace, String.class, tDocumentClass, FindOneAndReplaceOptions.class),
        func("findOneAndReplace", nativeCollection::findOneAndReplace, Bson.class, tDocumentClass),
        func("findOneAndReplace", nativeCollection::findOneAndReplace, Bson.class, tDocumentClass, FindOneAndReplaceOptions.class),
        func("findOneAndUpdate",  this::findOneAndUpdate, String.class, String.class),
        func("findOneAndUpdate",  this::findOneAndUpdate, String.class, String.class, FindOneAndUpdateOptions.class),
        func("findOneAndUpdate",  nativeCollection::findOneAndUpdate, Bson.class, Bson.class),
        func("findOneAndUpdate",  nativeCollection::findOneAndUpdate, Bson.class, Bson.class, FindOneAndUpdateOptions.class),
        func("createIndex",       this::createIndex, String.class),
        func("createIndex",       nativeCollection::createIndex, Bson.class),
        func("createIndex",       nativeCollection::createIndex, Bson.class, IndexOptions.class),
        func("createIndex",       this::createIndex, Map.class),
        func("createIndex",       this::createIndex, String.class, IndexOptions.class),
        func("createIndex",       this::createIndex, Map.class, Map.class),
        func("createIndexes",     nativeCollection::createIndexes, List.class),
        func("listIndexes",       this::listIndexes),
        func("listIndexes",       nativeCollection::listIndexes, Class.class),
        func("getNamespace",      nativeCollection::getNamespace),
        func("getDocumentClass",  nativeCollection::getDocumentClass),
        func("getCodecRegistry",  nativeCollection::getCodecRegistry),
        func("getReadPreference", nativeCollection::getReadPreference),
        func("getWriteConcern",   nativeCollection::getWriteConcern),
        func("withDocumentClass", this::withDocumentClass, Class.class),
        func("withCodecRegistry", this::withCodecRegistry, CodecRegistry.class),
        func("withReadPreference",this::withReadPreference, ReadPreference.class),
        func("withWriteConcern",  this::withWriteConcern, WriteConcern.class),
        func("distinct",  this::distinct, String.class, Class.class),
        func("aggregate", this::aggregate, Map.class),
        func("aggregate", nativeCollection::aggregate, List.class, Class.class),
        func("mapReduce", nativeCollection::mapReduce, String.class, String.class),
        func("mapReduce", nativeCollection::mapReduce, String.class, String.class, Class.class),
        func("bulkWrite", nativeCollection::bulkWrite, List.class),
        func("bulkWrite", nativeCollection::bulkWrite, List.class, BulkWriteOptions.class)));
  }

  @Override
  public boolean hasMember(String name) {
    return delegate.hasMember(name);
  }

  @Override
  public Object getMember(String name) {
    AbstractJSObject member = delegate.getMember(name);
    return member != null ? member : getSubCollection(name);
  }

  private JMongoCollection<Document> getSubCollection(String name) {
    String newName = this.name + "." + name;
    return new JMongoCollection<>(mongoDatabase.getCollection(newName), newName, mongoDatabase, Document.class);
  }

  public void insert(String str) {
    insertOne(str);
  }

  public void insertOne(String str) {
    //noinspection unchecked
    nativeCollection.insertOne((TDocument) JMongoUtil.parse(str));
  }

  public void insert(Map<?, ?> map) {
    insertOne(map);
  }

  public void insertOne(Map<?, ?> map) {
    //noinspection unchecked
    nativeCollection.insertOne((TDocument) toBson(map));
  }


  public ResultSet count(Map<?, ?> map) {
    //noinspection unchecked
    return wrapInResultSet("count", nativeCollection.countDocuments(new Document((Map<String, Object>) map)));
  }

  public ResultSet count(String str, CountOptions countOptions) {
    return wrapInResultSet("count", nativeCollection.countDocuments(JMongoUtil.parse(str), countOptions));
  }

  public JFindIterable<TDocument> find(String str) {
    return new JFindIterable<>(nativeCollection.find(JMongoUtil.parse(str)));
  }

  public JFindIterable<TDocument> find(String str, String proj) {
    return new JFindIterable<>(nativeCollection.find(JMongoUtil.parse(str)).projection(JMongoUtil.parse(proj)));
  }

  public <TResult> JFindIterable<TResult> find(String str, Class<TResult> aClass) {
    return new JFindIterable<>(nativeCollection.find(JMongoUtil.parse(str), aClass));
  }

  public JFindIterable<TDocument> find(Map<?, ?> map) {
    return new JFindIterable<>(nativeCollection.find(toBson(map)));
  }

  public JFindIterable<TDocument> find(Map<?, ?> map, Map<?, ?> proj) {
    return new JFindIterable<>(nativeCollection.find(toBson(map)).projection(toBson(proj)));
  }

  public JFindIterable<TDocument> find(Bson bson, Bson proj) {
    return new JFindIterable<>(nativeCollection.find(bson)).projection(proj);
  }


  public DeleteResult delete(String str) {
    return deleteOne(str);
  }

  public DeleteResult deleteOne(String str) {
    return nativeCollection.deleteOne(JMongoUtil.parse(str));
  }

  public DeleteResult delete(Map<?, ?> map) {
    return deleteOne(map);
  }

  public DeleteResult deleteOne(Map<?, ?> map) {
    return nativeCollection.deleteOne(toBson(map));
  }


  public DeleteResult deleteMany(String str) {
    return nativeCollection.deleteMany(JMongoUtil.parse(str));
  }


  public UpdateResult replaceOne(String str, TDocument o) {
    return nativeCollection.replaceOne(JMongoUtil.parse(str), o);
  }


  public UpdateResult replaceOne(String str, TDocument o, UpdateOptions updateOptions) {
    //noinspection deprecation
    return nativeCollection.replaceOne(JMongoUtil.parse(str), o, updateOptions);
  }

  public UpdateResult updateOne(Map<?, ?> map, Map<?, ?> map1) {
    return nativeCollection.updateOne(toBson(map), toBson(map1));
  }

  public UpdateResult updateOne(String str, String str1) {
    return nativeCollection.updateOne(JMongoUtil.parse(str), BsonDocument.parse(str1));
  }

  public UpdateResult updateOne(Map<?, ?> map, Map<?, ?> map1, UpdateOptions updateOptions) {
    return nativeCollection.updateOne(toBson(map), toBson(map1), updateOptions);
  }

  public UpdateResult updateOne(String str, String str1, UpdateOptions updateOptions) {
    return nativeCollection.updateOne(JMongoUtil.parse(str), BsonDocument.parse(str1), updateOptions);
  }

  public UpdateResult update(Map<?, ?> map, Map<?, ?> map1) {
    return updateMany(map, map1);
  }

  public UpdateResult updateMany(Map<?, ?> map, Map<?, ?> map1) {
    //noinspection unchecked
    return nativeCollection.updateMany(new Document((Map<String, Object>) map), new Document((Map<String, Object>) map1));
  }

  public UpdateResult updateMany(String str, String str1) {
    return nativeCollection.updateMany(JMongoUtil.parse(str), BsonDocument.parse(str1));
  }

  public UpdateResult update(Map<?, ?> map, Map<?, ?> map1, UpdateOptions updateOptions) {
    return updateMany(map, map1, updateOptions);
  }

  public UpdateResult updateMany(Map<?, ?> map, Map<?, ?> map1, UpdateOptions updateOptions) {
    return nativeCollection.updateMany(toBson(map), toBson(map1), updateOptions);
  }

  public UpdateResult updateMany(String str, String str1, UpdateOptions updateOptions) {
    return nativeCollection.updateMany(JMongoUtil.parse(str), BsonDocument.parse(str1), updateOptions);
  }

  public TDocument findOneAndDelete(Map<?, ?> map) {
    return nativeCollection.findOneAndDelete(toBson(map));
  }

  public TDocument findOneAndDelete(String str) {
    return nativeCollection.findOneAndDelete(JMongoUtil.parse(str));
  }


  public TDocument findOneAndDelete(String str, FindOneAndDeleteOptions findOneAndDeleteOptions) {
    return nativeCollection.findOneAndDelete(JMongoUtil.parse(str), findOneAndDeleteOptions);
  }


  public TDocument findOneAndReplace(String str, TDocument o) {
    return nativeCollection.findOneAndReplace(JMongoUtil.parse(str), o);
  }


  public TDocument findOneAndReplace(String str, TDocument o, FindOneAndReplaceOptions findOneAndReplaceOptions) {
    return nativeCollection.findOneAndReplace(JMongoUtil.parse(str), o, findOneAndReplaceOptions);
  }


  public TDocument findOneAndUpdate(String str, String str1) {
    return nativeCollection.findOneAndUpdate(JMongoUtil.parse(str), BsonDocument.parse(str1));
  }


  public TDocument findOneAndUpdate(String str, String str1, FindOneAndUpdateOptions findOneAndUpdateOptions) {
    return nativeCollection.findOneAndUpdate(JMongoUtil.parse(str), BsonDocument.parse(str1), findOneAndUpdateOptions);
  }


  public String createIndex(String str) {
    return nativeCollection.createIndex(JMongoUtil.parse(str));
  }

  public String createIndex(Map<?, ?> map) {
    return nativeCollection.createIndex(toBson(map));
  }

  public String createIndex(String str, IndexOptions indexOptions) {
    return nativeCollection.createIndex(JMongoUtil.parse(str), indexOptions);
  }

  public String createIndex(Map<?, ?> map, Map<?, ?> optionsMap) {
    return nativeCollection.createIndex(toBson(map), new IndexOptionsFromMap(optionsMap));
  }

  //---------------------------------------------------------------

  public JMongoCollection<TDocument> withDocumentClass(Class<?> aClass) {
    nativeCollection.withDocumentClass(aClass);
    return this;
  }

  public JMongoCollection<TDocument> withCodecRegistry(CodecRegistry codecRegistry) {
    nativeCollection.withCodecRegistry(codecRegistry);
    return this;
  }

  public JMongoCollection<TDocument> withReadPreference(ReadPreference readPreference) {
    nativeCollection.withReadPreference(readPreference);
    return this;
  }

  public JMongoCollection<TDocument> withWriteConcern(WriteConcern writeConcern) {
    nativeCollection.withWriteConcern(writeConcern);
    return this;
  }

  public ResultSet count() {
    return wrapInResultSet("count", nativeCollection.countDocuments());
  }

  private ResultSet wrapInResultSet(String columnName, Object value) {
    return new ListResultSet(value.toString(), new String[]{columnName});
  }

  public ResultSet count(Bson bson) {
    return wrapInResultSet("count", nativeCollection.countDocuments(bson));
  }

  public ResultSet count(Bson bson, CountOptions countOptions) {
    return wrapInResultSet("count", nativeCollection.countDocuments(bson, countOptions));
  }

  public <TResult> DistinctIterable<TResult> distinct(String s, Class<TResult> aClass) {
    return nativeCollection.distinct(s, aClass);
  }

  public JFindIterable<TDocument> find() {
    return new JFindIterable<>(nativeCollection.find());
  }

  public <TResult> JFindIterable<TResult> find(Class<TResult> aClass) {
    return new JFindIterable<>(nativeCollection.find(aClass));
  }

  public JFindIterable<TDocument> find(Bson bson) {
    return new JFindIterable<>(nativeCollection.find(bson));
  }

  public <TResult> JFindIterable<TResult> find(Bson bson, Class<TResult> aClass) {
    return new JFindIterable<>(nativeCollection.find(bson, aClass));
  }

  public JAggregateIterable<TDocument> aggregate(Map<?, ?>... maps) {
    List<Bson> list = new ArrayList<>();
    for (Map<?, ?> map : maps) {
      list.add(toBson(map));
    }
    return new JAggregateIterable<>(nativeCollection.aggregate(list));
  }

  public void insertOne(TDocument o) {
    nativeCollection.insertOne(o);
  }

  public UpdateResult replaceOne(Bson bson, TDocument o, UpdateOptions updateOptions) {
    //noinspection deprecation
    return nativeCollection.replaceOne(bson, o, updateOptions);
  }

  public ListIndexesIterable<Document> listIndexes() {
    return nativeCollection.listIndexes();
  }

  public void renameCollection(String newName) {
    nativeCollection.renameCollection(new MongoNamespace(nativeCollection.getNamespace().getDatabaseName(), newName));
  }
}
