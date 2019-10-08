package com.dbschema.mongo;

import com.dbschema.mongo.java.JMongoCollection;
import com.dbschema.mongo.java.JMongoDatabase;
import com.dbschema.mongo.java.MongoJService;
import com.dbschema.mongo.resultSet.AggregateResultSet;
import com.dbschema.mongo.resultSet.ArrayResultSet;
import com.dbschema.mongo.resultSet.OkResultSet;
import com.dbschema.mongo.resultSet.ResultSetIterator;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.UpdateOptions;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.bson.Document;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MongoPreparedStatement implements PreparedStatement {

  private final MongoConnection connection;
  private ResultSet lastResultSet;
  private boolean isClosed = false;
  private int maxRows = -1;
  private final String query;
  private int fetchSize = -1;

  public MongoPreparedStatement(final MongoConnection connection) {
    this.connection = connection;
    this.query = null;
  }

  public MongoPreparedStatement(final MongoConnection connection, String query) {
    this.connection = connection;
    this.query = query;
  }

  @Override
  public <T> T unwrap(final Class<T> iface) {
    return null;
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) {
    return false;
  }


  private static final Pattern PATTERN_USE_DATABASE = Pattern.compile("USE\\s+(.*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_CREATE_DATABASE = Pattern.compile("CREATE\\s+DATABASE\\s*'(.*)'\\s*", Pattern.CASE_INSENSITIVE);

  private static final Pattern PATTERN_SHOW_DATABASES = Pattern.compile("SHOW\\s+DATABASES\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_DBS = Pattern.compile("SHOW\\s+DBS\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_COLLECTIONS = Pattern.compile("SHOW\\s+COLLECTIONS\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_USERS = Pattern.compile("SHOW\\s+USERS\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_RULES = Pattern.compile("SHOW\\s+RULES\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_SHOW_PROFILE = Pattern.compile("SHOW\\s+PROFILE\\s*", Pattern.CASE_INSENSITIVE);

  @SuppressWarnings("deprecation")
  @Override
  public ResultSet executeQuery(String query) throws SQLException {
    checkClosed();
    if (lastResultSet != null) {
      lastResultSet.close();
    }
    if (query == null) {
      throw new SQLException("Null statement.");
    }
    Matcher matcherSetDb = PATTERN_USE_DATABASE.matcher(query);
    if (matcherSetDb.matches()) {
      String db = matcherSetDb.group(1).trim();
      if ((db.startsWith("\"") && db.endsWith("\"")) || (db.startsWith("'") && db.endsWith("'"))) {
        db = db.substring(1, db.length() - 1);
      }
      connection.setSchema(db);
      return new OkResultSet();
    }
    Matcher matcherCreateDatabase = PATTERN_CREATE_DATABASE.matcher(query);
    if (matcherCreateDatabase.matches()) {
      final String dbName = matcherCreateDatabase.group(1);
      connection.getJService().getDatabase(dbName);
      MongoJService.createdDatabases.add(dbName);
      return new OkResultSet();
    }
    if (query.toLowerCase().startsWith("show ")) {
      if (PATTERN_SHOW_DATABASES.matcher(query).matches() || PATTERN_SHOW_DBS.matcher(query).matches()) {
        ArrayResultSet result = new ArrayResultSet();
        result.setColumnNames(new String[]{"DATABASE_NAME"});
        for (String str : connection.getJService().getDatabaseNames()) {
          result.addRow(new String[]{str});
        }
        return lastResultSet = result;
      }
      if (PATTERN_SHOW_COLLECTIONS.matcher(query).matches()) {
        ArrayResultSet result = new ArrayResultSet();
        result.setColumnNames(new String[]{"COLLECTION_NAME"});
        for (String str : connection.getJService().getCollectionNames(connection.getSchema())) {
          result.addRow(new String[]{str});
        }
        return lastResultSet = result;
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
      ScriptEngine engine = nsef.getScriptEngine(BasicDBObject.class.getClassLoader());
      boolean dbIsSet = false;
      final Bindings binding = engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
      for (JMongoDatabase db : connection.getJService().getDatabases()) {
        binding.put(db.getName(), db);
        if (connection.getSchema() != null && connection.getSchema().equals(db.getName())) {
          binding.put("db", db);
          dbIsSet = true;
        }
      }
      if (!dbIsSet) {
        String currentSchemaName = connection.getSchema();
        binding.put("db", connection.getJService().getDatabase(currentSchemaName));
      }
      binding.put("client", connection);
      final String script = "var ObjectId = function( oid ) { return new org.bson.types.ObjectId( oid );}\n" +
          "var ISODate = function( str ) { return new java.text.SimpleDateFormat(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\").parse(str);}";
      engine.eval(script);
      Object obj = engine.eval(query);
      if (obj instanceof Iterable) {
        if (obj instanceof MongoIterable) ((MongoIterable<?>) obj).batchSize(fetchSize);
        lastResultSet = new ResultSetIterator(((Iterable<?>) obj).iterator());
      }
      else if (obj instanceof Iterator) {
        lastResultSet = new ResultSetIterator((Iterator<?>) obj);
      }
      else if (obj instanceof AggregationOutput) {
        lastResultSet = new AggregateResultSet((AggregationOutput) obj);
      }
      else if (obj instanceof JMongoCollection) {
        lastResultSet = new ResultSetIterator(((JMongoCollection<?>) obj).find());
      }
      else if (obj instanceof ResultSet) {
        lastResultSet = (ResultSet) obj;
      }
      return lastResultSet;
    }
    catch (Throwable ex) {
      ex.printStackTrace();
      throw new SQLException(ex.getMessage());
    }


        /*
        // https://dzone.com/articles/groovys-smooth-operators

        final Binding binding = new Binding();
        System.out.println("Connection catalog=" + con.getCatalog() );
        for ( JMongoDatabase db : con.getDatabases() ){
            binding.setProperty( db.getName(), db );
            if ( con.getCatalog() != null && con.getCatalog().equals(db.getName())){
                binding.setProperty( "db", db );
            }
        }

        final GroovyShell shell = new GroovyShell( NoSqlPreparedStatement.class.getClassLoader(), binding, GroovyConfiguration.CONFIG );
        final Script script = shell.parse( sql );
        Object obj = script.run();

        if ( obj instanceof Iterable){
            lastResultSet = new NoSqlIteratorResultSet( ((Iterable)obj).iterator() );
        } else if ( obj instanceof Iterator){
            lastResultSet = new NoSqlIteratorResultSet( (Iterator)obj );
        } else if ( obj instanceof AggregationOutput ){
            lastResultSet = new NoSqlAggregateResultSet( (AggregationOutput)obj );
        }
        return lastResultSet;
        */
  }


  @Override
  public boolean execute(final String query) throws SQLException {
    executeQuery(query);
    return lastResultSet != null;
  }

  private Document documentParam;

  @Override
  public void setObject(int parameterIndex, Object x) throws SQLException {
    if (x instanceof Document) {
      documentParam = (Document) x;
    }
    else if (x instanceof Map) {
      //noinspection unchecked
      documentParam = new Document((Map<String, Object>) x);
    }
    else if (x != null) {
      throw new SQLException("Map object expected. You currently did setObject( " + x.getClass().getName() + " ) ");
    }
    else {
      throw new SQLException("Map object expected. You currently did setObject( NULL ) ");
    }
  }

  @Override
  public int executeUpdate() throws SQLException {
    return executeUpdate(query);
  }

  private JMongoDatabase getDatabase(String name) {
    for (JMongoDatabase scan : connection.getJService().getDatabases()) {
      if (scan.getName().equalsIgnoreCase(name)) {
        return scan;
      }
    }
    if ("db".equals(name) && connection.getSchema() != null) {
      for (JMongoDatabase scan : connection.getJService().getDatabases()) {
        if (scan.getName().equalsIgnoreCase(connection.getSchema())) {
          return scan;
        }
      }
    }
    return null;
  }

  private static final Pattern PATTERN_UPDATE = Pattern.compile("UPDATE\\s+(.*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_DELETE = Pattern.compile("DELETE\\s+FROM\\s+(.*)", Pattern.CASE_INSENSITIVE);
  private static final String ERROR_MESSAGE = "Allowed statements: update(<dbname>.<collectionName>) or delete(<dbname>.<collectionName>). Before calling this do setObject(0,<dbobject>).";

  @Override
  public int executeUpdate(String sql) throws SQLException {
    if (sql != null) {
      if (documentParam == null) {
        // IF HAS NO PARAMETERS, EXECUTE AS NORMAL SQL
        execute(sql);
        return 1;
      }
      else {
        sql = sql.trim();
        Matcher matcher = PATTERN_UPDATE.matcher(sql);
        final Object id = documentParam.get("_id");
        if (matcher.matches()) {
          JMongoCollection<Document> collection = getCollectionMandatory(matcher.group(1), true);
          if (id == null) {
            collection.insertOne(documentParam);
          }
          else {
            collection.replaceOne(new Document("_id", id), documentParam, new UpdateOptions().upsert(true));
          }
          return 1;
        }
        matcher = PATTERN_DELETE.matcher(sql);
        if (matcher.matches()) {
          JMongoCollection<Document> collection = getCollectionMandatory(matcher.group(1), false);
          collection.deleteOne((Map<String, Object>) (new Document().append("_id", id)));
          return 1;
        }
      }
    }
    throw new SQLException(ERROR_MESSAGE);
  }

  private static final Pattern PATTERN_DB_IDENTIFIER = Pattern.compile("client\\.getDatabase\\('(.*)'\\).(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern PATTERN_COLLECTION_IDENTIFIER = Pattern.compile("getCollection\\('(.*)'\\).(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern PATTERN_DOT = Pattern.compile("(.*)\\.(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);


  private JMongoCollection<Document> getCollectionMandatory(String collectionRef, boolean createCollectionIfMissing) throws SQLException {
    JMongoDatabase mongoDatabase = null;
    Matcher matcherDbIdentifier = PATTERN_DB_IDENTIFIER.matcher(collectionRef);
    Matcher matcherDbDot = PATTERN_DOT.matcher(collectionRef);
    if (matcherDbIdentifier.matches()) {
      mongoDatabase = getDatabase(matcherDbIdentifier.group(1));
      collectionRef = matcherDbIdentifier.group(2);
    }
    else if (matcherDbDot.matches()) {
      mongoDatabase = getDatabase(matcherDbDot.group(1));
      collectionRef = matcherDbDot.group(2);
    }
    if (mongoDatabase == null) throw new SQLException("Cannot find database '" + collectionRef + "'.");
    Matcher matcherCollectionIdentifier = PATTERN_COLLECTION_IDENTIFIER.matcher(collectionRef);
    if (matcherCollectionIdentifier.matches()) {
      collectionRef = matcherDbIdentifier.group(1);
    }
    JMongoCollection<Document> collection = mongoDatabase.getCollection(collectionRef);
    if (collection == null && createCollectionIfMissing) {
      mongoDatabase.createCollection(collectionRef);
      collection = mongoDatabase.getCollection(collectionRef);
    }
    if (collection == null) throw new SQLException("Cannot find collection '" + collectionRef + "'.");
    return collection;
  }

  @Override
  public void close() throws SQLException {
    if (lastResultSet != null) {
      lastResultSet.close();
    }
    this.isClosed = true;
  }

  @Override
  public int getMaxFieldSize() {

    return 0;
  }

  @Override
  public void setMaxFieldSize(final int max) {
  }

  @Override
  public int getMaxRows() {
    return maxRows;
  }

  @Override
  public void setMaxRows(final int max) {
    this.maxRows = max;
  }

  @Override
  public void setEscapeProcessing(final boolean enable) {
  }

  @Override
  public int getQueryTimeout() throws SQLException {
    checkClosed();
    throw new SQLFeatureNotSupportedException("MongoDB provides no support for query timeouts.");
  }

  @Override
  public void setQueryTimeout(final int seconds) throws SQLException {
    checkClosed();
    throw new SQLFeatureNotSupportedException("MongoDB provides no support for query timeouts.");
  }

  @Override
  public void cancel() throws SQLException {
    checkClosed();
    throw new SQLFeatureNotSupportedException("MongoDB provides no support for interrupting an operation.");
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    checkClosed();
    return null;
  }

  @Override
  public void clearWarnings() throws SQLException {
    checkClosed();
  }

  @Override
  public void setCursorName(final String name) throws SQLException {
    checkClosed();
    // Driver doesn't support positioned updates for now, so no-op.
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    checkClosed();
    return lastResultSet;
  }

  @Override
  public int getUpdateCount() throws SQLException {
    checkClosed();
    return -1;
  }

  @Override
  public boolean getMoreResults() {
    return false;
  }

  @Override
  public void setFetchDirection(final int direction) {
  }

  @Override
  public int getFetchDirection() {
    return ResultSet.FETCH_FORWARD;
  }

  @Override
  public void setFetchSize(int rows) throws SQLException {
    if (rows <= 1) throw new SQLException("Fetch size must be > 1. Actual: " + rows);
    fetchSize = rows;
  }

  @Override
  public int getFetchSize() {
    return fetchSize;
  }

  @Override
  public int getResultSetConcurrency() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int getResultSetType() {
    return ResultSet.TYPE_FORWARD_ONLY;
  }

  @Override
  public void addBatch(final String sql) {
  }

  @Override
  public void clearBatch() {
  }

  @Override
  public int[] executeBatch() throws SQLException {
    checkClosed();
    return null;
  }

  @Override
  public Connection getConnection() throws SQLException {
    checkClosed();
    return this.connection;
  }

  @Override
  public boolean getMoreResults(final int current) throws SQLException {
    checkClosed();
    return false;
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    checkClosed();
    return null;
  }

  @Override
  public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
    checkClosed();
    return 0;
  }

  @Override
  public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
    checkClosed();
    return 0;
  }

  @Override
  public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
    checkClosed();
    return 0;
  }

  @Override
  public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
    checkClosed();
    return false;
  }

  @Override
  public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
    checkClosed();
    return false;
  }

  @Override
  public boolean execute(final String sql, final String[] columnNames) throws SQLException {
    checkClosed();
    return false;
  }

  @Override
  public int getResultSetHoldability() {
    return 0;
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }

  @Override
  public void setPoolable(final boolean poolable) {
  }

  @Override
  public boolean isPoolable() {
    return false;
  }

  private void checkClosed() throws SQLException {
    if (isClosed) {
      throw new SQLException("Statement was previously closed.");
    }
  }

  @Override
  public void closeOnCompletion() {
  }

  @Override
  public boolean isCloseOnCompletion() {
    return false;
  }

  @Override
  public ResultSet executeQuery() throws SQLException {
    execute(query);
    return lastResultSet;
  }

  @Override
  public void setNull(int parameterIndex, int sqlType) {

  }

  @Override
  public void setBoolean(int parameterIndex, boolean x) {

  }

  @Override
  public void setByte(int parameterIndex, byte x) {

  }

  @Override
  public void setShort(int parameterIndex, short x) {

  }

  @Override
  public void setInt(int parameterIndex, int x) {

  }

  @Override
  public void setLong(int parameterIndex, long x) {

  }

  @Override
  public void setFloat(int parameterIndex, float x) {

  }

  @Override
  public void setDouble(int parameterIndex, double x) {

  }

  @Override
  public void setBigDecimal(int parameterIndex, BigDecimal x) {

  }

  @Override
  public void setString(int parameterIndex, String x) {

  }

  @Override
  public void setBytes(int parameterIndex, byte[] x) {

  }

  @Override
  public void setDate(int parameterIndex, Date x) {

  }

  @Override
  public void setTime(int parameterIndex, Time x) {

  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x) {
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, int length) {
  }

  @Override
  public void setUnicodeStream(int parameterIndex, InputStream x, int length) {
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, int length) {
  }

  @Override
  public void clearParameters() {
  }

  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType) {
  }


  @Override
  public boolean execute() throws SQLException {
    return query != null && execute(query);
  }

  @Override
  public void addBatch() {

  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, int length) {

  }

  @Override
  public void setRef(int parameterIndex, Ref x) {

  }

  @Override
  public void setBlob(int parameterIndex, Blob x) {

  }

  @Override
  public void setClob(int parameterIndex, Clob x) {

  }

  @Override
  public void setArray(int parameterIndex, Array x) {

  }

  @Override
  public ResultSetMetaData getMetaData() {
    return null;
  }

  @Override
  public void setDate(int parameterIndex, Date x, Calendar cal) {

  }

  @Override
  public void setTime(int parameterIndex, Time x, Calendar cal) {

  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) {

  }

  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName) {

  }

  @Override
  public void setURL(int parameterIndex, URL x) {

  }

  @Override
  public ParameterMetaData getParameterMetaData() {
    return null;
  }

  @Override
  public void setRowId(int parameterIndex, RowId x) {

  }

  @Override
  public void setNString(int parameterIndex, String value) {

  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length) {

  }

  @Override
  public void setNClob(int parameterIndex, NClob value) {

  }

  @Override
  public void setClob(int parameterIndex, Reader reader, long length) {

  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length) {

  }

  @Override
  public void setNClob(int parameterIndex, Reader reader, long length) {

  }

  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) {

  }

  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) {

  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, long length) {

  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, long length) {

  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length) {

  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x) {

  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x) {

  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader) {

  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value) {

  }

  @Override
  public void setClob(int parameterIndex, Reader reader) {

  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream) {

  }

  @Override
  public void setNClob(int parameterIndex, Reader reader) {

  }
}


