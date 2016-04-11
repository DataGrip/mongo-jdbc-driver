package com.mongoui.sample;

import com.mongodb.*;


public class Sample {

    private final MongoClient mongoClient;
    private final DB db;
    public final DBCollection sampleCollection;
    public final DBObject queryOne;
    public final BasicDBObject queryExpr;


    public Sample() throws Exception {

        mongoClient = new MongoClient("localhost", 27017);
        db = mongoClient.getDB("local");

        sampleCollection = db.getCollection("user");
        sampleCollection.remove( new BasicDBObject());

        new User( db );

        queryOne = sampleCollection.findOne();
        queryExpr = new BasicDBObject("age", new BasicDBObject("$ne", 3)).append("age", new BasicDBObject("$gt", 10));


    }


    public BasicDBList execute( BasicDBObject query ){
        DBCursor cursor = query != null ? sampleCollection.find(query) : sampleCollection.find();
        final BasicDBList result = new BasicDBList();
        try {
            while(cursor.hasNext()) {
                DBObject object = cursor.next();
                result.add( object );
            }
        } finally {
            cursor.close();
        }
        return result;
    }




}
