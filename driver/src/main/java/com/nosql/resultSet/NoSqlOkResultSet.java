package com.nosql.resultSet;

import com.nosql.NoSqlResultSetMetaData;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class NoSqlOkResultSet extends NoSqlIteratorResultSet {

    public NoSqlOkResultSet(){
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return "Ok";
    }

    @Override
    public boolean next() throws SQLException {
        return false;
    }

    @Override
    public void close() throws SQLException {
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new NoSqlResultSetMetaData("Result", new String[]{"map"},  new int[]{Types.JAVA_OBJECT},new int[]{300});
    }

}
