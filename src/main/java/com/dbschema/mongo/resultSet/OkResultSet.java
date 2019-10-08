package com.dbschema.mongo.resultSet;

import java.sql.ResultSetMetaData;
import java.sql.Types;

public class OkResultSet extends ResultSetIterator {

  public OkResultSet() {
  }

  @Override
  public Object getObject(int columnIndex) {
    return "Ok";
  }

  @Override
  public boolean next() {
    return false;
  }

  @Override
  public void close() {
  }

  @Override
  public ResultSetMetaData getMetaData() {
    return new MongoResultSetMetaData("Result", new String[]{"map"}, new int[]{Types.JAVA_OBJECT});
  }
}
