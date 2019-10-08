package com.dbschema.mongo;

import com.dbschema.mongo.java.JMongoDatabase;
import com.dbschema.mongo.java.Service;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class MongoConnection implements Connection {
  private String schema;
  private Service service;
  private boolean isClosed = false;
  private boolean isReadOnly = false;


  public MongoConnection(Service service) {
    this.service = service;
    setSchema(service.getDatabaseNameFromUrl());
  }

  public String getCatalog() {
    return null;
  }

  public Service getService() {
    return service;
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    checkClosed();
    return null;
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    checkClosed();
    return false;
  }

  /**
   * @see java.sql.Connection#createStatement()
   */
  @Override
  public Statement createStatement() throws SQLException {
    checkClosed();
    return new MongoPreparedStatement(this);
  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
    checkClosed();
    return new MongoPreparedStatement(this);
  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    checkClosed();
    return new MongoPreparedStatement(this);
  }


  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException {
    checkClosed();
    return new MongoPreparedStatement(this, sql);
  }

  @Override
  public CallableStatement prepareCall(String sql) throws SQLException {
    checkClosed();
    return null;
  }

  /**
   * @see java.sql.Connection#nativeSQL(java.lang.String)
   */
  @Override
  public String nativeSQL(String sql) throws SQLException {
    checkClosed();
    throw new UnsupportedOperationException("MongoDB does not support SQL natively.");
  }

  /**
   * @see java.sql.Connection#setAutoCommit(boolean)
   */
  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    checkClosed();
  }

  /**
   * @see java.sql.Connection#getAutoCommit()
   */
  @Override
  public boolean getAutoCommit() throws SQLException {
    checkClosed();
    return true;
  }

  @Override
  public void commit() throws SQLException {
    checkClosed();
  }

  @Override
  public void rollback() throws SQLException {
    checkClosed();
  }

  @Override
  public void close() {
    isClosed = true;
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }


  private final MongoDatabaseMetaData metaData = new MongoDatabaseMetaData(this);

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    checkClosed();
    return metaData;
  }

  @Override
  public void setReadOnly(boolean readOnly) throws SQLException {
    checkClosed();
    isReadOnly = readOnly;
  }

  /**
   * @see java.sql.Connection#isReadOnly()
   */
  @Override
  public boolean isReadOnly() throws SQLException {
    checkClosed();
    return isReadOnly;
  }

  @Override
  public void setCatalog(String catalog) {
  }

  @Override
  public void setTransactionIsolation(int level) throws SQLException {
    checkClosed();
    // Since the only valid value for MongDB is Connection.TRANSACTION_NONE, and the javadoc for this method
    // indicates that this is not a valid value for level here, throw unsupported operation exception.
    throw new UnsupportedOperationException("MongoDB provides no support for transactions.");
  }

  /**
   * @see java.sql.Connection#getTransactionIsolation()
   */
  @Override
  public int getTransactionIsolation() throws SQLException {
    checkClosed();
    return Connection.TRANSACTION_NONE;
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
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) {
    return null;
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) {
    return null;
  }

  @Override
  public Map<String, Class<?>> getTypeMap() {
    return null;
  }

  @Override
  public void setTypeMap(Map<String, Class<?>> map) {
  }

  @Override
  public void setHoldability(int holdability) {
  }

  @Override
  public int getHoldability() {
    return 0;
  }

  @Override
  public Savepoint setSavepoint() {
    return null;
  }

  @Override
  public Savepoint setSavepoint(String name) {
    return null;
  }

  @Override
  public void rollback(Savepoint savepoint) {
  }

  @Override
  public void releaseSavepoint(Savepoint savepoint) {
  }


  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                                            int resultSetHoldability) {
    return null;
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                                       int resultSetHoldability) {
    return null;
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) {
    return null;
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) {
    return null;
  }

  @Override
  public PreparedStatement prepareStatement(String sql, String[] columnNames) {
    return null;
  }

  @Override
  public Clob createClob() {
    return null;
  }

  @Override
  public Blob createBlob() {
    return null;
  }

  @Override
  public NClob createNClob() throws SQLException {
    checkClosed();
    return null;
  }

  @Override
  public SQLXML createSQLXML() throws SQLException {
    checkClosed();
    return null;
  }

  /**
   * @see java.sql.Connection#isValid(int)
   */
  @Override
  public boolean isValid(int timeout) throws SQLException {
    checkClosed();
    return true;
  }

  /**
   * @see java.sql.Connection#setClientInfo(java.lang.String, java.lang.String)
   */
  @Override
  public void setClientInfo(String name, String value) {
    /* MongoDB does not support setting client information in the database. */
  }

  /**
   * @see java.sql.Connection#setClientInfo(java.util.Properties)
   */
  @Override
  public void setClientInfo(Properties properties) {
    /* MongoDB does not support setting client information in the database. */
  }

  /**
   * @see java.sql.Connection#getClientInfo(java.lang.String)
   */
  @Override
  public String getClientInfo(String name) throws SQLException {
    checkClosed();
    return null;
  }

  /**
   * @see java.sql.Connection#getClientInfo()
   */
  @Override
  public Properties getClientInfo() throws SQLException {
    checkClosed();
    return null;
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    checkClosed();

    return null;
  }

  /**
   * @see java.sql.Connection#createStruct(java.lang.String, java.lang.Object[])
   */
  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    checkClosed();

    return null;
  }

  public String getUrl() {
    return service.getURI();
  }

  public List<JMongoDatabase> getDatabases() {
    return service.getDatabases();
  }

  public List<String> getDatabaseNames() {
    return service.getDatabaseNames();
  }

  public JMongoDatabase getDatabase(String name) {
    return service.getDatabase(name);
  }


  private void checkClosed() throws SQLException {
    if (isClosed) {
      throw new SQLException("Statement was previously closed.");
    }
  }

  @Override
  public void setSchema(String schema) {
    this.schema = schema;
  }

  @Override
  public String getSchema() {
    return schema;
  }

  @Override
  public void abort(Executor executor) {
  }

  @Override
  public void setNetworkTimeout(Executor executor, int milliseconds) {
  }

  @Override
  public int getNetworkTimeout() {
    return 0;
  }

}
