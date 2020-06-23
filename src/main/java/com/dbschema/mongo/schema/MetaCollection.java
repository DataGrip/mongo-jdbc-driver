package com.dbschema.mongo.schema;

import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;

import java.sql.Timestamp;
import java.util.ArrayList;
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
    discoverCollectionFirstRecords(mongoCollection, fetchDocumentsForMeta);
    discoverIndexes(mongoCollection);

  }

  public MetaIndex createMetaIndex(String name, boolean pk, boolean unique) {
    MetaIndex index = new MetaIndex(this, name, "_id_".endsWith(name), false);
    metaIndexes.add(index);
    return index;
  }


  private void discoverCollectionFirstRecords(MongoCollection<?> mongoCollection, int iterations) {
    MongoCursor<?> cursor = mongoCollection.find().iterator();
    int iteration = 0;
    while (cursor.hasNext() && ++iteration <= iterations) {
      discoverMap(this, cursor.next());
    }
    cursor.close();
  }

  private void discoverCollectionRandomRecords(MongoCollection<Document> mongoCollection, int iterations) {
    int skip = 10, i = 0;
    FindIterable<Document> jFindIterable = mongoCollection.find(); // .limit(-1)
    while (i++ < iterations) {
      final MongoCursor<?> crs = jFindIterable.iterator();
      while (i++ < iterations && crs.hasNext()) {
        discoverMap(this, crs.next());
      }
      jFindIterable = jFindIterable.skip(skip);
      skip = skip * 2;
    }
  }

  private void discoverMap(MetaJson parentMap, Object object) {
    if (object instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) object;
      for (Object key : map.keySet()) {
        final Object value = map.get(key);
        String type = (value != null ? value.getClass().getName() : "String");
        if (type.lastIndexOf('.') > 0) type = type.substring(type.lastIndexOf('.') + 1);
        if (value instanceof Map) {
          final MetaJson childrenMap = parentMap.createJsonMapField(key.toString(), isFirstDiscover);
          discoverMap(childrenMap, value);
        }
        else if (value instanceof List) {
          final List<?> list = (List<?>) value;
          if ((list.isEmpty() || isListOfDocuments(value))) {
            final MetaJson subDocument = parentMap.createJsonListField(key.toString(), isFirstDiscover);
            for (Object child : (List<?>) value) {
              discoverMap(subDocument, child);
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
      for (MetaField field : parentMap.fields) {
        if (!map.containsKey(field.name)) {
          field.setMandatory(false);
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
    if (obj instanceof List) {
      List<?> list = (List<?>) obj;
      for (Object val : list) {
        if (!(val instanceof Map)) return false;
      }
      return list.size() > 0;
    }
    return false;
  }

  private static final String KEY_NAME = "name";
  private static final String KEY_UNIQUE = "unique";
  private static final String KEY_KEY = "key";

  private void discoverIndexes(MongoCollection<?> dbCollection) {
    try {
      ListIndexesIterable<?> iterable = dbCollection.listIndexes();
      for (Object indexObject : iterable) {
        if (indexObject instanceof Map) {
          Map<?, ?> indexMap = (Map<?, ?>) indexObject;
          final String indexName = String.valueOf(indexMap.get(KEY_NAME));
          final boolean indexIsPk = "_id_".endsWith(indexName);
          final boolean indexIsUnique = Boolean.TRUE.equals(indexMap.get(KEY_UNIQUE));
          final Object columnsObj = indexMap.get(KEY_KEY);
          if (columnsObj instanceof Map) {
            final Map<?, ?> columnsMap = (Map<?, ?>) columnsObj;
            MetaIndex metaIndex = createMetaIndex(indexName, indexIsPk, indexIsUnique);
            for (Object fieldNameObj : columnsMap.keySet()) {
              final MetaField metaField = findField((String) fieldNameObj);
              if (metaField != null) {
                metaIndex.addColumn(metaField);
              }
              else {
                System.err.println("MongoJDBC discover index cannot find metaField '" + fieldNameObj + "' for index " + indexObject);
              }
            }
          }
        }
      }
    }
    catch (Throwable ex) {
      System.err.println("Error in discover indexes " + dbCollection + "." + this + ". " + ex);
    }
  }


}
