package com.dbschema.mongo.nashorn;


import com.dbschema.mongo.resultSet.ListResultSet;
import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class JMongoCollection<TDocument> {


  private final MongoCollection<TDocument> nativeCollection;

  public JMongoCollection(MongoCollection<TDocument> nativeCollection) {
    this.nativeCollection = nativeCollection;
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

  @SuppressWarnings("unchecked")
  public void insertOne(Map<?, ?> map) {
    JMongoUtil.doConversions((Map<String, Object>) map);
    nativeCollection.insertOne((TDocument) (new Document((Map<String, Object>) map)));
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

  @SuppressWarnings("unchecked")
  public JFindIterable<TDocument> find(Map<?, ?> map) {
    JMongoUtil.doConversions((Map<String, Object>) map);
    return new JFindIterable<>(nativeCollection.find(new Document((Map<String, Object>) map)));
  }

  @SuppressWarnings("unchecked")
  public JFindIterable<TDocument> find(Map<?, ?> map, Map<?, ?> proj) {
    JMongoUtil.doConversions((Map<String, Object>) map);
    JMongoUtil.doConversions((Map<String, Object>) proj);
    return new JFindIterable<>(nativeCollection.find(new Document((Map<String, Object>) map)).projection(new Document((Map<String, Object>) proj)));
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

  @SuppressWarnings("unchecked")
  public DeleteResult deleteOne(Map<?, ?> map) {
    JMongoUtil.doConversions((Map<String, Object>) map);
    return nativeCollection.deleteOne(new Document((Map<String, Object>) map));
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

  @SuppressWarnings("unchecked")
  public UpdateResult updateOne(Map<?, ?> map, Map<?, ?> map1) {
    JMongoUtil.doConversions((Map<String, Object>) map);
    JMongoUtil.doConversions((Map<String, Object>) map1);
    return nativeCollection.updateOne(new Document((Map<String, Object>) map), new Document((Map<String, Object>) map1));
  }

  public UpdateResult updateOne(String str, String str1) {
    return nativeCollection.updateOne(JMongoUtil.parse(str), BsonDocument.parse(str1));
  }

  @SuppressWarnings("unchecked")
  public UpdateResult updateOne(Map<?, ?> map, Map<?, ?> map1, UpdateOptions updateOptions) {
    JMongoUtil.doConversions((Map<String, Object>) map);
    JMongoUtil.doConversions((Map<String, Object>) map1);
    return nativeCollection.updateOne(new Document((Map<String, Object>) map), new Document((Map<String, Object>) map1), updateOptions);
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

  @SuppressWarnings("unchecked")
  public UpdateResult updateMany(Map<?, ?> map, Map<?, ?> map1, UpdateOptions updateOptions) {
    JMongoUtil.doConversions((Map<String, Object>) map);
    JMongoUtil.doConversions((Map<String, Object>) map1);
    return nativeCollection.updateMany(new Document((Map<String, Object>) map), new Document((Map<String, Object>) map1), updateOptions);
  }

  public UpdateResult updateMany(String str, String str1, UpdateOptions updateOptions) {
    return nativeCollection.updateMany(JMongoUtil.parse(str), BsonDocument.parse(str1), updateOptions);
  }

  @SuppressWarnings("unchecked")
  public TDocument findOneAndDelete(Map<?, ?> map) {
    JMongoUtil.doConversions((Map<String, Object>) map);
    return nativeCollection.findOneAndDelete(new Document((Map<String, Object>) map));
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

  @SuppressWarnings("unchecked")
  public String createIndex(Map<?, ?> map) {
    JMongoUtil.doConversions((Map<String, Object>) map);
    return nativeCollection.createIndex(new Document((Map<String, Object>) map));
  }

  public String createIndex(String str, IndexOptions indexOptions) {
    return nativeCollection.createIndex(JMongoUtil.parse(str), indexOptions);
  }

  @SuppressWarnings("unchecked")
  public String createIndex(Map<?, ?> map, Map<?, ?> optionsMap) {
    JMongoUtil.doConversions((Map<String, Object>) map);
    return nativeCollection.createIndex(new Document((Map<String, Object>) map), new IndexOptionsFromMap(optionsMap));
  }

  //---------------------------------------------------------------

  public MongoNamespace getNamespace() {
    return nativeCollection.getNamespace();
  }

  public Class<TDocument> getDocumentClass() {
    return nativeCollection.getDocumentClass();
  }

  public CodecRegistry getCodecRegistry() {
    return nativeCollection.getCodecRegistry();
  }

  public ReadPreference getReadPreference() {
    return nativeCollection.getReadPreference();
  }

  public WriteConcern getWriteConcern() {
    return nativeCollection.getWriteConcern();
  }

  public <NewTDocument> JMongoCollection<TDocument> withDocumentClass(Class<?> aClass) {
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

  @SuppressWarnings("unchecked")
  public JAggregateIterable<TDocument> aggregate(Map<?, ?>... maps) {
    List<Bson> list = new ArrayList<>();
    for (Map<?, ?> map : maps) {
      JMongoUtil.doConversions((Map<String, Object>) map);
      list.add(new Document((Map<String, Object>) map));
    }
    return new JAggregateIterable<>(nativeCollection.aggregate(list));
  }

  public <TResult> AggregateIterable<TResult> aggregate(List<? extends Bson> bsons, Class<TResult> aClass) {
    return nativeCollection.aggregate(bsons, aClass);
  }

  public MapReduceIterable<TDocument> mapReduce(String s, String s1) {
    return nativeCollection.mapReduce(s, s1);
  }

  public <TResult> MapReduceIterable<TResult> mapReduce(String s, String s1, Class<TResult> aClass) {
    return nativeCollection.mapReduce(s, s1, aClass);
  }

  public BulkWriteResult bulkWrite(List<?> list) {
    //noinspection unchecked
    return nativeCollection.bulkWrite((List<? extends WriteModel<? extends TDocument>>) list);
  }


  public BulkWriteResult bulkWrite(List<?> list, BulkWriteOptions bulkWriteOptions) {
    //noinspection unchecked
    return nativeCollection.bulkWrite((List<? extends WriteModel<? extends TDocument>>) list, bulkWriteOptions);
  }


  public void insertOne(TDocument o) {
    nativeCollection.insertOne(o);
  }


  public void insertMany(List<?> list) {
    //noinspection unchecked
    nativeCollection.insertMany((List<? extends TDocument>) list);
  }


  public void insertMany(List<?> list, InsertManyOptions insertManyOptions) {
    //noinspection unchecked
    nativeCollection.insertMany((List<? extends TDocument>) list, insertManyOptions);
  }


  public DeleteResult deleteOne(Bson bson) {
    return nativeCollection.deleteOne(bson);
  }


  public DeleteResult deleteMany(Bson bson) {
    return nativeCollection.deleteMany(bson);
  }


  public UpdateResult replaceOne(Bson bson, TDocument o) {
    return nativeCollection.replaceOne(bson, o);
  }


  public UpdateResult replaceOne(Bson bson, TDocument o, UpdateOptions updateOptions) {
    //noinspection deprecation
    return nativeCollection.replaceOne(bson, o, updateOptions);
  }


  public UpdateResult updateOne(Bson bson, Bson bson1) {
    return nativeCollection.updateOne(bson, bson1);
  }


  public UpdateResult updateOne(Bson bson, Bson bson1, UpdateOptions updateOptions) {
    return nativeCollection.updateOne(bson, bson1, updateOptions);
  }


  public UpdateResult updateMany(Bson bson, Bson bson1) {
    return nativeCollection.updateMany(bson, bson1);
  }


  public UpdateResult updateMany(Bson bson, Bson bson1, UpdateOptions updateOptions) {
    return nativeCollection.updateMany(bson, bson1, updateOptions);
  }


  public TDocument findOneAndDelete(Bson bson) {
    return nativeCollection.findOneAndDelete(bson);
  }


  public TDocument findOneAndDelete(Bson bson, FindOneAndDeleteOptions findOneAndDeleteOptions) {
    return nativeCollection.findOneAndDelete(bson, findOneAndDeleteOptions);
  }


  public TDocument findOneAndReplace(Bson bson, TDocument o) {
    return nativeCollection.findOneAndReplace(bson, o);
  }


  public TDocument findOneAndReplace(Bson bson, TDocument o, FindOneAndReplaceOptions findOneAndReplaceOptions) {
    return nativeCollection.findOneAndReplace(bson, o, findOneAndReplaceOptions);
  }


  public TDocument findOneAndUpdate(Bson bson, Bson bson1) {
    return nativeCollection.findOneAndUpdate(bson, bson1);
  }


  public TDocument findOneAndUpdate(Bson bson, Bson bson1, FindOneAndUpdateOptions findOneAndUpdateOptions) {
    return nativeCollection.findOneAndUpdate(bson, bson1, findOneAndUpdateOptions);
  }


  public void drop() {
    nativeCollection.drop();
  }


  public String createIndex(Bson bson) {
    return nativeCollection.createIndex(bson);
  }


  public String createIndex(Bson bson, IndexOptions indexOptions) {
    return nativeCollection.createIndex(bson, indexOptions);
  }


  public List<String> createIndexes(List<IndexModel> indexModels) {
    return nativeCollection.createIndexes(indexModels);
  }


  public ListIndexesIterable<Document> listIndexes() {
    return nativeCollection.listIndexes();
  }


  public <TResult> ListIndexesIterable<TResult> listIndexes(Class<TResult> aClass) {
    return nativeCollection.listIndexes(aClass);
  }


  public void dropIndex(String s) {
    nativeCollection.dropIndex(s);
  }


  public void dropIndex(Bson bson) {
    nativeCollection.dropIndex(bson);
  }


  public void dropIndexes() {
    nativeCollection.dropIndexes();
  }


  public void renameCollection(String newName) {
    nativeCollection.renameCollection(new MongoNamespace(nativeCollection.getNamespace().getDatabaseName(), newName));
  }

  public void renameCollection(MongoNamespace mongoNamespace) {
    nativeCollection.renameCollection(mongoNamespace);
  }

  public void renameCollection(MongoNamespace mongoNamespace, RenameCollectionOptions renameCollectionOptions) {
    nativeCollection.renameCollection(mongoNamespace, renameCollectionOptions);
  }
}
