package com.dbschema.mongo;

/**
 * @author Liudmila Kornilova
 **/
public class Util {
  public static String nullize(String text) {
    return text == null || text.isEmpty() ? null : text;
  }
}
