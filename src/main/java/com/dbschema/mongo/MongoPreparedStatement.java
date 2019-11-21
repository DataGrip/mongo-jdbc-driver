package com.dbschema.mongo;

import com.dbschema.mongo.nashorn.JMongoCollection;
import com.dbschema.mongo.nashorn.JMongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.HashMap;
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

  @Override
  public ResultSet executeQuery(String query) throws SQLException {
    checkClosed();
    if (lastResultSet != null) {
      lastResultSet.close();
    }
    if (query == null) {
      throw new SQLException("Null statement.");
    }
    return lastResultSet = connection.getScriptEngine().execute(query, fetchSize);
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
    for (JMongoDatabase scan : connection.getService().getDatabases()) {
      if (scan.getName().equalsIgnoreCase(name)) {
        return scan;
      }
    }
    if ("db".equals(name) && connection.getSchema() != null) {
      for (JMongoDatabase scan : connection.getService().getDatabases()) {
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
          JMongoCollection collection = getCollectionMandatory(matcher.group(1), true);
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
          JMongoCollection collection = getCollectionMandatory(matcher.group(1), false);
          HashMap<String, Object> m = new HashMap<>();
          m.put("_id", id);
          collection.deleteOne(m);
          return 1;
        }
      }
    }
    throw new SQLException(ERROR_MESSAGE);
  }

  private static final Pattern PATTERN_DB_IDENTIFIER = Pattern.compile("client\\.getDatabase\\('(.*)'\\).(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern PATTERN_COLLECTION_IDENTIFIER = Pattern.compile("getCollection\\('(.*)'\\).(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern PATTERN_DOT = Pattern.compile("(.*)\\.(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);


  private JMongoCollection getCollectionMandatory(String collectionRef, boolean createCollectionIfMissing) throws SQLException {
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
    JMongoCollection collection = mongoDatabase.getCollection(collectionRef);
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


