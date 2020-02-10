package com.dbschema.mongo;

import com.dbschema.MongoJdbcDriver;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static com.dbschema.mongo.TestUtil.URL;
import static com.dbschema.mongo.TestUtil.getNumberOfConnections;
import static org.junit.Assert.assertEquals;

/**
 * @author Liudmila Kornilova
 **/
public class ConnectionClosedTest {
  @Test
  public void testConnectionIsClosed() throws SQLException {
    try (Connection connection = new MongoJdbcDriver().connect(URL, new Properties())) {
      int before = getNumberOfConnections(connection);
      try (Connection connection2 = new MongoJdbcDriver().connect(URL, new Properties());
           Statement stmt = connection2.createStatement()) {
        stmt.execute("1");
      }
      int after = getNumberOfConnections(connection);
      assertEquals(before, after);
    }
  }
}
