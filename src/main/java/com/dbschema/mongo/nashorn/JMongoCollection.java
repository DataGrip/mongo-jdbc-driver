package com.dbschema.mongo.nashorn;


import com.dbschema.mongo.Util;
import com.dbschema.mongo.resultSet.ListResultSet;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoNamespace;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import jdk.nashorn.api.scripting.AbstractJSObject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.util.*;

import static com.dbschema.mongo.Util.*;
import static com.dbschema.mongo.nashorn.JMongoDatabase.runCommand;
import static com.dbschema.mongo.nashorn.JMongoUtil.toBson;
import static com.dbschema.mongo.nashorn.MapProcessor.runProcessors;
import static com.dbschema.mongo.nashorn.MapProcessors.*;
import static com.dbschema.mongo.nashorn.MemberFunction.*;

public class JMongoCollection extends AbstractJSObject {
  private final MongoCollection<Document> nativeCollection;
  private final String name;
  private final MongoDatabase mongoDatabase;
  private final MongoJSObject delegate;

  public JMongoCollection(MongoCollection<Document> nativeCollection, String name, MongoDatabase mongoDatabase) {
    this.nativeCollection = nativeCollection;
    this.name = name;
    this.mongoDatabase = mongoDatabase;
    delegate = new MongoJSObject(Arrays.asList(
        func("aggregate",                     this::aggregate, List.class),
        func("aggregate",                     this::aggregate, List.class, Map.class),
        vararg("aggregate",                   this::aggregateVararg, Map.class),
        func("bulkWrite",                     this::bulkWrite, List.class),
        func("bulkWrite",                     this::bulkWrite, List.class, Map.class),
        func("copyTo",                        n -> new JAggregateIterable<>(nativeCollection.aggregate(Collections.singletonList(new Document("$out", n)))), String.class),
        func("count",                         nativeCollection::count),
        func("count",                         this::count, Map.class),
        func("count",                         this::count, Map.class, Map.class),
        func("countDocuments",                nativeCollection::countDocuments),
        func("countDocuments",                this::countDocuments, Map.class),
        func("countDocuments",                this::countDocuments, Map.class, Map.class),
        func("estimatedDocumentCount",        nativeCollection::estimatedDocumentCount),
        func("estimatedDocumentCount",        this::estimatedDocumentCount, Map.class),
        func("createIndex",                   m -> nativeCollection.createIndex(toBson(m)), Map.class),
        func("createIndex",                   this::createIndex, Map.class, Map.class),
        func("createIndexes",                 this::createIndexes, List.class),
        func("createIndexes",                 this::createIndexes, List.class, Map.class),
        func("dataSize",                      () -> getLongValueFromStats("size")),
        func("delete",                        this::deleteOne, Map.class),
        func("delete",                        this::deleteOne, Map.class, Map.class),
        func("deleteOne",                     this::deleteOne, Map.class),
        func("deleteOne",                     this::deleteOne, Map.class, Map.class),
        func("deleteMany",                    this::deleteMany, Map.class),
        func("deleteMany",                    this::deleteMany, Map.class, Map.class),
        notImplemented("distinct",            ObjectKind.COLLECTION),
        voidFunc("drop",                      nativeCollection::drop),
        voidFunc("drop",                      m -> { throw new UnsupportedOperationException("Method db.collection.drop(<document>) is not implemented"); }, Map.class),
        voidFunc("dropIndex",                 nativeCollection::dropIndex, String.class),
        voidFunc("dropIndex",                 m -> nativeCollection.dropIndex(toBson(m)), Map.class),
        voidFunc("dropIndexes",               nativeCollection::dropIndexes),
        voidFunc("dropIndexes",               nativeCollection::dropIndex, String.class),
        voidFunc("dropIndexes",               l -> { throw new UnsupportedOperationException("Method db.collection.dropIndexes(<array>) is not implemented"); }, List.class),
        voidFunc("dropIndexes",               m -> nativeCollection.dropIndex(toBson(m)), Map.class),
        notImplemented("ensureIndex",         ObjectKind.COLLECTION),
        notImplemented("explain",             ObjectKind.COLLECTION),
        func("find",                          this::find),
        func("find",                          this::find, Map.class),
        func("find",                          this::find, Map.class, Map.class),
        notImplemented("findAndModify",       ObjectKind.COLLECTION),
        notImplemented("findOne",             ObjectKind.COLLECTION),
        func("findOneAndDelete",              m -> nativeCollection.findOneAndDelete(toBson(m)), Map.class),
        func("findOneAndDelete",              this::findOneAndDelete, Map.class, Map.class),
        func("findOneAndReplace",             (m1, m2) -> nativeCollection.findOneAndReplace(toBson(m1), toBson(m2)), Map.class, Map.class),
        func("findOneAndReplace",             this::findOneAndReplace, Map.class, Map.class, Map.class),
        func("findOneAndUpdate",              (m1, m2) -> nativeCollection.findOneAndReplace(toBson(m1), toBson(m2)), Map.class, Map.class),
        func("findOneAndUpdate",              this::findOneAndUpdate, Map.class, Map.class, Map.class),
        func("findOneAndUpdate",              (m, l) -> { throw new UnsupportedOperationException("Method db.collection.findOneAndUpdate(<document>, <array>) is not implemented"); }, Map.class, List.class),
        func("findOneAndUpdate",              (m1, l, m2) -> { throw new UnsupportedOperationException("Method db.collection.findOneAndUpdate(<document>, <array>, <document>) is not implemented"); }, Map.class, List.class, Map.class),
        func("getIndexes",                    nativeCollection::listIndexes),
        notImplemented("getShardDistribution",ObjectKind.COLLECTION),
        notImplemented("getShardVersion",     ObjectKind.COLLECTION),
        voidFunc("insert",                    this::insertMany, List.class),
        voidFunc("insert",                    this::insertMany, List.class, Map.class),
        voidFunc("insert",                    this::insertOne, Map.class),
        voidFunc("insert",                    this::insertOne, Map.class, Map.class),
        voidFunc("insertOne",                 this::insertOne, Map.class),
        voidFunc("insertOne",                 this::insertOne, Map.class, Map.class),
        voidFunc("insertMany",                this::insertMany, List.class),
        voidFunc("insertMany",                this::insertMany, List.class, Map.class),
        notImplemented("isCapped",            ObjectKind.COLLECTION),
        notImplemented("latencyStats",        ObjectKind.COLLECTION),
        func("mapReduce",                     nativeCollection::mapReduce, String.class, String.class),
        func("mapReduce",                     (f1, f2) -> nativeCollection.mapReduce(f1.getText(), f2.getText()), JSFunction.class, JSFunction.class),
        func("mapReduce",                     (f1, f2, m) -> { throw new UnsupportedOperationException("Method db.collection.mapReduce(<function>, <function>, <document>) is not implemented"); }, JSFunction.class, JSFunction.class, Map.class),
        notImplemented("reIndex",             ObjectKind.COLLECTION),
        notImplemented("remove",              ObjectKind.COLLECTION),
        voidFunc("renameCollection",          this::renameCollection, String.class),
        voidFunc("renameCollection",          this::renameCollection, String.class, Boolean.class),
        func("replaceOne",                    (m1, m2) -> toDoc(nativeCollection.replaceOne(toBson(m1), toBson(m2))), Map.class, Map.class),
        func("replaceOne",                    (m1, m2, m3) -> toDoc(nativeCollection.replaceOne(toBson(m1), toBson(m2), runProcessors(new ReplaceOptions(), REPLACE_OPTIONS, m3))), Map.class, Map.class, Map.class),
        notImplemented("save",                ObjectKind.COLLECTION),
        func("stats",                         () -> runCommand(mongoDatabase, new Document("collStats", name))),
        func("stats",                         this::stats, Map.class),
        func("storageSize",                   () -> getLongValueFromStats("storageSize")),
        func("totalIndexSize",                () -> getLongValueFromStats("totalIndexSize")),
        func("totalSize",                     () -> getLongValueFromStats("storageSize") + getLongValueFromStats("totalIndexSize")),
        func("update",                        this::updateOne, Map.class, Map.class),
        func("update",                        this::updateOne, Map.class, Map.class, Map.class),
        func("updateOne",                     this::updateOne, Map.class, Map.class),
        func("updateOne",                     this::updateOne, Map.class, Map.class, Map.class),
        func("updateMany",                    this::updateMany, Map.class, Map.class),
        func("updateMany",                    this::updateMany, Map.class, Map.class, Map.class),
        notImplemented("watch",               ObjectKind.COLLECTION), // todo
        notImplemented("validate",            ObjectKind.COLLECTION)));
  }

  @NotNull
  private Document toDoc(@NotNull UpdateResult res) {
    return new Document("acknowledged", res.wasAcknowledged())
        .append("matchedCount", res.getMatchedCount())
        .append("modifiedCount", res.getModifiedCount());
  }

  @Override
  public boolean hasMember(String name) {
    return delegate.hasMember(name);
  }

  @Override
  public Object call(Object thiz, Object... args) {
    throw new UnsupportedOperationException("Method not found: db." + name);
  }

  private Document stats(Map<?, ?> options) {
    return runCommand(mongoDatabase, runProcessors(new Document("collStats", name), STATS_OPTIONS, options));
  }

  private long getLongValueFromStats(String fieldName) {
    try {
      Document res = mongoDatabase.runCommand(new Document("collStats", this.name));
      if (!res.containsKey(fieldName))
        throw new IllegalStateException("Result of command 'collStats' does not contain field '" + fieldName + "' " + res.toString());
      Object v = res.get(fieldName);
      if (!(v instanceof Number)) throw new IllegalStateException("Value is not a number : " + v);
      return ((Number) v).longValue();
    }
    catch (MongoCommandException e) {
      throw new IllegalStateException("Could not run command 'collStats'. Exception: " + toDocument(e.getResponse()));
    }
  }

  public Document bulkWrite(List<?> operations) {
    return bulkWrite(operations, null);
  }

  public Document bulkWrite(List<?> operations, @Nullable Map<?, ?> options) {
    ArrayList<WriteModel<Document>> requests = new ArrayList<>();
    for (Object operation : operations) {
      if (!(operation instanceof Map<?, ?>)) return result("All operations must be documents");
      Document doc = toBson(((Map<?, ?>) operation));
      WriteModel<Document> model = getWriteModel(doc);
      if (model == null) return result("Unsupported operation: " + doc.toString());
      requests.add(model);
    }
    return toDoc(options == null ?
                 nativeCollection.bulkWrite(requests) :
                 nativeCollection.bulkWrite(requests, runProcessors(new BulkWriteOptions(), BULK_WRITE_OPTIONS, options)));
  }

  @NotNull
  private Document toDoc(@NotNull BulkWriteResult res) {
    Document upsertsDoc = new Document();
    for (BulkWriteUpsert upsert : res.getUpserts()) {
      upsertsDoc.append(Integer.toString(upsert.getIndex()), decode(upsert.getId()));
    }
    return new Document("acknowledged", res.wasAcknowledged())
        .append("deletedCount", res.getDeletedCount())
        .append("insertedCount", res.getInsertedCount())
        .append("matchedCount", res.getMatchedCount())
        .append("upsertedCount", res.getUpserts().size())
        .append("insertedIds", upsertsDoc);
  }

  private Document toDoc(@NotNull DeleteResult res) {
    return new Document("acknowledged", res.wasAcknowledged())
        .append("deletedCount", res.getDeletedCount());
  }

  @NotNull
  private static Document result(@NotNull String message) {
    return new Document("Result", message);
  }

  @Nullable
  private WriteModel<Document> getWriteModel(Document model) {
    if (model.keySet().size() != 1) return null;
    String key = model.keySet().iterator().next();
    Document innerDoc = tryCast(model.get(key), Document.class);
    if (innerDoc == null) return null;
    switch (key) {
      case "insertOne":
        Document doc = tryCast(innerDoc.get("document"), Document.class);
        return doc == null ? null : new InsertOneModel<>(doc);
      case "deleteOne":
        Document filter = tryCast(innerDoc.get("filter"), Document.class);
        if (filter == null) return null;
        Document collation = tryCast(innerDoc.get("collation"), Document.class);
        if (collation == null) return new DeleteOneModel<>(filter);
        return new DeleteOneModel<>(filter, new DeleteOptions().collation(runProcessors(Collation.builder(), COLLATION, collation).build()));
      case "deleteMany":
      case "updateOne":
      case "updateMany":
      case "replaceOne":
    }
    return null;
  }

  @Override
  public Object getMember(String name) {
    AbstractJSObject member = delegate.getMember(name);
    return member != null ? member : getSubCollection(name);
  }

  private JMongoCollection getSubCollection(String name) {
    String newName = this.name + "." + name;
    return new JMongoCollection(mongoDatabase.getCollection(newName), newName, mongoDatabase);
  }

  public void insertOne(Map<?, ?> map) {
    nativeCollection.insertOne(toBson(map));
  }

  public void insertOne(Map<?, ?> map, Map<?, ?> options) {
    nativeCollection.insertOne(toBson(map), runProcessors(new InsertOneOptions(), INSERT_ONE_OPTIONS, options));
  }

  public void insertMany(List<?> docs) {
    nativeCollection.insertMany(toDocs(docs));
  }

  public void insertMany(List<?> docs, Map<?, ?> options) {
    nativeCollection.insertMany(toDocs(docs), runProcessors(new InsertManyOptions(), INSERT_MANY_OPTIONS, options));
  }

  private ArrayList<Document> toDocs(List<?> docs) {
    ArrayList<Document> documents = new ArrayList<>();
    for (Object doc : docs) {
      if (!(doc instanceof Map<?, ?>)) throw new IllegalArgumentException("All elements must be documents: " + doc);
      documents.add(toBson((Map<?, ?>) doc));
    }
    return documents;
  }

  public long count(Map<?, ?> find) {
    //noinspection deprecation
    return nativeCollection.count(toBson(find));
  }

  public long count(Map<?, ?> find, Map<?, ?> options) {
    //noinspection deprecation
    return nativeCollection.count(toBson(find), runProcessors(new CountOptions(), COUNT_OPTIONS, options));
  }

  public long countDocuments(Map<?, ?> find) {
    return nativeCollection.countDocuments(toBson(find));
  }

  public long countDocuments(Map<?, ?> find, Map<?, ?> options) {
    return nativeCollection.countDocuments(toBson(find), runProcessors(new CountOptions(), COUNT_OPTIONS, options));
  }

  public long estimatedDocumentCount(Map<?, ?> options) {
    return nativeCollection.estimatedDocumentCount(runProcessors(new EstimatedDocumentCountOptions(), ESTIMATED_DOCUMENT_OPTIONS, options));
  }

  public JFindIterable<Document> find(Map<?, ?> map) {
    return new JFindIterable<>(nativeCollection.find(toBson(map)));
  }

  public JFindIterable<Document> find(Map<?, ?> map, Map<?, ?> proj) {
    return new JFindIterable<>(nativeCollection.find(toBson(map)).projection(toBson(proj)));
  }

  public Document deleteOne(Map<?, ?> filter) {
    return toDoc(nativeCollection.deleteOne(toBson(filter)));
  }

  public Document deleteOne(Map<?, ?> filter, Map<?, ?> options) {
    DeleteResult res = nativeCollection.deleteOne(toBson(filter), runProcessors(new DeleteOptions(), DELETE_OPTIONS, options));
    return toDoc(res);
  }

  public Document deleteMany(Map<?, ?> filter) {
    return toDoc(nativeCollection.deleteMany(toBson(filter)));
  }

  public Document deleteMany(Map<?, ?> filter, Map<?, ?> options) {
    DeleteResult res = nativeCollection.deleteMany(toBson(filter), runProcessors(new DeleteOptions(), DELETE_OPTIONS, options));
    return toDoc(res);
  }

  public Document updateOne(Map<?, ?> query, Map<?, ?> update) {
    return toDoc(nativeCollection.updateOne(toBson(query), toBson(update)));
  }

  public Document updateOne(Map<?, ?> query, Map<?, ?> update, Map<?, ?> options) {
    return toDoc(nativeCollection.updateOne(toBson(query), toBson(update), runProcessors(new UpdateOptions(), UPDATE_OPTIONS, options)));
  }

  public Document updateMany(Map<?, ?> filter, Map<?, ?> update) {
    return toDoc(nativeCollection.updateMany(toBson(filter), toBson(update)));
  }

  public Document updateMany(Map<?, ?> filter, Map<?, ?> update, Map<?, ?> options) {
    return toDoc(nativeCollection.updateMany(toBson(filter), toBson(update), runProcessors(new UpdateOptions(), UPDATE_OPTIONS, options)));
  }

  private Document findOneAndDelete(Map<?, ?> filter, Map<?, ?> options) {
    return nativeCollection.findOneAndDelete(toBson(filter), runProcessors(new FindOneAndDeleteOptions(), FIND_ONE_AND_DELETE_OPTIONS, options));
  }

  private Document findOneAndReplace(Map<?, ?> filter, Map<?, ?> replacement, Map<?, ?> options) {
    return nativeCollection.findOneAndReplace(toBson(filter), toBson(replacement), runProcessors(new FindOneAndReplaceOptions(), FIND_ONE_AND_REPLACE_OPTIONS, options));
  }

  private Document findOneAndUpdate(Map<?, ?> filter, Map<?, ?> replacement, Map<?, ?> options) {
    return nativeCollection.findOneAndUpdate(toBson(filter), toBson(replacement), runProcessors(new FindOneAndUpdateOptions(), FIND_ONE_AND_UPDATE_OPTIONS, options));
  }

  public String createIndex(Map<?, ?> map, Map<?, ?> options) {
    return nativeCollection.createIndex(toBson(map), runProcessors(new IndexOptions(), INDEX_OPTIONS, options));
  }

  public ResultSet createIndexes(List<?> keyPatterns, Map<?, ?> options) {
    IndexOptions indexOptions = runProcessors(new IndexOptions(), INDEX_OPTIONS, options);
    List<IndexModel> models = new ArrayList<>();
    for (Object keyPattern : keyPatterns) {
      if (!(keyPattern instanceof Map<?, ?>)) throw new IllegalArgumentException("All key patterns must be documents: " + keyPattern);
      models.add(new IndexModel(toBson((Map<?, ?>) keyPattern), indexOptions));
    }
    return resultSet(nativeCollection.createIndexes(models));
  }

  public ResultSet createIndexes(List<?> keyPatterns) {
    List<IndexModel> models = new ArrayList<>();
    for (Object keyPattern : keyPatterns) {
      if (!(keyPattern instanceof Map<?, ?>)) throw new IllegalArgumentException("All key patterns must be documents: " + keyPattern);
      models.add(new IndexModel(toBson((Map<?, ?>) keyPattern)));
    }
    return resultSet(nativeCollection.createIndexes(models));
  }

  private ResultSet resultSet(List<?> values) {
    List<Object[]> res = Util.map(values, v -> new Object[]{v});
    return new ListResultSet(res, new String[] {"result"});
  }

  //---------------------------------------------------------------

  public JFindIterable<Document> find() {
    return new JFindIterable<>(nativeCollection.find());
  }

  public <TResult> JFindIterable<TResult> find(Class<TResult> aClass) {
    return new JFindIterable<>(nativeCollection.find(aClass));
  }

  public JFindIterable<Document> find(Bson bson) {
    return new JFindIterable<>(nativeCollection.find(bson));
  }

  public <TResult> JFindIterable<TResult> find(Bson bson, Class<TResult> aClass) {
    return new JFindIterable<>(nativeCollection.find(bson, aClass));
  }

  public JAggregateIterable<Document> aggregateVararg(@SuppressWarnings("rawtypes") List<Map> pipeline) {
    List<Bson> convertedPipe = new ArrayList<>();
    for (Map<?, ?> pipe : pipeline) {
      convertedPipe.add(toBson(pipe));
    }
    return new JAggregateIterable<>(nativeCollection.aggregate(convertedPipe));
  }

  public JAggregateIterable<Document> aggregate(List<?> pipeline, Map<?, ?> options) {
    List<Bson> convertedPipe = new ArrayList<>(pipeline.size());
    for (Object pipe : pipeline) {
      if (pipe instanceof Map<?, ?>) convertedPipe.add(toBson((Map<?, ?>) pipe));
    }
    AggregateIterable<Document> aggregateIterable = nativeCollection.aggregate(convertedPipe);
    return new JAggregateIterable<>(runProcessors(aggregateIterable, AGGREGATE_ITERABLE, options));
  }

  public JAggregateIterable<Document> aggregate(List<?> pipeline) {
    List<Bson> convertedPipe = new ArrayList<>(pipeline.size());
    for (Object pipe : pipeline) {
      if (pipe instanceof Map<?, ?>) convertedPipe.add(toBson((Map<?, ?>) pipe));
    }
    return new JAggregateIterable<>(nativeCollection.aggregate(convertedPipe));
  }

  public void insertOne(Document o) {
    nativeCollection.insertOne(o);
  }

  public void replaceOne(Bson bson, Document o, UpdateOptions updateOptions) {
    //noinspection deprecation
    nativeCollection.replaceOne(bson, o, updateOptions);
  }

  public ListIndexesIterable<Document> listIndexes() {
    return nativeCollection.listIndexes();
  }

  public void renameCollection(String newName) {
    nativeCollection.renameCollection(new MongoNamespace(nativeCollection.getNamespace().getDatabaseName(), newName));
  }

  public void renameCollection(String newName, Boolean dropTarget) {
    nativeCollection.renameCollection(new MongoNamespace(nativeCollection.getNamespace().getDatabaseName(), newName), new RenameCollectionOptions().dropTarget(true));
  }
}
