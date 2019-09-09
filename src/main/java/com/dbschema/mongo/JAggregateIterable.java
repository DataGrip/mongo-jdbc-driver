package com.dbschema.mongo;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCursor;

import java.util.Collection;
import java.util.concurrent.TimeUnit;


public class JAggregateIterable<TResult> implements com.mongodb.client.MongoIterable<TResult> {

    private final AggregateIterable<TResult> aggregateIterable;

    public JAggregateIterable(AggregateIterable<TResult> aggregateIterable){
        this.aggregateIterable = aggregateIterable;
    }

    public JAggregateIterable allowDiskUse(Boolean aBoolean) {
        aggregateIterable.allowDiskUse( aBoolean );
        return this;
    }

    public JAggregateIterable batchSize(int i) {
        aggregateIterable.batchSize( i );
        return this;
    }

    public JAggregateIterable maxTime(long l, TimeUnit timeUnit) {
        aggregateIterable.maxTime( l, timeUnit );
        return this;
    }

    public JAggregateIterable useCursor(Boolean aBoolean) {
        aggregateIterable.useCursor( aBoolean );
        return this;
    }

    public MongoCursor iterator() {
        return aggregateIterable.iterator();
    }

    @Override
    public MongoCursor<TResult> cursor()
    {
        return aggregateIterable.cursor();
    }

    public TResult first() {
        return aggregateIterable.first();
    }


    public <U> com.mongodb.client.MongoIterable<U> map(com.mongodb.Function<TResult,U> function) {
        return aggregateIterable.map( function );
    }

    public void forEach(com.mongodb.Block<? super TResult> block) {
        aggregateIterable.forEach( block );

    }

    public <A extends Collection<? super TResult>> A into(A a) {
        return aggregateIterable.into( a );
    }
}
