package com.dbschema.mongo.nashorn;

import com.dbschema.mongo.nashorn.parser.JsonLoaderCallback;
import com.dbschema.mongo.nashorn.parser.JsonParseException;
import com.dbschema.mongo.nashorn.parser.JsonParser;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbschema.mongo.Util.tryCast;
import static java.time.temporal.ChronoField.*;

@SuppressWarnings("UnusedReturnValue")
public class JMongoUtil {
  private static final Pattern REGEX_BODY = Pattern.compile("/(.*)/[a-z]*");
  /**
   * yyyy-MM-dd['T'HH:mm:ss.SSS['Z']]
   */
  private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(DateTimeFormatter.ISO_LOCAL_DATE)
      .optionalStart()
      .appendLiteral('T')
      .append(DateTimeFormatter.ISO_LOCAL_TIME)
      .optionalStart()
      .appendLiteral('Z')
      .optionalEnd()
      .optionalEnd()
      .toFormatter();

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

  @SuppressWarnings("unused")
  @NotNull
  public static Date now() {
    return new Date(Instant.now().toEpochMilli());
  }

  @SuppressWarnings("unused")
  @NotNull
  public static Date parseDate(@NotNull String str) {
    TemporalAccessor accessor = DATE_FORMATTER.parse(str);
    LocalDateTime localDateTime = LocalDateTime.of(
        getFieldOrZero(accessor, YEAR),
        getFieldOrZero(accessor, MONTH_OF_YEAR),
        getFieldOrZero(accessor, DAY_OF_MONTH),
        getFieldOrZero(accessor, HOUR_OF_DAY),
        getFieldOrZero(accessor, MINUTE_OF_HOUR),
        getFieldOrZero(accessor, SECOND_OF_MINUTE),
        getFieldOrZero(accessor, NANO_OF_SECOND));
    return new Date(localDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli());
  }

  private static int getFieldOrZero(TemporalAccessor accessor, TemporalField field) {
    try {
      return accessor.get(field);
    }
    catch (UnsupportedTemporalTypeException ignored) {
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @NotNull
  public static Binary binData(@Nullable Integer subtype, @Nullable String data) {
    if (data == null) {
      throw new IllegalArgumentException("BinData data must be a String");
    }
    if (subtype == null || subtype < 0 || subtype > 255) {
      throw new IllegalArgumentException("BinData subtype must be a Number between 0 and 255 inclusive");
    }
    return new Binary((byte) (int) subtype, Base64.getDecoder().decode(data));
  }
}
