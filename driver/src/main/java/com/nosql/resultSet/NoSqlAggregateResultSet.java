package com.nosql.resultSet;

import com.mongodb.AggregationOutput;
import com.mongodb.DBObject;

import java.sql.SQLException;
import java.util.Iterator;

public class NoSqlAggregateResultSet extends NoSqlIteratorResultSet {

    private final Iterator<DBObject> iterator;


    public NoSqlAggregateResultSet(AggregationOutput aggregationOutput){
        this.iterator = aggregationOutput.results().iterator();
    }

    @Override
    public boolean next() throws SQLException {
        actual = null;
        if ( iterator != null ) {
            if ( iterator.hasNext() ) {
                actual = iterator.next();
                return true;
            }
        }
        return false;
    }


}
