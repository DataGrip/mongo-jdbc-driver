package com.mongoui.sample;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class User {

    private final DBCollection colection;

    public User(DB db){
        colection = db.getCollection("user");
        colection.remove( new BasicDBObject());

        for ( int i = 0; i < 100; i++ ) {
            BasicDBObject doc = new BasicDBObject().
                    append("_id", i).
                    append("age", i).
                    append("username", "luigi" + (int)( Math.random() * 1000)).
                    append("password", "luigi" + (int)( Math.random() * 1000));
            if ( i %10 == 0 ){
                doc.append( "asSubObject", new BasicDBObject().append("car", "Audi").append("birthdate", "last year").
                        append("thirdChild", new BasicDBObject().append("food", "Vegi").append("sport", "swim")));
            }
            if ( i %10 == 1 ){
                doc.append( "asArray", new DBObject[]{ new BasicDBObject().append("wa", "w1"), new BasicDBObject().append( "wa", "w2")} );
            }
            if ( i %10 == 2 ){
                List<DBObject> list = new ArrayList<DBObject>();
                list.add( new BasicDBObject().append("wa", "w1").append("date", new Date( System.currentTimeMillis() - (long)(100*Math.random()))) );
                list.add( new BasicDBObject().append("wa", "w2").append("date", new Date(System.currentTimeMillis() - (long) (100 * Math.random()))) );
                doc.append( "asList", list );
            }
            colection.insert(doc);
        }
    }

}