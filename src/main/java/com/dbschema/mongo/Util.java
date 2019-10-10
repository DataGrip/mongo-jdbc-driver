package com.dbschema.mongo;

import com.dbschema.mongo.resultSet.ListResultSet;
import org.bson.Document;

import java.sql.ResultSet;

/**
 * @author Liudmila Kornilova
 **/
public class Util {
  public static String nullize(String text) {
    return text == null || text.isEmpty() ? null : text;
  }

  public static ResultSet ok() {
    return new ListResultSet("OK", new String[]{"result"});
  }

  public static ResultSet ok(Object result) {
    return new ListResultSet(result, new String[]{"result"});
  }

  public static ResultSet ok(Document result) {
    return new ListResultSet(result, new String[]{"map"});
  }

  public static ResultSet error() {
    return new ListResultSet("ERROR", new String[]{"result"});
  }
}
