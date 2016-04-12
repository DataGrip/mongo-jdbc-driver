package com.dbschema;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class MongoResultSetMetaData implements ResultSetMetaData
{

	private final String tableName;
    private final String[] columnNames;
    private final int[] javaTypes;
    private final int[] displaySizes;

	public MongoResultSetMetaData(String tableName, String[] columnNames, int[] javaTypes, int[] displaySizes)
	{
		this.tableName = tableName;
		this.columnNames = columnNames;
		this.displaySizes = displaySizes;
        this.javaTypes = javaTypes;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException	{
		return false;
	}

	/**
	 * @see java.sql.ResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws SQLException
	{
		return this.columnNames.length;
	}

	/**
	 * @see java.sql.ResultSetMetaData#isAutoIncrement(int)
	 */
	@Override
	public boolean isAutoIncrement(int column) throws SQLException
	{
		return false;
	}

	@Override
	public boolean isCaseSensitive(int column) throws SQLException {
		return false;
	}

	@Override
	public boolean isSearchable(int column) throws SQLException {
		return false;
	}

	@Override
	public boolean isCurrency(int column) throws SQLException {
		return false;
	}

	/**
	 * @see java.sql.ResultSetMetaData#isNullable(int)
	 */
	@Override
	public int isNullable(int column) throws SQLException
	{
		return ResultSetMetaData.columnNoNulls;
	}

	/**
	 * @see java.sql.ResultSetMetaData#isSigned(int)
	 */
	@Override
	public boolean isSigned(int column) throws SQLException
	{
		return false;
	}

	/**
	 * @see java.sql.ResultSetMetaData#getColumnDisplaySize(int)
	 */
	@Override
	public int getColumnDisplaySize(int column) throws SQLException
	{
		return displaySizes[column - 1];
	}

	/**
	 * @see java.sql.ResultSetMetaData#getColumnLabel(int)
	 */
	@Override
	public String getColumnLabel(int column) throws SQLException
	{
		return columnNames[column - 1];
	}

	/**
	 * @see java.sql.ResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) throws SQLException	{
		return columnNames[column - 1];
	}

	/**
	 * @see java.sql.ResultSetMetaData#getSchemaName(int)
	 */
	@Override
	public String getSchemaName(int column) throws SQLException
	{
		return null;
	}

	@Override
	public int getPrecision(int column) throws SQLException
	{
		return 0;
	}

	/**
	 * @see java.sql.ResultSetMetaData#getScale(int)
	 */
	@Override
	public int getScale(int column) throws SQLException
	{
		return 0;
	}

	/**
	 * @see java.sql.ResultSetMetaData#getTableName(int)
	 */
	@Override
	public String getTableName(int column) throws SQLException
	{
		return tableName;
	}

	@Override
	public String getCatalogName(int column) throws SQLException {
		return null;
	}

	/**
	 * @see java.sql.ResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int column) throws SQLException {
		return javaTypes[ column -1 ];
	}

	@Override
	public String getColumnTypeName(int column) throws SQLException
	{
        switch ( javaTypes[column - 1] ){
            case Types.JAVA_OBJECT : return "map";
            default : return "varchar";
        }
	}

	/**
	 * @see java.sql.ResultSetMetaData#isReadOnly(int)
	 */
	@Override
	public boolean isReadOnly(int column) throws SQLException
	{
		return false;
	}

	/**
	 * @see java.sql.ResultSetMetaData#isWritable(int)
	 */
	@Override
	public boolean isWritable(int column) throws SQLException
	{
		return true;
	}

	/**
	 * @see java.sql.ResultSetMetaData#isDefinitelyWritable(int)
	 */
	@Override
	public boolean isDefinitelyWritable(int column) throws SQLException
	{
		return true;
	}

	/**
	 * @see java.sql.ResultSetMetaData#getColumnClassName(int)
	 */
	@Override
	public String getColumnClassName(int column) throws SQLException
	{
		return "java.lang.String";
	}

}
