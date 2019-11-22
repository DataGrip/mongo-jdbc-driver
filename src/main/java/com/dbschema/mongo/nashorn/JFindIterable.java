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


public class JFindIterable implements com.mongodb.client.MongoIterable<Document> {
  private final FindIterable<Document> findIterable;

  public JFindIterable(FindIterable<Document> findIterable) {
    this.findIterable = findIterable;
  }

  public JFindIterable filter(String str) {
    findIterable.filter(JMongoUtil.parse(str));
    return this;
  }

  public JFindIterable filter(Map<?, ?> map) {
    findIterable.filter(toBson(map));
    return this;
  }

  public JFindIterable modifiers(String str) {
    //noinspection deprecation
    findIterable.modifiers(JMongoUtil.parse(str));
    return this;
  }

  public JFindIterable projection(String str) {
    findIterable.projection(JMongoUtil.parse(str));
    return this;
  }

  public JFindIterable projection(Map<?, ?> map) {
    findIterable.projection(toBson(map));
    return this;
  }

  public JFindIterable sort(String str) {
    findIterable.sort(JMongoUtil.parse(str));
    return this;
  }

  public JFindIterable sort(Map<?, ?> map) {
    findIterable.sort(toBson(map));
    return this;
  }


  //---------------------------------------------------------------

  public JFindIterable filter(Bson bson) {
    findIterable.filter(bson);
    return this;
  }

  public JFindIterable limit(int i) {
    findIterable.limit(i);
    return this;
  }

  public JFindIterable skip(int i) {
    findIterable.skip(i);
    return this;
  }

  public JFindIterable maxTime(long l, TimeUnit timeUnit) {
    findIterable.maxTime(l, timeUnit);
    return this;
  }

  public JFindIterable modifiers(Bson bson) {
    //noinspection deprecation
    findIterable.modifiers(bson);
    return this;
  }

  public JFindIterable projection(Bson bson) {
    findIterable.projection(bson);
    return this;
  }

  public JFindIterable sort(Bson bson) {
    findIterable.sort(bson);
    return this;
  }

  public JFindIterable noCursorTimeout(boolean b) {
    findIterable.noCursorTimeout(b);
    return this;
  }

  public JFindIterable oplogReplay(boolean b) {
    findIterable.oplogReplay(b);
    return this;
  }

  public JFindIterable partial(boolean b) {
    findIterable.partial(b);
    return this;
  }

  public JFindIterable cursorType(CursorType cursorType) {
    findIterable.cursorType(cursorType);
    return this;
  }

  @NotNull
  public JFindIterable batchSize(int i) {
    findIterable.batchSize(i);
    return this;
  }

  @NotNull
  public MongoCursor<Document> iterator() {
    return findIterable.iterator();
  }

  @NotNull
  @Override
  public MongoCursor<Document> cursor() {
    return findIterable.cursor();
  }

  public Document first() {
    return findIterable.first();
  }

  @NotNull
  public <U> MongoIterable<U> map(@NotNull Function<Document, U> DocumentUFunction) {
    return findIterable.map(DocumentUFunction);
  }

  @NotNull
  public <A extends Collection<? super Document>> A into(@NotNull A a) {
    return findIterable.into(a);
  }

  public void forEach(@NotNull Block<? super Document> block) {
    //noinspection deprecation
    findIterable.forEach(block);
  }
}
