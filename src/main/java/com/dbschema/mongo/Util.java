package com.dbschema.mongo;

import com.dbschema.mongo.resultSet.ListResultSet;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.print.Doc;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;

/**
 * @author Liudmila Kornilova
 **/
public class Util {
  public static String nullize(String text) {
    return text == null || text.isEmpty() ? null : text;
  }

  public static ResultSet ok() {
    return new ListResultSet("OK", new String[]{"result"});
  }

  public static ResultSet ok(Object result) {
    return new ListResultSet(result, new String[]{"result"});
  }

  public static ResultSet ok(Document result) {
    return new ListResultSet(result, new String[]{"map"});
  }

  public static ResultSet error() {
    return new ListResultSet("ERROR", new String[]{"result"});
  }

  @Contract(pure = true)
  public static <T> T find(@NotNull Iterable<? extends T> iterable, @NotNull Condition<? super T> condition) {
    return find(iterable.iterator(), condition);
  }

  public static <T> T find(@NotNull Iterator<? extends T> iterator, @NotNull Condition<? super T> condition) {
    while (iterator.hasNext()) {
      T value = iterator.next();
      if (condition.value(value)) return value;
    }
    return null;
  }

  @NotNull
  @Contract(pure = true)
  public static <T> T[] filter(@NotNull T[] collection, @NotNull Condition<? super T> condition) {
    List<T> result = new ArrayList<>();
    for (T t : collection) {
      if (condition.value(t)) {
        result.add(t);
      }
    }
    //noinspection unchecked
    return ((T[]) result.toArray());
  }

  @NotNull
  @Contract(pure = true)
  public static <T, R> R[] map(@NotNull T[] collection, @NotNull Function<? super T, R> func) {
    List<R> result = new ArrayList<>();
    for (T t : collection) {
      result.add(func.apply(t));
    }
    //noinspection unchecked
    return ((R[]) result.toArray());
  }

  @Nullable
  @Contract("!null -> !null")
  public static Document toDocument(@Nullable BsonDocument bson) {
    return bson == null ? null : getDefaultCodecRegistry().get(Document.class).decode(bson.asBsonReader(), DecoderContext.builder().build());
  }
}
