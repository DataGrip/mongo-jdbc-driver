package com.dbschema.mongo.nashorn;

import com.dbschema.mongo.MongoConnection;
import com.dbschema.mongo.MongoService;
import com.dbschema.mongo.ScriptEngine;
import com.dbschema.mongo.resultSet.AggregateResultSet;
import com.dbschema.mongo.resultSet.ListResultSet;
import com.dbschema.mongo.resultSet.ResultSetIterator;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoIterable;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbschema.mongo.Util.ok;

/**
 * @author Liudmila Kornilova
 **/
public class NashornScriptEngine implements ScriptEngine {
  private static final Pattern PATTERN_USE_DATABASE = Pattern.compile("USE\\s+(.*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_CREATE_DATABASE = Pattern.compile("CREATE\\s+DATABASE\\s*'(.*)'\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_DATABASES = Pattern.compile("SHOW\\s+DATABASES\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_DBS = Pattern.compile("SHOW\\s+DBS\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_COLLECTIONS = Pattern.compile("SHOW\\s+COLLECTIONS\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_USERS = Pattern.compile("SHOW\\s+USERS\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_RULES = Pattern.compile("SHOW\\s+RULES\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_PROFILE = Pattern.compile("SHOW\\s+PROFILE\\s*", Pattern.CASE_INSENSITIVE);

  private final MongoConnection connection;

  public NashornScriptEngine(@NotNull MongoConnection connection) {
    this.connection = connection;
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
      NashornScriptEngineFactory nsef = new NashornScriptEngineFactory();
      javax.script.ScriptEngine engine = nsef.getScriptEngine(BasicDBObject.class.getClassLoader());
      boolean dbIsSet = false;
      final Bindings binding = engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
      for (JMongoDatabase db : connection.getService().getDatabases()) {
        binding.put(db.getName(), db);
        if (connection.getSchema() != null && connection.getSchema().equals(db.getName())) {
          binding.put("db", db);
          dbIsSet = true;
        }
      }
      if (!dbIsSet) {
        String currentSchemaName = connection.getSchema();
        binding.put("db", connection.getService().getDatabase(currentSchemaName));
      }
      binding.put("client", connection);
      final String script = "var ObjectId = function( oid ) { return new org.bson.types.ObjectId( oid );}\n" +
          "var ISODate = function( str ) { return new java.text.SimpleDateFormat(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\").parse(str);}";
      engine.eval(script);
      Object obj = engine.eval(query);
      if (obj instanceof Iterable) {
        if (obj instanceof MongoIterable) ((MongoIterable<?>) obj).batchSize(fetchSize);
        return new ResultSetIterator(((Iterable<?>) obj).iterator());
      }
      else if (obj instanceof Iterator) {
        return new ResultSetIterator((Iterator<?>) obj);
      }
      else if (obj instanceof AggregationOutput) {
        return new AggregateResultSet((AggregationOutput) obj);
      }
      else if (obj instanceof JMongoCollection) {
        return new ResultSetIterator(((JMongoCollection<?>) obj).find());
      }
      else if (obj instanceof ResultSet) {
        return (ResultSet) obj;
      }
      return null;
    }
    catch (Throwable ex) {
      ex.printStackTrace();
      throw new SQLException(ex.getMessage());
    }
  }
}
