package com.dbschema.mongo.nashorn;

import com.dbschema.mongo.MongoConnection;
import com.dbschema.mongo.MongoScriptEngine;
import com.dbschema.mongo.MongoService;
import com.dbschema.mongo.SQLAlreadyClosedException;
import com.dbschema.mongo.resultSet.AggregateResultSet;
import com.dbschema.mongo.resultSet.ListResultSet;
import com.dbschema.mongo.resultSet.ResultSetIterator;
import com.mongodb.AggregationOutput;
import com.mongodb.client.MongoIterable;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.bson.Document;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbschema.mongo.Util.ok;

/**
 * @author Liudmila Kornilova
 **/
public class MongoNashornScriptEngine implements MongoScriptEngine {
  private static final Pattern PATTERN_USE_DATABASE = Pattern.compile("USE\\s+(.*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_CREATE_DATABASE = Pattern.compile("CREATE\\s+DATABASE\\s*'(.*)'\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_DATABASES = Pattern.compile("SHOW\\s+DATABASES\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_DBS = Pattern.compile("SHOW\\s+DBS\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_COLLECTIONS = Pattern.compile("SHOW\\s+COLLECTIONS\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_USERS = Pattern.compile("SHOW\\s+USERS\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_RULES = Pattern.compile("SHOW\\s+RULES\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_PROFILE = Pattern.compile("SHOW\\s+PROFILE\\s*", Pattern.CASE_INSENSITIVE);

  @Language("JavaScript")
  private static final String STARTUP_SCRIPT = "var ObjectId = function(oid) { return new org.bson.types.ObjectId(oid);}\n" +
      "var ISODate = function(str) { return str === undefined || str === null ? com.dbschema.mongo.nashorn.JMongoUtil.now() : com.dbschema.mongo.nashorn.JMongoUtil.parseDate(str); }\n" +
      "var UUID = function(str) { return str === undefined || str === null ? java.util.UUID.randomUUID() : java.util.UUID.fromString(str) }\n" +
      "var BinData = function(type, data) { return com.dbschema.mongo.nashorn.JMongoUtil.binData(type === undefined ? null : type, data === undefined ? null : data); }\n" +
      "var NumberLong = function(number) { return com.dbschema.mongo.nashorn.JMongoUtil.numberLong(number === undefined ? null : number); }\n" +
      "var NumberInt = function(number) { return com.dbschema.mongo.nashorn.JMongoUtil.numberInt(number === undefined ? null : number); }\n" +
      "var NumberDecimal = function(number) { return com.dbschema.mongo.nashorn.JMongoUtil.numberDecimal(number === undefined ? null : number); }\n" +
      "var MinKey = function() { return new org.bson.types.MinKey(); }\n" +
      "var MaxKey = function() { return new org.bson.types.MaxKey(); }\n";

  private final MongoConnection connection;
  private final boolean useEs6;
  private ScriptEngine engine;

  public MongoNashornScriptEngine(@NotNull MongoConnection connection, boolean useEs6) {
    this.connection = connection;
    this.useEs6 = useEs6;
  }

  private ScriptEngine getEngine() throws SQLException {
    if (engine == null) {
      try {
        Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory");
      }
      catch (ClassNotFoundException ex) {
        throw new SQLException(
            "Error: Java 1.8 or later from Oracle is required.\n" +
                "MongoDb JDBC driver uses the Nashorn JavaScript engine delivered with Java.\n" +
                "Check in DbSchema Help/About Dialog the current Java version.\n" +
                "Cause : " + ex);
      }
      try {
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        String[] args = useEs6 ? new String[]{"--language=es6"} : new String[0];
        engine = factory.getScriptEngine(args);
        engine.eval(STARTUP_SCRIPT);
      }
      catch (Throwable t) {
        throw new SQLException(t);
      }
    }
    updateBindings(engine);
    return engine;
  }

  private void updateBindings(@NotNull ScriptEngine engine) throws SQLAlreadyClosedException {
    final Bindings binding = engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
    for (JMongoDatabase db : connection.getService().getDatabases()) {
      binding.put(db.getName(), db);
    }
    String currentDatabase = connection.getSchema();
    if (currentDatabase != null) {
      binding.put("db", connection.getService().getDatabase(currentDatabase));
    }
  }

  @Nullable
  @Override
  public ResultSet execute(@NotNull String query, int fetchSize) throws SQLException {
    Matcher matcherSetDb = PATTERN_USE_DATABASE.matcher(query);
    if (matcherSetDb.matches()) {
      String db = matcherSetDb.group(1).trim();
      if ((db.startsWith("\"") && db.endsWith("\"")) || (db.startsWith("'") && db.endsWith("'"))) {
        db = db.substring(1, db.length() - 1);
      }
      connection.setSchema(db);
      return ok();
    }
    Matcher matcherCreateDatabase = PATTERN_CREATE_DATABASE.matcher(query);
    if (matcherCreateDatabase.matches()) {
      final String dbName = matcherCreateDatabase.group(1);
      connection.getService().getDatabase(dbName);
      MongoService.createdDatabases.add(dbName);
      return ok();
    }
    if (query.toLowerCase().startsWith("show ")) {
      if (PATTERN_SHOW_DATABASES.matcher(query).matches() || PATTERN_SHOW_DBS.matcher(query).matches()) {
        ListResultSet result = new ListResultSet();
        result.setColumnNames("DATABASE_NAME");
        for (String str : connection.getService().getDatabaseNames()) {
          result.addRow(new String[]{str});
        }
        return result;
      }
      if (PATTERN_SHOW_COLLECTIONS.matcher(query).matches()) {
        ListResultSet result = new ListResultSet();
        result.setColumnNames("COLLECTION_NAME");
        for (String str : connection.getService().getCollectionNames(connection.getSchema())) {
          result.addRow(new String[]{str});
        }
        return result;
      }
      if (PATTERN_SHOW_USERS.matcher(query).matches()) {
        query = "db.runCommand(\"{usersInfo:'" + connection.getSchema() + "'}\")";
      }
      if (PATTERN_SHOW_PROFILE.matcher(query).matches() || PATTERN_SHOW_RULES.matcher(query).matches()) {
        throw new SQLException("Not yet implemented in this driver.");
      }
      throw new SQLException("Invalid command : " + query);
    }

    try {
      Object obj = getEngine().eval(query);
      if (obj == Undefined.INSTANCE) {
        return null;
      }
      else if (obj instanceof Iterable) {
        if (fetchSize > 1) {
          if (obj instanceof JFindIterable) ((JFindIterable) obj).batchSize(fetchSize);
          else if (obj instanceof JAggregateIterable) ((JAggregateIterable) obj).batchSize(fetchSize);
          else if (obj instanceof MongoIterable<?>) ((MongoIterable<?>) obj).batchSize(fetchSize);
        }
        return new ResultSetIterator(((Iterable<?>) obj).iterator());
      }
      else if (obj instanceof Iterator) {
        return new ResultSetIterator((Iterator<?>) obj);
      }
      else if (obj instanceof AggregationOutput) {
        return new AggregateResultSet((AggregationOutput) obj);
      }
      else if (obj instanceof JMongoCollection) {
        return new ResultSetIterator(((JMongoCollection) obj).find());
      }
      else if (obj instanceof ResultSet) {
        return (ResultSet) obj;
      }
      else if (obj instanceof Document) {
        return new ResultSetIterator(Collections.singletonList(obj));
      }
      return ok(obj);
    }
    catch (Throwable ex) {
      ex.printStackTrace();
      throw new SQLException(ex.getMessage());
    }
  }
}
