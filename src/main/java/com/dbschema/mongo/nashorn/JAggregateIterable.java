package com.dbschema.mongo.nashorn;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.TimeUnit;


public class JAggregateIterable implements com.mongodb.client.MongoIterable<Document> {
  private final AggregateIterable<Document> aggregateIterable;

  public JAggregateIterable(AggregateIterable<Document> aggregateIterable) {
    this.aggregateIterable = aggregateIterable;
  }

  public JAggregateIterable allowDiskUse(Boolean aBoolean) {
    aggregateIterable.allowDiskUse(aBoolean);
    return this;
  }

  @NotNull
  public JAggregateIterable batchSize(int i) {
    aggregateIterable.batchSize(i);
    return this;
  }

  public JAggregateIterable maxTime(long l, TimeUnit timeUnit) {
    aggregateIterable.maxTime(l, timeUnit);
    return this;
  }

  @SuppressWarnings("deprecation")
  public JAggregateIterable useCursor(Boolean aBoolean) {
    aggregateIterable.useCursor(aBoolean);
    return this;
  }

  @NotNull
  public MongoCursor<Document> iterator() {
    return aggregateIterable.iterator();
  }

  @NotNull
  @Override
  public MongoCursor<Document> cursor() {
    return aggregateIterable.cursor();
  }

  public Document first() {
    return aggregateIterable.first();
  }


  @NotNull
  public <U> com.mongodb.client.MongoIterable<U> map(@NotNull com.mongodb.Function<Document, U> function) {
    return aggregateIterable.map(function);
  }

  @SuppressWarnings("deprecation")
  public void forEach(@NotNull com.mongodb.Block<? super Document> block) {
    aggregateIterable.forEach(block);
  }

  @NotNull
  public <A extends Collection<? super Document>> A into(@NotNull A a) {
    return aggregateIterable.into(a);
  }
}
