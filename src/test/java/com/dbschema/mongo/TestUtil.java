package com.dbschema.mongo;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * @author Liudmila Kornilova
 **/
public class TestUtil {
  public static final String URL = "mongodb://admin:admin@localhost:27017/admin";
  private static final Pattern MONGO_ID_PATTERN = Pattern.compile("[0-9a-f]{24}");

  public static String print(@NotNull ResultSet resultSet) throws SQLException {
    StringBuilder sb = new StringBuilder("|");
    ResultSetMetaData metaData = resultSet.getMetaData();
    boolean first = true;
    for (int i = 1; i <= metaData.getColumnCount(); i++) {
      if (first) first = false;
      else sb.append("|");
      sb.append(metaData.getColumnName(i));
    }
    sb.append("|").append("\n");
    while (resultSet.next()) {
      sb.append("|");
      first = true;
      for (int i = 1; i <= metaData.getColumnCount(); i++) {
        if (first) first = false;
        else sb.append("|");
        sb.append(resultSet.getObject(i));
      }
      sb.append("|").append("\n");
    }
    return sb.toString();
  }

  public static void doTest(String name, Connection connection, String testDataPath) throws IOException, SQLException {
    boolean hasResults = !name.endsWith("-undefined");
    boolean throwsE = name.endsWith("-throws");
    File testFile = new File(testDataPath + "/" + name + ".js");
    String test = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);
    String[] before = new String[1];
    String[] command = new String[1];
    String[] clear = new String[1];
    TestDataReader.read(test, Arrays.asList(
        new TestDataReader.SectionHandler("before", s -> before[0] = s),
        new TestDataReader.SectionHandler("command", s -> command[0] = s),
        new TestDataReader.SectionHandler("clear", s -> clear[0] = s)
    ));
    try (Statement statement = connection.createStatement()) {
      runIgnore(statement, clear);
      try {
        run(statement, before);
        assertNotNull("Command cannot be null", command[0]);
        try {
          boolean result = statement.execute(command[0]);
          if (!hasResults) {
            assertFalse("Command should not produce nothing", result);
          }
          else {
            assertTrue("Command should produce result set", result);
            ResultSet resultSet = statement.getResultSet();
            assertNotNull("Result set cannot be null", resultSet);
            String actual = print(resultSet);
            compare(testDataPath, name, actual);
          }
        }
        catch (Throwable t) {
          if (!throwsE) throw t;
          compare(testDataPath, name, t.getMessage());
        }
      }
      finally {
        runIgnore(statement, clear);
      }
    }
  }

  private static void compare(String testDataPath, String name, String actual) throws IOException {
    File expectedFile = new File(testDataPath + "/" + name + ".expected.txt");
    actual = replaceId(actual).trim();
    if (!expectedFile.exists()) {
      assertTrue(expectedFile.createNewFile());
      FileUtils.write(expectedFile, actual, StandardCharsets.UTF_8);
      fail("Created output file " + expectedFile);
    }
    else {
      String expected = FileUtils.readFileToString(expectedFile, StandardCharsets.UTF_8);
      assertEquals(expected.trim(), actual);
    }
  }

  private static void run(Statement statement, String[] q) throws SQLException {
    if (q[0] != null && !q[0].isEmpty()) {
      statement.execute(q[0]);
    }
  }

  private static void runIgnore(Statement statement, String[] q) {
    try {
      if (q[0] != null && !q[0].isEmpty()) {
        statement.execute(q[0]);
      }
    }
    catch (Throwable ignored) {
    }
  }

  public static String replaceId(@NotNull String value) {
    return MONGO_ID_PATTERN.matcher(value).replaceAll("<ObjectID>");
  }

  public static int getNumberOfConnections(@NotNull Connection connection) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("db.serverStatus()");
      ResultSet rs = stmt.getResultSet();
      rs.next();
      Object o = rs.getObject(1);
      Object connections = ((Map<?, ?>) o).get("connections");
      return ((Number)((Map<?, ?>) connections).get("current")).intValue();
    }
  }
}
