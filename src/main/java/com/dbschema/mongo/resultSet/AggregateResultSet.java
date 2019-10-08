package com.dbschema.mongo.resultSet;

import com.mongodb.AggregationOutput;
import com.mongodb.DBObject;

import java.util.Iterator;

public class AggregateResultSet extends ResultSetIterator {

  private final Iterator<DBObject> iterator;


  public AggregateResultSet(AggregationOutput aggregationOutput) {
    this.iterator = aggregationOutput.results().iterator();
  }

  @Override
  public boolean next() {
    actual = null;
    if (iterator != null) {
      if (iterator.hasNext()) {
        actual = iterator.next();
        return true;
      }
    }
    return false;
  }
}
