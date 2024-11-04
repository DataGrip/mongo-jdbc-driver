package com.dbschema.mongo;

import com.dbschema.mongo.resultSet.ListResultSet;
import com.mongodb.AuthenticationMechanism;
import kotlin.Pair;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import static com.dbschema.mongo.DriverPropertyInfoHelper.*;
import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static java.util.Collections.emptyList;

public class Util {
  private static final String MONGODB_PREFIX = "mongodb://";
  private static final String MONGODB_SRV_PREFIX = "mongodb+srv://";
  /* see URLEncoder */
  static BitSet dontNeedEncoding = new BitSet(256);

  static {
    for (int i = 97; i <= 122; ++i) {
      dontNeedEncoding.set(i);
    }

    for (int i = 65; i <= 90; ++i) {
      dontNeedEncoding.set(i);
    }

    for (int i = 48; i <= 57; ++i) {
      dontNeedEncoding.set(i);
    }

    dontNeedEncoding.set(32);
    dontNeedEncoding.set(45);
    dontNeedEncoding.set(95);
    dontNeedEncoding.set(46);
    dontNeedEncoding.set(42);
  }

  public static String nullize(String text) {
    return text == null || text.isEmpty() ? null : text;
  }

  public static ResultSet ok() {
    return new ListResultSet("OK", new String[]{"result"});
  }

  public static ResultSet ok(Object result) {
    if (result instanceof Map<?, ?>) return ok((Map<?, ?>) result);
    if (result instanceof List<?>) {
      String name = Util.all((List<?>) result, o -> o instanceof Map) ? "map" : "result";
      return new ListResultSet(Util.map((List<?>) result, o -> new Object[]{o}), new String[]{name});
    }
    return new ListResultSet(result, new String[]{"result"});
  }

  public static ResultSet ok(Map<?, ?> result) {
    return new ListResultSet(result, new String[]{"map"});
  }

  public static ResultSet error() {
    return new ListResultSet("ERROR", new String[]{"result"});
  }

  @Contract(pure = true)
  public static <T> T find(@NotNull Iterable<? extends T> iterable, @NotNull Condition<? super T> condition) {
    return find(iterable.iterator(), condition);
  }

  public static <T> T find(@NotNull Iterator<? extends T> iterator, @NotNull Condition<? super T> condition) {
    while (iterator.hasNext()) {
      T value = iterator.next();
      if (condition.value(value)) return value;
    }
    return null;
  }

  public static <T> T find(@NotNull T[] array, @NotNull Condition<? super T> condition) {
    for (T value : array) {
      if (condition.value(value)) return value;
    }
    return null;
  }

  @NotNull
  @Contract(pure = true)
  public static <T> T[] filter(@NotNull T[] collection, @NotNull Condition<? super T> condition, @NotNull Class<? extends T[]> type) {
    List<T> result = new ArrayList<>();
    for (T t : collection) {
      if (condition.value(t)) {
        result.add(t);
      }
    }
    return Arrays.copyOf(result.toArray(), result.size(), type);
  }

  @NotNull
  @Contract(pure = true)
  public static <T, R> R[] map(@NotNull T[] collection, @NotNull Function<? super T, R> func, @NotNull Class<? extends R[]> newType) {
    List<R> result = new ArrayList<>();
    for (T t : collection) {
      result.add(func.apply(t));
    }
    return Arrays.copyOf(result.toArray(), result.size(), newType);
  }

  @Nullable
  @Contract("!null -> !null")
  public static Document toDocument(@Nullable BsonDocument bson) {
    return bson == null ? null : getDefaultCodecRegistry().get(Document.class).decode(bson.asBsonReader(), DecoderContext.builder().build());
  }

  @Nullable
  @Contract("!null -> !null")
  public static Object decode(@Nullable BsonValue bson) {
    return bson == null ? null :
           getDefaultCodecRegistry().get(Document.class).decode(new BsonDocument("v", bson).asBsonReader(), DecoderContext.builder().build()).get("v");
  }

  @Contract(value = "null, _ -> null", pure = true)
  @Nullable
  public static <T> T tryCast(@Nullable Object obj, @NotNull Class<T> clazz) {
    if (clazz.isInstance(obj)) {
      return clazz.cast(obj);
    }
    return null;
  }

  @Contract(pure=true)
  public static <T> boolean all(@NotNull Collection<? extends T> collection, @NotNull Function<? super T, Boolean> predicate) {
    for (T v : collection) {
      if (!predicate.apply(v)) return false;
    }
    return true;
  }

  @NotNull
  @Contract(pure=true)
  public static <T,V> List<V> map(@NotNull Collection<? extends T> collection, @NotNull Function<? super T, ? extends V> mapping) {
    if (collection.isEmpty()) return emptyList();
    List<V> list = new ArrayList<>(collection.size());
    for (final T t : collection) {
      list.add(mapping.apply(t));
    }
    return list;
  }

  @NotNull
  @Contract(pure=true)
  public static <T, V> List<V> mapNotNull(@NotNull Collection<? extends T> collection, @NotNull Function<? super T, ? extends V> mapping) {
    if (collection.isEmpty()) {
      return emptyList();
    }

    List<V> result = new ArrayList<>(collection.size());
    for (T t : collection) {
      final V o = mapping.apply(t);
      if (o != null) {
        result.add(o);
      }
    }
    return result.isEmpty() ? emptyList() : result;
  }


  @NotNull
  public static String insertCredentials(@NotNull String uri, @Nullable String username, @Nullable String password) {
    return insertCredentials(uri, username, password, true);
  }

  @NotNull
  public static String insertAuthMechanism(@NotNull String uri, @Nullable String authMechanism) {
    if (authMechanism == null) return uri;
    AuthenticationMechanism mechanism;
    try {
      mechanism = AuthenticationMechanism.fromMechanismName(authMechanism);
    } catch (IllegalArgumentException ignored) {
      return uri;
    }
    return insertUrlParameter(uri, AUTH_MECHANISM, mechanism.getMechanismName());
  }

  @NotNull
  public static String insertRetryWrites(@NotNull String uri, @Nullable String retryWrites) {
    if (retryWrites == null) return uri;
    String lowercase = retryWrites.toLowerCase(Locale.ENGLISH);
    return lowercase.equals("true") || lowercase.equals("false") ? insertUrlParameter(uri, RETRY_WRITES, retryWrites) : uri;
  }

  @NotNull
  public static String insertAuthSource(@NotNull String uri, @Nullable String source) {
    return insertUrlParameter(uri, AUTH_SOURCE, source);
  }

  @NotNull
  public static String insertUrlParameter(@NotNull String uri, @NotNull String key, @Nullable String value) {
    if (value == null || value.isEmpty()) return uri;
    Pair<String, String> pair = splitPrefix(uri);
    String prefix = pair.getFirst();
    String uriWithoutPrefix = pair.getSecond();

    Pair<String, String> pair2 = splitUriAndParameters(uriWithoutPrefix);
    String uriWithoutParameters = pair2.getFirst();
    String parameters = pair2.getSecond();

    if (!parameters.contains(key)) {
      parameters = parameters.isEmpty() || parameters.endsWith("&") ? parameters : parameters + "&";
      parameters += key + "=" + value;
    }
    return prefix + uriWithoutParameters + parameters;
  }

  @NotNull
  private static Pair<String, String> splitUriAndParameters(@NotNull String uriWithoutPrefix) {
    int idx = uriWithoutPrefix.indexOf("?");
    String uriWithoutParameters = idx == -1 ? uriWithoutPrefix : uriWithoutPrefix.substring(0, idx + 1);
    if (!uriWithoutParameters.endsWith("?")) {
      uriWithoutParameters += uriWithoutParameters.contains("/") ? "?" : "/?";
    }
    String parameters = idx == -1 ? "" : uriWithoutPrefix.substring(idx + 1);
    return new Pair<>(uriWithoutParameters, parameters);
  }

  @NotNull
  public static String insertAuthProperty(@NotNull String uri, @NotNull String name, @Nullable String value) {
    if (value == null || value.isEmpty()) return uri;

    Pair<String, String> pair = splitPrefix(uri);
    String prefix = pair.getFirst();
    String uriWithoutPrefix = pair.getSecond();

    Pair<String, String> pair2 = splitUriAndParameters(uriWithoutPrefix);
    String uriWithoutParameters = pair2.getFirst();
    String parameters = pair2.getSecond();
    String key = "authMechanismProperties=";
    String keyAndValue = find(parameters.split("&"), param -> param.startsWith(key));
    String mechanismProperties = keyAndValue == null ? "" : keyAndValue.split("=")[1];
    String parametersWithoutProperties = String.join("&", filter(parameters.split("&"), param -> !param.isEmpty() && !param.startsWith(key), String[].class));
    mechanismProperties = mechanismProperties.isEmpty()
            ? name + ":" + value
            : mechanismProperties.contains(name)
            ? mechanismProperties
            : mechanismProperties + (mechanismProperties.endsWith(",") ? "" : ",") + name + ":" + value;
    return prefix + uriWithoutParameters + parametersWithoutProperties + (parametersWithoutProperties.isEmpty() ? "" : "&") + key + mechanismProperties;
  }

  @NotNull
  public static String insertCredentials(@NotNull String uri, @Nullable String username, @Nullable String password, boolean automaticEncoding) {
    if (username == null) {
      if (password != null) {
        System.err.println("WARNING: Password is ignored because username is not specified");
      }
      return uri;
    }
    Pair<String, String> pair = splitPrefix(uri);
    String prefix = pair.getFirst();
    String uriWithoutPrefix = pair.getSecond();

    int idx = uriWithoutPrefix.indexOf("/");
    String userAndHostInformation = idx == -1 ? uriWithoutPrefix : uriWithoutPrefix.substring(0, idx);
    if (userAndHostInformation.contains("@")) return uri;

    String passwordPart = password == null ? "" : ":" + encode(password, automaticEncoding, "password");
    return prefix + encode(username, automaticEncoding, "username") + passwordPart + "@" + uriWithoutPrefix;
  }

  private static Pair<String, String> splitPrefix(@NotNull String uri) {
    boolean jdbc = false;
    if (uri.startsWith("jdbc:")) {
      jdbc = true;
      uri = uri.substring("jdbc:".length());
    }
    if (uri.startsWith(MONGODB_SRV_PREFIX)) {
      return new Pair<>((jdbc ? "jdbc:" : "") + MONGODB_SRV_PREFIX, uri.substring(MONGODB_SRV_PREFIX.length()));
    } else if (uri.startsWith(MONGODB_PREFIX)) {
      return new Pair<>((jdbc ? "jdbc:" : "") + MONGODB_PREFIX, uri.substring(MONGODB_PREFIX.length()));
    }
    throw new IllegalArgumentException("No valid prefix in uri: " + uri);
  }

  private static String encode(String text, boolean automaticEncoding, String what) {
    if (!automaticEncoding) return text;
    boolean shouldEncode = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (dontNeedEncoding.get(c)) continue;
      if (c == '%' && i + 2 < text.length() && isEncodedCharId(text.charAt(i + 1)) && isEncodedCharId(text.charAt(i + 2))) {
        continue;
      }
      shouldEncode = true;
      break;
    }
    if (!shouldEncode) return text;

    System.err.println("WARNING: " + what + " was automatically url-encoded. To turn it off set " + ENCODE_CREDENTIALS + " driver property to false.");
    try {
      return URLEncoder.encode(text, "UTF-8").replaceAll("\\+", "%20");
    }
    catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return text;
  }

  private static boolean isEncodedCharId(char c) {
    return c >= '0' && c <= '9' ||
        c >= 'a' && c <= 'f' ||
        c >= 'A' && c <= 'F';
  }

  @NotNull
  public static String trimEnd(@NotNull String str, char end) {
    int i = str.length() - 1;
    while (i >= 0 && str.charAt(i) == end) i--;
    return str.substring(0, i + 1);
  }

  public static boolean isTrue(@Nullable String value) {
    return value != null && (value.equals("1") || value.toLowerCase(Locale.ENGLISH).equals("true"));
  }

  public static boolean isNullOrEmpty(@Nullable String value) {
    return value == null || value.isEmpty();
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

  @NotNull
  public static ThreadFactory newNamedThreadFactory(@NonNls @NotNull final String name) {
    return r -> {
      Thread thread = new Thread(r, name);
      thread.setDaemon(true);
      return thread;
    };
  }
}
