package com.dbschema.mongo.nashorn;

import com.dbschema.mongo.nashorn.parser.JsonLoaderCallback;
import com.dbschema.mongo.nashorn.parser.JsonParseException;
import com.dbschema.mongo.nashorn.parser.JsonParser;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  public static Bson toBson(@NotNull ScriptObjectMirror objectMirror) {
    Document doc = new Document();
    for (String key : objectMirror.keySet()) {
      Object value = toBsonValue(objectMirror.get(key));
      doc.put(key, value);
    }
    return doc;
  }

  private static Object toBsonValue(@Nullable Object o) {
    return o instanceof ScriptObjectMirror && ((ScriptObjectMirror) o).isArray() ?
           new ScriptObjectMirrorList((ScriptObjectMirror) o) :
           o instanceof Map<?, ?> ? toBson((Map<?, ?>) o) :
           o;
  }

  public static Document toBson(@NotNull Map<?, ?> map) {
    Document doc = new Document();
    for (Object key : map.keySet()) {
      if (!(key instanceof String)) continue;
      Object value = toBsonValue(map.get(key));
      doc.put((String) key, value);
    }
    return doc;
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
