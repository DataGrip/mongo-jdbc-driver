package com.dbschema.mongo.resultSet;

import com.dbschema.mongo.MongoPreparedStatement;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class ListResultSet implements ResultSet {
  private final List<Object[]> data;
  private String[] columnNames;
  private int currentRow = -1;
  private String tableName = null;
  private boolean isClosed = false;

  private MongoPreparedStatement statement = null;

  public ListResultSet() {
    this(new ArrayList<>(), new String[0]);
  }

  public ListResultSet(List<Object[]> data, String[] columnNames) {
    this.data = data;
    this.columnNames = columnNames;
  }

  public ListResultSet(Object[] row, String[] columnNames) {
    this.data = new ArrayList<>();
    this.data.add(row);
    this.columnNames = columnNames;
  }

  public void setColumnNames(String... columnNames) {
    this.columnNames = columnNames;
  }

  public void addRow(Object[] columnValues) {
    data.add(columnValues);
  }

  public <T> T unwrap(Class<T> iface) {
    return null;
  }

  public boolean isWrapperFor(Class<?> iface) {
    return false;
  }

  /**
   * @see java.sql.ResultSet#next()
   */
  public boolean next() {
    if (data == null) {
      return false;
    }
    if (currentRow < data.size() - 1) {
      currentRow++;
      return true;
    }
    return false;
  }

  /**
   * @see java.sql.ResultSet#close()
   */
  public void close() {
    this.isClosed = true;
  }

  /**
   * @see java.sql.ResultSet#wasNull()
   */
  public boolean wasNull() {

    return false;
  }

  public String getString(int columnIndex) throws SQLException {
    if (currentRow >= data.size()) {
      throw new SQLException("ResultSet exhausted, request currentRow = " + currentRow);
    }
    int adjustedColumnIndex = columnIndex - 1;
    if (adjustedColumnIndex >= data.get(currentRow).length) {
      throw new SQLException("Column index does not exist: " + columnIndex);
    }
    final Object val = data.get(currentRow)[adjustedColumnIndex];
    return val != null ? val.toString() : null;
  }

  public boolean getBoolean(int columnIndex) throws SQLException {
    return Boolean.parseBoolean(getString(columnIndex));
  }

  public byte getByte(int columnIndex) {

    return 0;
  }

  /**
   * @see java.sql.ResultSet#getShort(int)
   */
  public short getShort(int columnIndex) throws SQLException {
    checkClosed();
    return Short.parseShort(getString(columnIndex));
  }

  /**
   * @see java.sql.ResultSet#getInt(int)
   */
  public int getInt(int columnIndex) throws SQLException {
    checkClosed();
    return Integer.parseInt(getString(columnIndex));
  }

  /**
   * @see java.sql.ResultSet#getLong(int)
   */
  public long getLong(int columnIndex) throws SQLException {
    checkClosed();
    return Long.parseLong(getString(columnIndex));
  }

  /**
   * @see java.sql.ResultSet#getFloat(int)
   */
  public float getFloat(int columnIndex) throws SQLException {
    checkClosed();
    return Float.parseFloat(getString(columnIndex));
  }

  /**
   * @see java.sql.ResultSet#getDouble(int)
   */
  public double getDouble(int columnIndex) throws SQLException {
    checkClosed();
    return Double.parseDouble(getString(columnIndex));
  }

  public BigDecimal getBigDecimal(int columnIndex, int scale) {

    return null;
  }

  public byte[] getBytes(int columnIndex) {

    return null;
  }

  public Date getDate(int columnIndex) {

    return null;
  }

  public Time getTime(int columnIndex) {

    return null;
  }

  public Timestamp getTimestamp(int columnIndex) {

    return null;
  }

  public InputStream getAsciiStream(int columnIndex) {

    return null;
  }

  public InputStream getUnicodeStream(int columnIndex) {

    return null;
  }

  public InputStream getBinaryStream(int columnIndex) {

    return null;
  }

  public String getString(String columnLabel) throws SQLException {
    checkClosed();
    int index = -1;
    if (columnNames == null) {
      throw new SQLException("Use of columnLabel requires setColumnNames to be called first.");
    }
    for (int i = 0; i < columnNames.length; i++) {
      if (columnLabel.equals(columnNames[i])) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      throw new SQLException("Column " + columnLabel + " doesn't exist in this ResultSet");
    }
    return getString(index + 1);
  }

  public boolean getBoolean(String columnLabel) throws SQLException {
    checkClosed();

    return false;
  }

  public byte getByte(String columnLabel) {

    return 0;
  }

  public short getShort(String columnLabel) {

    return 0;
  }

  public int getInt(String columnLabel) {

    return 0;
  }

  public long getLong(String columnLabel) {

    return 0;
  }

  public float getFloat(String columnLabel) {

    return 0;
  }

  public double getDouble(String columnLabel) throws SQLException {
    return Double.parseDouble(getString(columnLabel));
  }

  public BigDecimal getBigDecimal(String columnLabel, int scale) {

    return null;
  }

  public byte[] getBytes(String columnLabel) throws SQLException {
    return getString(columnLabel).getBytes();
  }

  public Date getDate(String columnLabel) {

    return null;
  }

  public Time getTime(String columnLabel) {

    return null;
  }

  public Timestamp getTimestamp(String columnLabel) {

    return null;
  }

  public InputStream getAsciiStream(String columnLabel) {

    return null;
  }

  public InputStream getUnicodeStream(String columnLabel) {
    return null;
  }

  public InputStream getBinaryStream(String columnLabel) {
    return null;
  }

  public SQLWarning getWarnings() {
    return null;
  }

  public void clearWarnings() {


  }

  public String getCursorName() {

    return null;
  }

  /**
   * @see java.sql.ResultSet#getMetaData()
   */
  public ResultSetMetaData getMetaData() throws SQLException {
    checkClosed();

    if (data == null) {
      return new MongoResultSetMetaData(tableName, new String[0], new int[0]);
    }

    int[] columnJavaTypes = new int[columnNames.length];
    for (int i = 0; i < columnNames.length; i++) {
      columnJavaTypes[i] = Types.VARCHAR;
    }

    return new MongoResultSetMetaData(tableName, columnNames, columnJavaTypes);
  }

  public Object getObject(int columnIndex) throws SQLException {
    if (currentRow >= data.size()) {
      throw new SQLException("ResultSet exhausted, request currentRow = " + currentRow);
    }
    int adjustedColumnIndex = columnIndex - 1;
    if (adjustedColumnIndex >= data.get(currentRow).length) {
      throw new SQLException("Column index does not exist: " + columnIndex);
    }
    return data.get(currentRow)[adjustedColumnIndex];
  }

  public Object getObject(String columnLabel) {

    return null;
  }

  public int findColumn(String columnLabel) {

    return 0;
  }

  public Reader getCharacterStream(int columnIndex) {

    return null;
  }

  public Reader getCharacterStream(String columnLabel) {

    return null;
  }

  public BigDecimal getBigDecimal(int columnIndex) {

    return null;
  }

  public BigDecimal getBigDecimal(String columnLabel) {

    return null;
  }

  public boolean isBeforeFirst() {

    return false;
  }

  public boolean isAfterLast() {

    return false;
  }

  public boolean isFirst() {

    return false;
  }

  public boolean isLast() {

    return false;
  }

  public void beforeFirst() {


  }

  public void afterLast() {


  }

  public boolean first() {

    return false;
  }

  public boolean last() {

    return false;
  }

  public int getRow() {

    return 0;
  }

  public boolean absolute(int row) {

    return false;
  }

  public boolean relative(int rows) {

    return false;
  }

  public boolean previous() {

    return false;
  }

  public void setFetchDirection(int direction) throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  public int getFetchDirection() {
    return ResultSet.FETCH_FORWARD;
  }

  public void setFetchSize(int rows) {

  }

  public int getFetchSize() {
    return 0;
  }

  /**
   * @see java.sql.ResultSet#getType()
   */
  public int getType() {
    return ResultSet.TYPE_FORWARD_ONLY;
  }

  /**
   * @see java.sql.ResultSet#getConcurrency()
   */
  public int getConcurrency() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  public boolean rowUpdated() {

    return false;
  }

  public boolean rowInserted() {

    return false;
  }

  public boolean rowDeleted() {

    return false;
  }

  public void updateNull(int columnIndex) {


  }

  public void updateBoolean(int columnIndex, boolean x) {


  }

  public void updateByte(int columnIndex, byte x) {


  }

  public void updateShort(int columnIndex, short x) {


  }

  public void updateInt(int columnIndex, int x) {


  }

  public void updateLong(int columnIndex, long x) {


  }

  public void updateFloat(int columnIndex, float x) {


  }

  public void updateDouble(int columnIndex, double x) {


  }

  public void updateBigDecimal(int columnIndex, BigDecimal x) {


  }

  public void updateString(int columnIndex, String x) {


  }

  public void updateBytes(int columnIndex, byte[] x) {


  }

  public void updateDate(int columnIndex, Date x) {


  }

  public void updateTime(int columnIndex, Time x) {


  }

  public void updateTimestamp(int columnIndex, Timestamp x) {


  }

  public void updateAsciiStream(int columnIndex, InputStream x, int length) {


  }

  public void updateBinaryStream(int columnIndex, InputStream x, int length) {


  }

  public void updateCharacterStream(int columnIndex, Reader x, int length) {


  }

  public void updateObject(int columnIndex, Object x, int scaleOrLength) {


  }

  public void updateObject(int columnIndex, Object x) {


  }

  public void updateNull(String columnLabel) {


  }

  public void updateBoolean(String columnLabel, boolean x) {


  }

  public void updateByte(String columnLabel, byte x) {


  }

  public void updateShort(String columnLabel, short x) {


  }

  public void updateInt(String columnLabel, int x) {


  }

  public void updateLong(String columnLabel, long x) {


  }

  public void updateFloat(String columnLabel, float x) {


  }

  public void updateDouble(String columnLabel, double x) {


  }

  public void updateBigDecimal(String columnLabel, BigDecimal x) {


  }

  public void updateString(String columnLabel, String x) {


  }

  public void updateBytes(String columnLabel, byte[] x) {


  }

  public void updateDate(String columnLabel, Date x) {


  }

  public void updateTime(String columnLabel, Time x) {


  }

  public void updateTimestamp(String columnLabel, Timestamp x) {


  }

  public void updateAsciiStream(String columnLabel, InputStream x, int length) {


  }

  public void updateBinaryStream(String columnLabel, InputStream x, int length) {


  }

  public void updateCharacterStream(String columnLabel, Reader reader, int length) {


  }

  public void updateObject(String columnLabel, Object x, int scaleOrLength) {


  }

  public void updateObject(String columnLabel, Object x) {


  }

  public void insertRow() {


  }

  public void updateRow() {


  }

  public void deleteRow() {


  }

  public void refreshRow() {


  }

  public void cancelRowUpdates() {


  }

  public void moveToInsertRow() {


  }

  public void moveToCurrentRow() {


  }

  /**
   * @see java.sql.ResultSet#getStatement()
   */
  public Statement getStatement() {
    return this.statement;
  }

  public Object getObject(int columnIndex, Map<String, Class<?>> map) {

    return null;
  }

  public Ref getRef(int columnIndex) {

    return null;
  }

  public Blob getBlob(int columnIndex) {

    return null;
  }

  public Clob getClob(int columnIndex) {

    return null;
  }

  public Array getArray(int columnIndex) {

    return null;
  }

  public Object getObject(String columnLabel, Map<String, Class<?>> map) {

    return null;
  }

  public Ref getRef(String columnLabel) {

    return null;
  }

  public Blob getBlob(String columnLabel) {

    return null;
  }

  public Clob getClob(String columnLabel) {

    return null;
  }

  public Array getArray(String columnLabel) {

    return null;
  }

  public Date getDate(int columnIndex, Calendar cal) {

    return null;
  }

  public Date getDate(String columnLabel, Calendar cal) {

    return null;
  }

  public Time getTime(int columnIndex, Calendar cal) {

    return null;
  }

  public Time getTime(String columnLabel, Calendar cal) {

    return null;
  }

  public Timestamp getTimestamp(int columnIndex, Calendar cal) {

    return null;
  }

  public Timestamp getTimestamp(String columnLabel, Calendar cal) {

    return null;
  }

  public URL getURL(int columnIndex) {

    return null;
  }

  public URL getURL(String columnLabel) {

    return null;
  }

  public void updateRef(int columnIndex, Ref x) {


  }

  public void updateRef(String columnLabel, Ref x) {


  }

  public void updateBlob(int columnIndex, Blob x) {


  }

  public void updateBlob(String columnLabel, Blob x) {


  }

  public void updateClob(int columnIndex, Clob x) {


  }

  public void updateClob(String columnLabel, Clob x) {


  }

  public void updateArray(int columnIndex, Array x) {


  }

  public void updateArray(String columnLabel, Array x) {


  }

  public RowId getRowId(int columnIndex) {

    return null;
  }

  public RowId getRowId(String columnLabel) {

    return null;
  }

  public void updateRowId(int columnIndex, RowId x) {


  }

  public void updateRowId(String columnLabel, RowId x) {


  }

  public int getHoldability() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  /**
   * @see java.sql.ResultSet#isClosed()
   */
  public boolean isClosed() {
    return isClosed;
  }

  public void updateNString(int columnIndex, String nString) throws SQLException {
    checkClosed();


  }

  public void updateNString(String columnLabel, String nString) {


  }

  public void updateNClob(int columnIndex, NClob nClob) {


  }

  public void updateNClob(String columnLabel, NClob nClob) {


  }

  public NClob getNClob(int columnIndex) {

    return null;
  }

  public NClob getNClob(String columnLabel) {

    return null;
  }

  public SQLXML getSQLXML(int columnIndex) {

    return null;
  }

  public SQLXML getSQLXML(String columnLabel) {

    return null;
  }

  public void updateSQLXML(int columnIndex, SQLXML xmlObject) {


  }

  public void updateSQLXML(String columnLabel, SQLXML xmlObject) {


  }

  public String getNString(int columnIndex) {

    return null;
  }

  public String getNString(String columnLabel) {

    return null;
  }

  public Reader getNCharacterStream(int columnIndex) {

    return null;
  }

  public Reader getNCharacterStream(String columnLabel) {

    return null;
  }

  public void updateNCharacterStream(int columnIndex, Reader x, long length) {


  }

  public void updateNCharacterStream(String columnLabel, Reader reader, long length) {


  }

  public void updateAsciiStream(int columnIndex, InputStream x, long length) {


  }

  public void updateBinaryStream(int columnIndex, InputStream x, long length) {


  }

  public void updateCharacterStream(int columnIndex, Reader x, long length) {


  }

  public void updateAsciiStream(String columnLabel, InputStream x, long length) {


  }

  public void updateBinaryStream(String columnLabel, InputStream x, long length) {


  }

  public void updateCharacterStream(String columnLabel, Reader reader, long length) {


  }

  public void updateBlob(int columnIndex, InputStream inputStream, long length) {


  }

  public void updateBlob(String columnLabel, InputStream inputStream, long length) {


  }

  public void updateClob(int columnIndex, Reader reader, long length) {


  }

  public void updateClob(String columnLabel, Reader reader, long length) {


  }

  public void updateNClob(int columnIndex, Reader reader, long length) {


  }

  public void updateNClob(String columnLabel, Reader reader, long length) {


  }

  public void updateNCharacterStream(int columnIndex, Reader x) {


  }

  public void updateNCharacterStream(String columnLabel, Reader reader) {


  }

  public void updateAsciiStream(int columnIndex, InputStream x) {


  }

  public void updateBinaryStream(int columnIndex, InputStream x) {


  }

  public void updateCharacterStream(int columnIndex, Reader x) {


  }

  public void updateAsciiStream(String columnLabel, InputStream x) {


  }

  public void updateBinaryStream(String columnLabel, InputStream x) {


  }

  public void updateCharacterStream(String columnLabel, Reader reader) {


  }

  public void updateBlob(int columnIndex, InputStream inputStream) {


  }

  public void updateBlob(String columnLabel, InputStream inputStream) {


  }

  public void updateClob(int columnIndex, Reader reader) {


  }

  public void updateClob(String columnLabel, Reader reader) {


  }

  public void updateNClob(int columnIndex, Reader reader) {


  }

  public void updateNClob(String columnLabel, Reader reader) {


  }

  private void checkClosed() throws SQLException {
    if (isClosed) {
      throw new SQLException("ResultSet was previously closed.");
    }
  }

  @Override
  public <T> T getObject(int columnIndex, Class<T> type) {
    return null;
  }

  @Override
  public <T> T getObject(String columnLabel, Class<T> type) {
    return null;
  }
}
