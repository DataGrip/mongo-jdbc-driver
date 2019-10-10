package com.dbschema.mongo.nashorn;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCursor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("unused")
public class JAggregateIterable<TResult> implements com.mongodb.client.MongoIterable<TResult> {

  private final AggregateIterable<TResult> aggregateIterable;

  public JAggregateIterable(AggregateIterable<TResult> aggregateIterable) {
    this.aggregateIterable = aggregateIterable;
  }

  public JAggregateIterable<TResult> allowDiskUse(Boolean aBoolean) {
    aggregateIterable.allowDiskUse(aBoolean);
    return this;
  }

  @NotNull
  public JAggregateIterable<TResult> batchSize(int i) {
    aggregateIterable.batchSize(i);
    return this;
  }

  public JAggregateIterable<TResult> maxTime(long l, TimeUnit timeUnit) {
    aggregateIterable.maxTime(l, timeUnit);
    return this;
  }

  @SuppressWarnings("deprecation")
  public JAggregateIterable<TResult> useCursor(Boolean aBoolean) {
    aggregateIterable.useCursor(aBoolean);
    return this;
  }

  @NotNull
  public MongoCursor<TResult> iterator() {
    return aggregateIterable.iterator();
  }

  @NotNull
  @Override
  public MongoCursor<TResult> cursor() {
    return aggregateIterable.cursor();
  }

  public TResult first() {
    return aggregateIterable.first();
  }


  @NotNull
  public <U> com.mongodb.client.MongoIterable<U> map(@NotNull com.mongodb.Function<TResult, U> function) {
    return aggregateIterable.map(function);
  }

  @SuppressWarnings("deprecation")
  public void forEach(@NotNull com.mongodb.Block<? super TResult> block) {
    aggregateIterable.forEach(block);
  }

  @NotNull
  public <A extends Collection<? super TResult>> A into(@NotNull A a) {
    return aggregateIterable.into(a);
  }
}
