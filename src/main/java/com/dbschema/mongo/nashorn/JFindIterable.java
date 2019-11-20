package com.dbschema.mongo.nashorn;

import com.mongodb.Block;
import com.mongodb.CursorType;
import com.mongodb.Function;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dbschema.mongo.nashorn.JMongoUtil.toBson;


@SuppressWarnings({"unused", "UnusedReturnValue"})
public class JFindIterable<TResult> implements com.mongodb.client.MongoIterable<TResult> {

  private final FindIterable<TResult> findIterable;

  public JFindIterable(FindIterable<TResult> findIterable) {
    this.findIterable = findIterable;
  }

  public JFindIterable<TResult> filter(String str) {
    findIterable.filter(JMongoUtil.parse(str));
    return this;
  }

  public JFindIterable<TResult> filter(Map<?, ?> map) {
    findIterable.filter(toBson(map));
    return this;
  }

  public JFindIterable<TResult> modifiers(String str) {
    //noinspection deprecation
    findIterable.modifiers(JMongoUtil.parse(str));
    return this;
  }

  public JFindIterable<TResult> projection(String str) {
    findIterable.projection(JMongoUtil.parse(str));
    return this;
  }

  public JFindIterable<TResult> projection(Map<?, ?> map) {
    findIterable.projection(toBson(map));
    return this;
  }

  public JFindIterable<TResult> sort(String str) {
    findIterable.sort(JMongoUtil.parse(str));
    return this;
  }

  public JFindIterable<TResult> sort(Map<?, ?> map) {
    findIterable.sort(toBson(map));
    return this;
  }


  //---------------------------------------------------------------

  public JFindIterable<TResult> filter(Bson bson) {
    findIterable.filter(bson);
    return this;
  }

  public JFindIterable<TResult> limit(int i) {
    findIterable.limit(i);
    return this;
  }

  public JFindIterable<TResult> skip(int i) {
    findIterable.skip(i);
    return this;
  }

  public JFindIterable<TResult> maxTime(long l, TimeUnit timeUnit) {
    findIterable.maxTime(l, timeUnit);
    return this;
  }

  public JFindIterable<TResult> modifiers(Bson bson) {
    //noinspection deprecation
    findIterable.modifiers(bson);
    return this;
  }

  public JFindIterable<TResult> projection(Bson bson) {
    findIterable.projection(bson);
    return this;
  }

  public JFindIterable<TResult> sort(Bson bson) {
    findIterable.sort(bson);
    return this;
  }

  public JFindIterable<TResult> noCursorTimeout(boolean b) {
    findIterable.noCursorTimeout(b);
    return this;
  }

  public JFindIterable<TResult> oplogReplay(boolean b) {
    findIterable.oplogReplay(b);
    return this;
  }

  public JFindIterable<TResult> partial(boolean b) {
    findIterable.partial(b);
    return this;
  }

  public JFindIterable<TResult> cursorType(CursorType cursorType) {
    findIterable.cursorType(cursorType);
    return this;
  }

  @NotNull
  public JFindIterable<TResult> batchSize(int i) {
    findIterable.batchSize(i);
    return this;
  }

  @NotNull
  public MongoCursor<TResult> iterator() {
    return findIterable.iterator();
  }

  @NotNull
  @Override
  public MongoCursor<TResult> cursor() {
    return findIterable.cursor();
  }

  public TResult first() {
    return findIterable.first();
  }

  @NotNull
  public <U> MongoIterable<U> map(@NotNull Function<TResult, U> tResultUFunction) {
    return findIterable.map(tResultUFunction);
  }

  @NotNull
  public <A extends Collection<? super TResult>> A into(@NotNull A a) {
    return findIterable.into(a);
  }

  public void forEach(@NotNull Block<? super TResult> block) {
    //noinspection deprecation
    findIterable.forEach(block);
  }
}
