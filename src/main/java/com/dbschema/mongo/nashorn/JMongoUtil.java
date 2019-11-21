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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbschema.mongo.Util.tryCast;

@SuppressWarnings("UnusedReturnValue")
public class JMongoUtil {
  private static final Pattern REGEX_BODY = Pattern.compile("/(.*)/[a-z]*");

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
    if (o instanceof ScriptObjectMirror) {
      ScriptObjectMirror mirror = (ScriptObjectMirror) o;
      if (mirror.isArray()) return new JSArray((ScriptObjectMirror) o);

      if ("RegExp".equals(mirror.getClassName())) {
        String regexLiteral = (String) mirror.getDefaultValue(String.class);
        Matcher matcher = REGEX_BODY.matcher(regexLiteral);
        if (matcher.matches()) {
          String body = matcher.group(1);
          Boolean ignoreCase = tryCast(mirror.get("ignoreCase"), Boolean.class);
          Boolean multiline = tryCast(mirror.get("multiline"), Boolean.class);
          return Pattern.compile(body, (ignoreCase == Boolean.TRUE ? Pattern.CASE_INSENSITIVE : 0) |
              (multiline == Boolean.TRUE ? Pattern.MULTILINE : 0));
        }
      }
    }
    return o instanceof Map<?, ?> ? toBson((Map<?, ?>) o) :
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
