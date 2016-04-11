package com.nosql.mongo;


import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JMongoCollection<TDocument> {


    private final MongoCollection<TDocument> nativeCollection;

    public JMongoCollection( MongoCollection<TDocument> nativeCollection){
        this.nativeCollection = nativeCollection;
    }

    public AggregateIterable aggregate(String... strings) {
        final List<Bson> bsons = new ArrayList<Bson>();
        for ( String str : strings ){
            bsons.add(JMongoUtil.parse(str));
        }
        return nativeCollection.aggregate( bsons );
    }

    public void insert( String str ){
        insertOne( str );
    }

    public void insertOne( String str ){
        nativeCollection.insertOne((TDocument)JMongoUtil.parse(str));
    }

    public void insert( Map map ){
        insertOne( map );
    }

    public void insertOne( Map map ){
        JMongoUtil.doConversions(map);
        nativeCollection.insertOne( (TDocument)(new Document( map )) );
    }


    public long count(String str) {
        return nativeCollection.count( JMongoUtil.parse(str) );
    }

    public long count(String str, CountOptions countOptions) {
        return nativeCollection.count( JMongoUtil.parse(str), countOptions);
    }

    public JFindIterable find(String str) {
        return new JFindIterable<TDocument>( nativeCollection.find( JMongoUtil.parse(str) ) );
    }

    public <TResult> JFindIterable find(String str, Class aClass) {
        return new JFindIterable<TDocument>( nativeCollection.find( JMongoUtil.parse(str), aClass ));
    }

    public JFindIterable find( Map map ){
        JMongoUtil.doConversions(map);
        return new JFindIterable<TDocument>( nativeCollection.find(new Document( map )));
    }

    public DeleteResult delete(String str) {
        return deleteOne( str );
    }

    public DeleteResult deleteOne(String str) {
        return nativeCollection.deleteOne( JMongoUtil.parse(str) );
    }

    public DeleteResult delete( Map map ){
        return deleteOne( map );
    }
    public DeleteResult deleteOne( Map map ){
        JMongoUtil.doConversions(map);
        return nativeCollection.deleteOne( new Document( map ) );
    }


    public DeleteResult deleteMany(String str) {
        return nativeCollection.deleteMany( JMongoUtil.parse(str) );
    }


    public UpdateResult replaceOne(String str, TDocument o) {
        return nativeCollection.replaceOne( JMongoUtil.parse(str), o );
    }


    public UpdateResult replaceOne(String str, TDocument o, UpdateOptions updateOptions) {
        return nativeCollection.replaceOne( JMongoUtil.parse(str), o, updateOptions);
    }

    public UpdateResult updateOne(Map map, Map map1) {
        JMongoUtil.doConversions(map);
        JMongoUtil.doConversions(map1);
        return nativeCollection.updateOne( new Document(map), new Document(map1));
    }

    public UpdateResult updateOne(String str, String str1) {
        return nativeCollection.updateOne( JMongoUtil.parse(str), BsonDocument.parse(str1));
    }

    public UpdateResult updateOne(Map map, Map map1, UpdateOptions updateOptions) {
        JMongoUtil.doConversions(map);
        JMongoUtil.doConversions(map1);
        return nativeCollection.updateOne( new Document(map), new Document(map1), updateOptions );
    }

    public UpdateResult updateOne(String str, String str1, UpdateOptions updateOptions) {
        return nativeCollection.updateOne( JMongoUtil.parse(str), BsonDocument.parse(str1), updateOptions);
    }

    public UpdateResult update(Map map, Map map1) {
        return updateMany( map, map1 );
    }
    public UpdateResult updateMany(Map map, Map map1) {
        return nativeCollection.updateMany( new Document(map), new Document(map1) );
    }

    public UpdateResult updateMany(String str, String str1) {
        return nativeCollection.updateMany( JMongoUtil.parse(str), BsonDocument.parse(str1) );
    }
    public UpdateResult update(Map map, Map map1, UpdateOptions updateOptions) {
        return updateMany( map, map1, updateOptions );
    }
    public UpdateResult updateMany(Map map, Map map1, UpdateOptions updateOptions) {
        JMongoUtil.doConversions(map);
        JMongoUtil.doConversions(map1);
        return nativeCollection.updateMany( new Document(map), new Document(map1), updateOptions );
    }

    public UpdateResult updateMany(String str, String str1, UpdateOptions updateOptions) {
        return nativeCollection.updateMany( JMongoUtil.parse(str), BsonDocument.parse(str1), updateOptions );
    }

    public TDocument findOneAndDelete(Map map) {
        JMongoUtil.doConversions(map);
        return nativeCollection.findOneAndDelete( new Document( map ) );
    }

    public TDocument findOneAndDelete(String str) {
        return nativeCollection.findOneAndDelete( JMongoUtil.parse(str) );
    }


    public TDocument findOneAndDelete(String str, FindOneAndDeleteOptions findOneAndDeleteOptions) {
        return nativeCollection.findOneAndDelete( JMongoUtil.parse(str), findOneAndDeleteOptions );
    }


    public TDocument findOneAndReplace(String str, TDocument o) {
        return nativeCollection.findOneAndReplace( JMongoUtil.parse(str), o);
    }


    public TDocument findOneAndReplace(String str, TDocument o, FindOneAndReplaceOptions findOneAndReplaceOptions) {
        return nativeCollection.findOneAndReplace( JMongoUtil.parse(str), o, findOneAndReplaceOptions );
    }


    public TDocument findOneAndUpdate(String str, String str1) {
        return nativeCollection.findOneAndUpdate( JMongoUtil.parse(str), BsonDocument.parse(str1) );
    }


    public TDocument findOneAndUpdate(String str, String str1, FindOneAndUpdateOptions findOneAndUpdateOptions) {
        return nativeCollection.findOneAndUpdate( JMongoUtil.parse(str), BsonDocument.parse(str1), findOneAndUpdateOptions );
    }


    public String createIndex(String str) {
        return nativeCollection.createIndex( JMongoUtil.parse(str) );
    }

    public String createIndex( Map map ) {
        JMongoUtil.doConversions(map);
        return nativeCollection.createIndex( new Document( map ) );
    }

    public String createIndex(String str, IndexOptions indexOptions) {
        return nativeCollection.createIndex( JMongoUtil.parse(str), indexOptions );
    }

    public String createIndex( Map map, Map optionsMap ) {
        JMongoUtil.doConversions(map);
        return nativeCollection.createIndex( new Document( map ), new IndexOptionsFromMap( optionsMap) );
    }

    //---------------------------------------------------------------

    public MongoNamespace getNamespace() {
        return nativeCollection.getNamespace();
    }

    public Class getDocumentClass() {
        return nativeCollection.getDocumentClass();
    }

    public CodecRegistry getCodecRegistry() {
        return nativeCollection.getCodecRegistry();
    }

    public ReadPreference getReadPreference() {
        return nativeCollection.getReadPreference();
    }

    public WriteConcern getWriteConcern() {
        return nativeCollection.getWriteConcern();
    }

    public <NewTDocument> JMongoCollection withDocumentClass(Class aClass) {
        nativeCollection.withDocumentClass( aClass);
        return this;
    }

    public JMongoCollection withCodecRegistry(CodecRegistry codecRegistry) {
        nativeCollection.withCodecRegistry( codecRegistry);
        return this;
    }

    public JMongoCollection withReadPreference(ReadPreference readPreference) {
        nativeCollection.withReadPreference( readPreference );
        return this;
    }

    public JMongoCollection withWriteConcern(WriteConcern writeConcern) {
        nativeCollection.withWriteConcern( writeConcern );
        return this;
    }

    public long count() {
        return nativeCollection.count();
    }

    public long count(Bson bson) {
        return nativeCollection.count( bson );
    }

    public long count(Bson bson, CountOptions countOptions) {
        return nativeCollection.count( bson, countOptions);
    }

    public <TResult> DistinctIterable distinct(String s, Class<TResult> aClass) {
        return nativeCollection.distinct( s, aClass );
    }

    public JFindIterable find() {
        return new JFindIterable( nativeCollection.find() );
    }

    public <TResult> JFindIterable find(Class<TResult> aClass) {
        return new JFindIterable( nativeCollection.find( aClass ) );
    }

    public JFindIterable find(Bson bson) {
        return new JFindIterable( nativeCollection.find( bson ) );
    }

    public <TResult> JFindIterable find(Bson bson, Class<TResult> aClass) {
        return new JFindIterable( nativeCollection.find( bson, aClass ));
    }

    public AggregateIterable aggregate(List<? extends Bson> bsons) {
        return nativeCollection.aggregate( bsons );
    }

    public <TResult> AggregateIterable aggregate(List<? extends Bson> bsons, Class<TResult> aClass) {
        return nativeCollection.aggregate( bsons, aClass );
    }

    /*
    Sample:
local.words.drop()

local.words.insertOne({word: 'bla'});
local.words.insertOne({word: 'cla'});
local.words.insertOne({word: 'zla'});

local.words.find()

local.words.mapReduce(
    "function map() { \r\n" +
    " emit(this.word, {count: 1}) \r\n" +
    "}",
    " function reduce(key, values) { \r\n" +
    "    var count = 0 \r\n" +
    "    for (var i = 0; i < values.length; i++) \r\n" +
    "        count += values[i].count \r\n" +
    "    return {count: count} \r\n" +
    "}"
    // , "mrresult"
)

OR


var m =function map() {
 emit(this.word, {count: 5})
}
var r=function reduce(key, values) {
        var count = 5
        for (var i = 0; i < values.length; i++)
            count += values[i].count
        return {count: count}
    }
local.words.mapReduce(m, r );


     */

    public MapReduceIterable mapReduce(String s, String s1) {
        return nativeCollection.mapReduce( s, s1);
    }

    public <TResult> MapReduceIterable mapReduce(String s, String s1, Class<TResult> aClass) {
        return nativeCollection.mapReduce( s, s1, aClass );
    }

    public BulkWriteResult bulkWrite(List list) {
        return nativeCollection.bulkWrite( list );
    }


    public BulkWriteResult bulkWrite(List list, BulkWriteOptions bulkWriteOptions) {
        return nativeCollection.bulkWrite( list, bulkWriteOptions);
    }


    public void insertOne( TDocument o) {
        nativeCollection.insertOne(o );
    }


    public void insertMany(List list) {
        nativeCollection.insertMany( list );
    }


    public void insertMany(List list, InsertManyOptions insertManyOptions) {
        nativeCollection.insertMany(list, insertManyOptions);
    }


    public DeleteResult deleteOne(Bson bson) {
        return nativeCollection.deleteOne( bson );
    }


    public DeleteResult deleteMany(Bson bson) {
        return nativeCollection.deleteMany( bson );
    }


    public UpdateResult replaceOne(Bson bson, TDocument o) {
        return nativeCollection.replaceOne( bson, o );
    }


    public UpdateResult replaceOne(Bson bson, TDocument o, UpdateOptions updateOptions) {
        return nativeCollection.replaceOne( bson, o, updateOptions);
    }


    public UpdateResult updateOne(Bson bson, Bson bson1) {
        return nativeCollection.updateOne( bson, bson1);
    }


    public UpdateResult updateOne(Bson bson, Bson bson1, UpdateOptions updateOptions) {
        return nativeCollection.updateOne( bson, bson1, updateOptions);
    }


    public UpdateResult updateMany(Bson bson, Bson bson1) {
        return nativeCollection.updateMany( bson, bson1);
    }


    public UpdateResult updateMany(Bson bson, Bson bson1, UpdateOptions updateOptions) {
        return nativeCollection.updateMany( bson, bson1, updateOptions );
    }


    public TDocument findOneAndDelete(Bson bson) {
        return nativeCollection.findOneAndDelete( bson );
    }


    public TDocument findOneAndDelete(Bson bson, FindOneAndDeleteOptions findOneAndDeleteOptions) {
        return nativeCollection.findOneAndDelete( bson, findOneAndDeleteOptions );
    }


    public TDocument findOneAndReplace(Bson bson, TDocument o) {
        return nativeCollection.findOneAndReplace( bson, o);
    }


    public TDocument findOneAndReplace(Bson bson, TDocument o, FindOneAndReplaceOptions findOneAndReplaceOptions) {
        return nativeCollection.findOneAndReplace( bson, o, findOneAndReplaceOptions );
    }


    public TDocument findOneAndUpdate(Bson bson, Bson bson1) {
        return nativeCollection.findOneAndUpdate( bson, bson1 );
    }


    public TDocument findOneAndUpdate(Bson bson, Bson bson1, FindOneAndUpdateOptions findOneAndUpdateOptions) {
        return nativeCollection.findOneAndUpdate( bson, bson1, findOneAndUpdateOptions );
    }


    public void drop() {
        nativeCollection.drop();
    }


    public String createIndex(Bson bson) {
        return nativeCollection.createIndex( bson );
    }


    public String createIndex(Bson bson, IndexOptions indexOptions) {
        return nativeCollection.createIndex( bson, indexOptions );
    }


    public List<String> createIndexes(List<IndexModel> indexModels) {
        return nativeCollection.createIndexes( indexModels );
    }


    public ListIndexesIterable<Document> listIndexes() {
        return nativeCollection.listIndexes();
    }


    public <TResult> ListIndexesIterable listIndexes(Class<TResult> aClass) {
        return nativeCollection.listIndexes( aClass );
    }


    public void dropIndex(String s) {
        nativeCollection.dropIndex( s );
    }


    public void dropIndex(Bson bson) {
        nativeCollection.dropIndex( bson );
    }


    public void dropIndexes() {
        nativeCollection.dropIndexes();
    }


    public void renameCollection(String newName) {
        nativeCollection.renameCollection( new MongoNamespace( nativeCollection.getNamespace().getDatabaseName(), newName ));
    }

    public void renameCollection(MongoNamespace mongoNamespace) {
        nativeCollection.renameCollection( mongoNamespace );
    }


    public void renameCollection(MongoNamespace mongoNamespace, RenameCollectionOptions renameCollectionOptions) {
        nativeCollection.renameCollection( mongoNamespace, renameCollectionOptions );
    }
}
