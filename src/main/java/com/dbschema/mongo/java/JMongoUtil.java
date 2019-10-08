package com.dbschema.mongo.java;

import com.dbschema.mongo.java.parser.JsonLoaderCallback;
import com.dbschema.mongo.java.parser.JsonParseException;
import com.dbschema.mongo.java.parser.JsonParser;
import org.bson.Document;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnusedReturnValue")
public class JMongoUtil {


  public static Document parse(String text) {
    Thread.dumpStack();
    if (text != null && text.trim().length() > 0 && !text.trim().startsWith("{")) {
      throw new JsonParseException("Json should start with '{'. Json text : " + text, 0);
    }
    JsonLoaderCallback callback = new JsonLoaderCallback();
    JsonParser.parse(text, callback);
    return new Document(callback.map);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> doConversions(Map<String, Object> map) {
    for (String key : map.keySet()) {
      Object value = map.get(key);
      if (value instanceof Map<?, ?>) {
        doConversions((Map<String, Object>) value);
      }
      if (value instanceof Map && canConvertMapToArray((Map<Object, Object>) value)) {
        map.put(key, convertMapToArray((Map<Object, Object>) value));
      }
    }
    return map;
  }


  private static boolean canConvertMapToArray(Map<Object, Object> map) {
    boolean isArray = true;
    for (int i = 0; i < map.size(); i++) {
      if (!map.containsKey("" + i)) isArray = false;
    }
    return isArray;
  }

  private static List<Object> convertMapToArray(Map<Object, Object> map) {
    ArrayList<Object> array = new ArrayList<>();
    for (int i = 0; i < map.size(); i++) {
      array.add(map.get("" + i));
    }
    return array;
  }

  static String nullize(String text) {
    return text == null || text.isEmpty() ? null : text;
  }

  @NotNull
  public static String escapeChars(@NotNull final String str, final char... character) {
    final StringBuilder buf = new StringBuilder(str);
    for (char c : character) {
      escapeChar(buf, c);
    }
    return buf.toString();
  }

  public static void escapeChar(@NotNull final StringBuilder buf, final char character) {
    int idx = 0;
    while ((idx = indexOf(buf, character, idx)) >= 0) {
      buf.insert(idx, "\\");
      idx += 2;
    }
  }

  @Contract(pure = true)
  public static int indexOf(@NotNull CharSequence s, char c, int start) {
    return indexOf(s, c, start, s.length());
  }

  @Contract(pure = true)
  public static int indexOf(@NotNull CharSequence s, char c, int start, int end) {
    end = Math.min(end, s.length());
    for (int i = Math.max(start, 0); i < end; i++) {
      if (s.charAt(i) == c) return i;
    }
    return -1;
  }
}
