package com.dbschema.mongo;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCursor;

import java.util.Collection;
import java.util.concurrent.TimeUnit;


public class JAggregateIterableNotUsed<TResult> {

    private final AggregateIterable<TResult> aggregateIterable;
    
    public JAggregateIterableNotUsed(AggregateIterable<TResult> aggregateIterable){
        this.aggregateIterable = aggregateIterable;
    }

    public JAggregateIterableNotUsed allowDiskUse(Boolean aBoolean) {
        aggregateIterable.allowDiskUse( aBoolean );
        return this;
    }

    public JAggregateIterableNotUsed batchSize(int i) {
        aggregateIterable.batchSize( i );
        return this;
    }

    public JAggregateIterableNotUsed maxTime(long l, TimeUnit timeUnit) {
        aggregateIterable.maxTime( l, timeUnit );
        return this;
    }

    public JAggregateIterableNotUsed useCursor(Boolean aBoolean) {
        aggregateIterable.useCursor( aBoolean );
        return this;
    }

    public MongoCursor iterator() {
        return aggregateIterable.iterator();  
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

    public <A extends Collection<? super TResult>> Collection into(Collection collection) {
        return aggregateIterable.into( collection );  
    }
}
