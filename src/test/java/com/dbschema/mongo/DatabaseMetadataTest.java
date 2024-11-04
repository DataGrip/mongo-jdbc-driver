package com.dbschema.mongo;

import com.dbschema.MongoJdbcDriver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import static com.dbschema.mongo.TestUtil.URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DatabaseMetadataTest {
  private static Connection connection;

  @BeforeClass
  public static void before() throws SQLException {
    Properties properties = new Properties();
    connection = new MongoJdbcDriver().connect(URL, properties);
  }

  @AfterClass
  public static void after() throws SQLException {
    if (connection != null) connection.close();
  }

  @Test
  public void test() throws SQLException {
    try {
      connection.createStatement().execute("use my_database");
      connection.createStatement().execute("db.getCollection('hello.my_collection').insertOne({'hello_world': 1})");
      ResultSet columns = connection.getMetaData().getColumns("", "my\\_database", "hello.my\\_collection", "%");
      assertTrue(columns.next());
      assertEquals("my_database", columns.getString(2));
      assertEquals("hello.my_collection", columns.getString(3));
      assertEquals("_id", columns.getString(4));
      assertTrue(columns.next());
      assertEquals("hello_world", columns.getString(4));
    }
    finally {
      connection.createStatement().execute("db.getCollection('hello.my_collection').drop()");
    }
  }
}
