package com.dbschema.mongo.nashorn;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.*;
import org.bson.Document;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dbschema.mongo.nashorn.JMongoUtil.toBson;
import static com.dbschema.mongo.nashorn.MapProcessor.proc;
import static com.dbschema.mongo.nashorn.MapProcessor.runProcessors;

/**
 * @author Liudmila Kornilova
 **/
public interface MapProcessors {

  List<MapProcessor<CreateCollectionOptions, ?>> CREATE_COLLECTION_OPTIONS = Arrays.asList(
      proc("capped",              Boolean.class, (v, o) -> o.capped(v)),
      proc("autoIndexId",         Boolean.class, (v, o) -> o.autoIndex(v)),
      proc("size",                Number.class,  (v, o) -> o.sizeInBytes(v.longValue())),
      proc("max",                 Number.class,  (v, o) -> o.maxDocuments(v.longValue()))
//      proc("storageEngine",       Map.class,     (v, o) -> o), // todo
//      proc("validator",           Map.class,     (v, o) -> o), // todo
//      proc("validationLevel",     String.class,  (v, o) -> o), // todo
//      proc("validationAction",    String.class,  (v, o) -> o), // todo
//      proc("indexOptionDefaults", Map.class,     (v, o) -> o), // todo
//      proc("viewOn",              String.class,  (v, o) -> o), // todo
//      proc("pipeline",            String.class,  (v, o) -> o), // todo
//      proc("collation",           Map.class,     (v, o) -> o), // todo
//      proc("writeConcern",        Map.class,     (v, o) -> o) // todo
  );

  List<MapProcessor<Collation.Builder, ?>> COLLATION = Arrays.asList(
      proc("locale",          String.class,  (v, o) -> o.locale(v)),
      proc("caseLevel",       Boolean.class, (v, o) -> o.caseLevel(v)),
      proc("caseFirst",       String.class,  (v, o) -> o.collationCaseFirst(CollationCaseFirst.fromString(v))),
      proc("strength",        Number.class,  (v, o) -> o.collationStrength(CollationStrength.fromInt(v.intValue()))),
      proc("numericOrdering", Boolean.class, (v, o) -> o.numericOrdering(v)),
      proc("alternate",       String.class,  (v, o) -> o.collationAlternate(CollationAlternate.fromString(v))),
      proc("maxVariable",     String.class,  (v, o) -> o.collationMaxVariable(CollationMaxVariable.fromString(v))),
      proc("backwards",       Boolean.class, (v, o) -> o.backwards(v))
  );

  List<MapProcessor<CreateViewOptions, ?>> CREATE_VIEW_OPTIONS = Collections.singletonList(
      proc("collation", Map.class, (map, o) -> o.collation(MapProcessor.runProcessors(Collation.builder(), COLLATION, map).build()))
  );

  List<MapProcessor<BulkWriteOptions, ?>> BULK_WRITE_OPTIONS = Arrays.asList(
      proc("ordered",      Boolean.class, (v, o) -> o.ordered(v))
//      proc("writeConcern", Boolean.class, (v, o) -> o)
  );

  List<MapProcessor<EstimatedDocumentCountOptions, ?>> ESTIMATED_DOCUMENT_OPTIONS = Collections.singletonList(
      proc("maxTimeMS", Number.class, (v, o) -> o.maxTime(v.longValue(), TimeUnit.MILLISECONDS))
  );

  List<MapProcessor<CountOptions, ?>> COUNT_OPTIONS = Arrays.asList(
      proc("limit",       Number.class, (v, o) -> o.limit(v.intValue())),
      proc("skip",        Number.class, (v, o) -> o.skip(v.intValue())),
      proc("hint",        Map.class,    (v, o) -> o.hint(toBson(v))),
      proc("maxTimeMS",   Number.class, (v, o) -> o.maxTime(v.longValue(), TimeUnit.MILLISECONDS)),
//      proc("readConcern", String.class, (v, o) -> o),
      proc("collation",   Map.class,    (v, o) -> o.collation(MapProcessor.runProcessors(Collation.builder(), COLLATION, v).build()))
  );

  List<MapProcessor<AggregateIterable<Document>, ?>> AGGREGATE_ITERABLE_CURSOR = Collections.singletonList(
      proc("batchSize", Number.class, (v, o) -> o.batchSize(v.intValue()))
  );

  List<MapProcessor<AggregateIterable<Document>, ?>> AGGREGATE_ITERABLE = Arrays.asList(
//      proc("explain",                  Boolean.class, (v, o) -> o),
      proc("allowDiskUse",             Boolean.class, (v, o) -> o.allowDiskUse(v)),
      proc("cursor",                   Map.class,     (v, o) -> MapProcessor.runProcessors(o, AGGREGATE_ITERABLE_CURSOR, v)),
      proc("maxTimeMS",                Number.class,  (v, o) -> o.maxTime(v.longValue(), TimeUnit.MILLISECONDS)),
      proc("bypassDocumentValidation", Boolean.class, (v, o) -> o.bypassDocumentValidation(v)),
//      proc("readConcern",              Map.class,     (v, o) -> o),
      proc("collation",                Map.class,     (v, o) -> o.collation(MapProcessor.runProcessors(Collation.builder(), COLLATION, v).build())),
      proc("hint",                     Map.class,     (v, o) -> o.hint(toBson(v))),
      proc("comment",                  String.class,  (v, o) -> o.comment(v))
//      proc("writeConcern",             String.class,  (v, o) -> o)
  );

  List<MapProcessor<IndexOptions, ?>> INDEX_OPTIONS = Arrays.asList(
      proc("background",              Boolean.class, (v, o) -> o.background(v)),
      proc("unique",                  Boolean.class, (v, o) -> o.unique(v)),
      proc("name",                    String.class,  (v, o) -> o.name(v)),
      proc("partialFilterExpression", Map.class,     (v, o) -> o.partialFilterExpression(toBson(v))),
      proc("sparse",                  Boolean.class, (v, o) -> o.sparse(v)),
      proc("expireAfterSeconds",      Number.class,  (v, o) -> o.expireAfter(v.longValue(), TimeUnit.SECONDS)),
      proc("storageEngine",           Map.class,     (v, o) -> o.storageEngine(toBson(v))),
      proc("collation",               Map.class,     (v, o) -> o.collation(MapProcessor.runProcessors(Collation.builder(), COLLATION, v).build())),
      proc("weights",                 Map.class,     (v, o) -> o.weights(toBson(v))),
      proc("default_language",        String.class,  (v, o) -> o.defaultLanguage(v)),
      proc("language_override",       String.class,  (v, o) -> o.languageOverride(v)),
      proc("textIndexVersion",        Number.class,  (v, o) -> o.textVersion(v.intValue())),
      proc("2dsphereIndexVersion",    Number.class,  (v, o) -> o.sphereVersion(v.intValue())),
      proc("bits",                    Number.class,  (v, o) -> o.bits(v.intValue())),
      proc("min",                     Number.class,  (v, o) -> o.min(v.doubleValue())),
      proc("max",                     Number.class,  (v, o) -> o.max(v.doubleValue())),
      proc("bucketSize",              Number.class,  (v, o) -> o.bucketSize(v.doubleValue())),
      proc("wildcardProjection",      Map.class,     (v, o) -> o.wildcardProjection(toBson(v)))
  );

  List<MapProcessor<Document, ?>> STATS_OPTIONS = Collections.singletonList(
      proc("scale", Number.class, (v, o) -> o.append("scale", v.intValue()))
  );

  List<MapProcessor<DeleteOptions, ?>> DELETE_OPTIONS = Collections.singletonList(
      proc("collation", Map.class, (v, o) -> o.collation(MapProcessor.runProcessors(Collation.builder(), COLLATION, v).build()))
  );

  List<MapProcessor<FindOneAndDeleteOptions, ?>> FIND_ONE_AND_DELETE_OPTIONS = Arrays.asList(
      proc("projection", Map.class, (v, o) -> o.projection(toBson(v))),
      proc("sort",       Map.class, (v, o) -> o.sort(toBson(v))),
      proc("maxTimeMS",  Number.class,   (v, o) -> o.maxTime(v.longValue(), TimeUnit.MILLISECONDS)),
      proc("collation",  Map.class, (v, o) -> o.collation(MapProcessor.runProcessors(Collation.builder(), COLLATION, v).build()))
  );

  List<MapProcessor<FindOneAndReplaceOptions, ?>> FIND_ONE_AND_REPLACE_OPTIONS = Arrays.asList(
      proc("projection",        Map.class,     (v, o) -> o.projection(toBson(v))),
      proc("sort",              Map.class,     (v, o) -> o.sort(toBson(v))),
      proc("maxTimeMS",         Number.class,  (v, o) -> o.maxTime(v.longValue(), TimeUnit.MILLISECONDS)),
      proc("upsert",            Boolean.class, (v, o) -> o.upsert(v)),
      proc("returnNewDocument", Boolean.class, (v, o) -> o.returnDocument(v ? ReturnDocument.AFTER : ReturnDocument.BEFORE)),
      proc("collation",         Map.class,     (v, o) -> o.collation(MapProcessor.runProcessors(Collation.builder(), COLLATION, v).build()))
  );

  List<MapProcessor<FindOneAndUpdateOptions, ?>> FIND_ONE_AND_UPDATE_OPTIONS = Arrays.asList(
      proc("projection",        Map.class,     (v, o) -> o.projection(toBson(v))),
      proc("sort",              Map.class,     (v, o) -> o.sort(toBson(v))),
      proc("maxTimeMS",         Number.class,  (v, o) -> o.maxTime(v.longValue(), TimeUnit.MILLISECONDS)),
      proc("upsert",            Boolean.class, (v, o) -> o.upsert(v)),
      proc("returnNewDocument", Boolean.class, (v, o) -> o.returnDocument(v ? ReturnDocument.AFTER : ReturnDocument.BEFORE)),
      proc("collation",         Map.class,     (v, o) -> o.collation(MapProcessor.runProcessors(Collation.builder(), COLLATION, v).build()))
  );

  List<MapProcessor<InsertOneOptions, ?>> INSERT_ONE_OPTIONS = Arrays.asList(
//      proc("writeConcern", Map.class,     (v, o) -> o),
//      proc("ordered",      Boolean.class, (v, o) -> o)
  );

  List<MapProcessor<InsertManyOptions, ?>> INSERT_MANY_OPTIONS = Arrays.asList(
//      proc("writeConcern", Map.class,     (v, o) -> o),
//      proc("ordered",      Boolean.class, (v, o) -> o)
  );

  List<MapProcessor<ReplaceOptions, ?>> REPLACE_OPTIONS = Arrays.asList(
      proc("upsert",       Boolean.class, (v, o) -> o.upsert(v)),
      proc("writeConcern", Map.class,     (v, o) -> o),
      proc("collation",    Map.class,     (v, o) -> o.collation(runProcessors(Collation.builder(), COLLATION, v).build()))
//      proc("hint", Map.class,     (v, o) -> o),
//      proc("hint", String.class,     (v, o) -> o)
  );

  List<MapProcessor<UpdateOptions, ?>> UPDATE_OPTIONS = Arrays.asList(
      proc("upsert",       Boolean.class, (v, o) -> o.upsert(v)),
//      proc("multi",        Boolean.class, (v, o) -> o),
//      proc("writeConcern", Map.class, (v, o) -> o),
      proc("collation",    Map.class, (v, o) -> o.collation(runProcessors(Collation.builder(), COLLATION, v).build()))
//      proc("arrayFilters", List.class, (v, o) -> o),
//      proc("hint",         Map.class, (v, o) -> o),
//      proc("hint",         String.class, (v, o) -> o),
  );
}
