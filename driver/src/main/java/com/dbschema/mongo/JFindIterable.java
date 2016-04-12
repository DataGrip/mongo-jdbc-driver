package com.dbschema.mongo;

import com.mongodb.Block;
import com.mongodb.CursorType;
import com.mongodb.Function;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class JFindIterable<TResult> implements com.mongodb.client.MongoIterable<TResult> {

    private final FindIterable<TResult> findIterable;

    public JFindIterable( FindIterable<TResult> findIterable ){
        this.findIterable = findIterable;
    }

    public JFindIterable filter(String str) {
        findIterable.filter( JMongoUtil.parse(str) );
        return this;
    }

    public JFindIterable filter(Map map) {
        JMongoUtil.doConversions(map);
        findIterable.filter( new Document( map ) );
        return this;
    }

    public JFindIterable modifiers(String str) {
        findIterable.modifiers(JMongoUtil.parse(str));
        return this;
    }

    public JFindIterable projection(String str) {
        findIterable.projection(JMongoUtil.parse(str));
        return this;
    }

    public JFindIterable projection(Map map) {
        JMongoUtil.doConversions(map);
        findIterable.projection(new Document(map));
        return this;
    }

    public JFindIterable sort(String str) {
        findIterable.sort(JMongoUtil.parse(str));
        return this;
    }

    public JFindIterable sort(Map map) {
        JMongoUtil.doConversions(map);
        findIterable.sort( new Document( map ) );
        return this;
    }


    //---------------------------------------------------------------

    public JFindIterable filter(Bson bson) {
        findIterable.filter( bson );
        return this;
    }

    public JFindIterable limit(int i) {
        findIterable.limit( i );
        return this;
    }

    public JFindIterable skip(int i) {
        findIterable.skip( i );
        return this;
    }

    public JFindIterable maxTime(long l, TimeUnit timeUnit) {
        findIterable.maxTime( l, timeUnit);
        return this;
    }

    public JFindIterable modifiers(Bson bson) {
        findIterable.modifiers( bson );
        return this;
    }

    public JFindIterable projection(Bson bson) {
        findIterable.projection( bson );
        return this;
    }

    public JFindIterable sort(Bson bson) {
        findIterable.sort( bson );
        return this;
    }

    public JFindIterable noCursorTimeout(boolean b) {
        findIterable.noCursorTimeout( b );
        return this;
    }

    public JFindIterable oplogReplay(boolean b) {
        findIterable.oplogReplay( b );
        return this;
    }

    public JFindIterable partial(boolean b) {
        findIterable.partial( b );
        return this;
    }

    public JFindIterable cursorType(CursorType cursorType) {
        findIterable.cursorType( cursorType );
        return this;
    }

    public JFindIterable batchSize(int i) {
        findIterable.batchSize( i );
        return this;
    }

    public MongoCursor iterator() {
        return findIterable.iterator();
    }

    public TResult first() {
        return findIterable.first();
    }

    public <U> MongoIterable<U> map(Function<TResult, U> tResultUFunction) {
        return findIterable.map( tResultUFunction);
    }

    public <A extends Collection<? super TResult>> A into(A a) {
        return findIterable.into( a );
    }

    public void forEach(Block block) {
        findIterable.forEach( block );
    }


}
