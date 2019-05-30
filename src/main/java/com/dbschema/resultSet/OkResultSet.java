package com.dbschema.resultSet;

import com.dbschema.MongoResultSetMetaData;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class OkResultSet extends ResultSetIterator {

    public OkResultSet(){
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
        return new MongoResultSetMetaData("Result", new String[]{"map"},  new int[]{Types.JAVA_OBJECT},new int[]{300});
    }

}
