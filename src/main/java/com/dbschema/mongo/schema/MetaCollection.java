package com.dbschema.mongo.schema;

import com.mongodb.MongoQueryException;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.conversions.Bson;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MetaCollection extends MetaJson {
  private boolean isFirstDiscover = true;

  public final String db;
  public final List<MetaIndex> metaIndexes = new ArrayList<>();

  public MetaCollection(final MongoCollection<?> mongoCollection, final int fetchDocumentsForMeta) {
    super(null, mongoCollection.getNamespace().getCollectionName(), TYPE_MAP);
    db = mongoCollection.getNamespace().getDatabaseName();
    discoverCollectionSampleRecords(mongoCollection, fetchDocumentsForMeta);
    discoverIndexes(mongoCollection);

  }

  public MetaIndex createMetaIndex(String name, boolean pk, boolean unique) {
    MetaIndex index = new MetaIndex(this, name, pk, unique);
    metaIndexes.add(index);
    return index;
  }

  private void discoverCollectionSampleRecords(MongoCollection<?> mongoCollection, int iterations) {
    List<Bson> pipeline = List.of(Aggregates.sample(iterations));
    try (MongoCursor<?> cursor = mongoCollection.aggregate(pipeline).iterator()) {
      int iteration = 0;
      while (cursor.hasNext() && ++iteration <= iterations) {
        discoverMap(this, cursor.next(), true);
      }
    }
    catch (MongoQueryException e) {
      if (e.getErrorCode() == 13) return; // Authorized
      throw e;
    }
  }

  private void discoverMap(MetaJson parentMap, Object object, boolean updateMandatory) {
    if (object instanceof Map<?, ?> map) {
        for (Object key : map.keySet()) {
        final Object value = map.get(key);
        String type = (value != null ? value.getClass().getName() : "String");
        if (type.lastIndexOf('.') > 0) type = type.substring(type.lastIndexOf('.') + 1);
        if (value instanceof Map) {
          final MetaJson childrenMap = parentMap.createJsonMapField(key.toString(), isFirstDiscover);
          discoverMap(childrenMap, value, updateMandatory);
        }
        else if (value instanceof List<?> list) {
            if ((list.isEmpty() || isListOfDocuments(value))) {
            final MetaJson subDocument = parentMap.createJsonListField(key.toString(), isFirstDiscover);
            for (Object child : list) {
              discoverMap(subDocument, child, updateMandatory);
            }
          }
          else {
            parentMap.createField((String) key, "array", MetaJson.TYPE_ARRAY, isFirstDiscover);
          }
        }
        else {
          parentMap.createField((String) key, type, getJavaType(value), isFirstDiscover);
        }
      }
      if (updateMandatory) {
        for (MetaField field : parentMap.fields) {
          if (!map.containsKey(field.name)) {
            field.setMandatory(false);
          }
        }
      }
    }
    isFirstDiscover = false;
  }

  public int getJavaType(Object value) {
    if (value instanceof Integer) return java.sql.Types.INTEGER;
    else if (value instanceof Timestamp) return java.sql.Types.TIMESTAMP;
    else if (value instanceof Date) return java.sql.Types.DATE;
    else if (value instanceof Double) return java.sql.Types.DOUBLE;
    return java.sql.Types.VARCHAR;
  }


  private boolean isListOfDocuments(Object obj) {
    if (obj instanceof List<?> list) {
        for (Object val : list) {
        if (!(val instanceof Map)) return false;
      }
      return !list.isEmpty();
    }
    return false;
  }

  private static final String KEY_NAME = "name";
  private static final String KEY_UNIQUE = "unique";
  private static final String KEY_KEY = "key";

  private void discoverIndexes(MongoCollection<?> dbCollection) {
    ListIndexesIterable<?> iterable;
    try {
      iterable = dbCollection.listIndexes();
    }
    catch (Throwable ex) {
      System.err.println("Error listing indexes for " + dbCollection + "." + this + ". " + ex);
      return;
    }
    for (Object indexObject : iterable) {
      try {
        processIndex(dbCollection, indexObject);
      }
      catch (Throwable ex) {
        System.err.println("Error processing index " + indexObject + " of " + dbCollection + ". " + ex);
      }
    }
  }

  private void processIndex(MongoCollection<?> dbCollection, Object indexObject) {
    if (!(indexObject instanceof Map<?, ?> indexMap)) return;
    final String indexName = String.valueOf(indexMap.get(KEY_NAME));
    final boolean indexIsPk = "_id_".equals(indexName);
    final boolean indexIsUnique = Boolean.TRUE.equals(indexMap.get(KEY_UNIQUE));
    final Object columnsObj = indexMap.get(KEY_KEY);
    if (!(columnsObj instanceof Map<?, ?> columnsMap)) return;
    MetaIndex metaIndex = createMetaIndex(indexName, indexIsPk, indexIsUnique);
    for (Map.Entry<?, ?> fieldEntry : columnsMap.entrySet()) {
      String fieldPath = String.valueOf(fieldEntry.getKey());
      int direction = directionOf(fieldEntry.getValue());
      MetaField metaField = resolveIndexField(dbCollection, fieldPath);
      metaIndex.addColumn(new MetaIndexField(metaField, direction));
    }
  }

  private MetaField resolveIndexField(MongoCollection<?> dbCollection, String fieldPath) {
    MetaField field = findField(fieldPath);
    if (field != null) return field;
    field = fetchAndRegisterField(dbCollection, fieldPath);
    if (field != null) return field;
    return createStubField(fieldPath);
  }

  // 1/-1 for ascending/descending; for "text"/"2dsphere"/"hashed" there is no asc/desc semantics.
  private static int directionOf(Object value) {
    return value instanceof Number ? ((Number) value).intValue() : 0;
  }

  private MetaField fetchAndRegisterField(MongoCollection<?> coll, String fieldPath) {
    try (MongoCursor<?> cursor = coll
        .find(Filters.exists(fieldPath))
        .projection(Projections.include(fieldPath))
        .limit(1)
        .iterator()) {
      if (!cursor.hasNext()) return null;
      return registerFieldFromDoc(cursor.next(), fieldPath);
    }
    catch (MongoQueryException e) {
      if (e.getErrorCode() == 13) return null; // unauthorized to read
      throw e;
    }
  }

  private MetaField registerFieldFromDoc(Object docObject, String fieldPath) {
    if (!(docObject instanceof Map<?, ?> currentMap)) return null;
    String[] parts = fieldPath.split("\\.", -1);
    MetaJson parentNode = this;
    for (int i = 0; i < parts.length - 1; i++) {
      Object next = currentMap.get(parts[i]);
      if (!(next instanceof Map)) return null;
      parentNode = parentNode.createJsonMapField(parts[i], false);
      currentMap = (Map<?, ?>) next;
    }
    String leaf = parts[parts.length - 1];
    if (!currentMap.containsKey(leaf)) return null;
    discoverMap(parentNode, Collections.singletonMap(leaf, currentMap.get(leaf)), false);
    for (MetaField existing : parentNode.fields) {
      if (existing.name.equals(leaf)) return existing;
    }
    return null;
  }

  // Type defaults to VARCHAR; the index stays visible in JDBC metadata.
  private MetaField createStubField(String fieldPath) {
    String[] parts = fieldPath.split("\\.", -1);
    MetaJson parent = this;
    for (int i = 0; i < parts.length - 1; i++) {
      parent = parent.createJsonMapField(parts[i], false);
    }
    String leaf = parts[parts.length - 1];
    for (MetaField existing : parent.fields) {
      if (existing.name.equals(leaf)) return existing;
    }
    MetaField stub = new MetaField(parent, leaf, "String", java.sql.Types.VARCHAR);
    stub.setMandatory(false);
    parent.fields.add(stub);
    return stub;
  }


}
