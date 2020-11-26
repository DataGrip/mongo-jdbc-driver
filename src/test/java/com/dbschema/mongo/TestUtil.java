package com.dbschema.mongo;

import com.dbschema.mongo.Command.CommandOptions;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

/**
 * @author Liudmila Kornilova
 **/
public class TestUtil {
  public static final String URL = "mongodb://admin:admin@localhost:27017/admin";
  private static final Pattern MONGO_ID_PATTERN = Pattern.compile("[0-9a-f]{24}");
  private static final Pattern MONGO_UUID_PATTERN = Pattern.compile("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}");

  public static String print(@NotNull ResultSet resultSet, @NotNull CommandOptions options) throws SQLException {
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
        Object o = resultSet.getObject(i);
        sb.append(options.dontCheckValue ? o.getClass().getSimpleName() : o);
      }
      sb.append("|").append("\n");
    }
    return sb.toString();
  }

  public static void doTest(String name, Connection connection, String testDataPath, String expectedDataPath) throws IOException, SQLException {
    File expectedFileIgnored = new File(expectedDataPath + "/" + name + "-ignored.expected.txt");
    assumeFalse(expectedFileIgnored.exists());
    File testFile = new File(testDataPath + "/" + name + ".js");
    String test = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);
    String[] before = new String[1];
    List<Command> commands = new ArrayList<>();
    String[] clear = new String[1];
    TestDataReader.read(test, Arrays.asList(
        new TestDataReader.SectionHandler("before", (s, properties) -> before[0] = s),
        new TestDataReader.SectionHandler("command", (s, properties) -> commands.add(new Command(s, new CommandOptions("true".equals(properties.get("dontCheckValue")))))),
        new TestDataReader.SectionHandler("clear", (s, properties) -> clear[0] = s)
    ));
    try (Statement statement = connection.createStatement()) {
      runIgnore(statement, clear);
      try {
        run(statement, before);
        StringBuilder actual = new StringBuilder();
        assertFalse("Command cannot be null", commands.isEmpty());
        try {
          for (Command command : commands) {

            boolean result = statement.execute(command.command);
            if (result) {
              ResultSet resultSet = statement.getResultSet();
              assertNotNull("Result set cannot be null", resultSet);
              actual.append(print(resultSet, command.options)).append("\n");
            }
            else {
              actual.append("No result\n");
            }
          }
        }
        catch (Throwable t) {
          System.err.println("IGNORED:\n");
          t.printStackTrace();
          String message = t.getMessage();
          String msg = message.contains("\n") ? message.substring(0, message.indexOf("\n")) : message;
          actual.append(msg).append("\n");
        }
        compare(expectedDataPath, name, actual.toString());
      }
      finally {
        runIgnore(statement, clear);
      }
    }
  }

  private static void compare(String testDataPath, String name, String actual) throws IOException {
    File expectedFile = new File(testDataPath + "/" + name + ".expected.txt");
    actual = replaceUuid(replaceId(actual)).trim();
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

  public static String replaceUuid(@NotNull String value) {
    return MONGO_UUID_PATTERN.matcher(value).replaceAll("<UUID>");
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
