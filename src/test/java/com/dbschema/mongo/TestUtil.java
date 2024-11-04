package com.dbschema.mongo;

import com.dbschema.mongo.Command.CommandOptions;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class TestUtil {
  public static final String PATH_TO_URI = "src/test/resources/URI.txt";
  public static final String URL;
  private static final Pattern MONGO_ID_PATTERN = Pattern.compile("[0-9a-f]{24}");
  private static final Pattern MONGO_UUID_PATTERN = Pattern.compile("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}");

  static {
    String url;
    try {
      url = FileUtils.readFileToString(new File(PATH_TO_URI), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      url = "File was not read. " + e.getMessage();
    }
    URL = url;
  }

  @NotNull
  private static String getHostPort(String uri) {
    try {
      URI u = new URI(uri);
      return u.getHost() + ":" + u.getPort();
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

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

  public static void doTest(@NotNull String name, @NotNull Connection connection, @NotNull String testDataPath) throws IOException, SQLException {
    String database = getDatabase(connection);
    File expectedFileIgnored = new File(testDataPath + "/" + name + "-ignored.expected.txt");
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
          for (Command command : commands) {
            try {
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
            catch (Throwable t) {
              System.err.println("IGNORED:");
              t.printStackTrace();
              String message = t.getMessage();
              String msg = message.contains("\n") ? message.substring(0, message.indexOf("\n")) : message;
              String res = command.options.dontCheckValue ? extractExceptionType(t) : msg;
              actual.append(res).append("\n");
              break;
            }
          }
        compare(testDataPath, name, actual.toString());
      }
      finally {
        runIgnore(statement, clear);
        run(statement, "use " + database);
      }
    }
  }

  @NotNull
  private static String extractExceptionType(@NotNull Throwable t) {
    if (t instanceof SQLException && t.getCause() != null) return extractExceptionType(t.getCause());
    String msg = t.getMessage();
    int colonPos = msg.indexOf(":");
    if (colonPos > 0) return msg.substring(0, colonPos).trim();
    return t.getClass().getSimpleName();
  }

  @NotNull
  private static String getDatabase(Connection connection) {
    try (Statement statement = connection.createStatement()) {
      statement.execute("db");
      ResultSet resultSet = statement.getResultSet();
      assertTrue(resultSet.next());
      return resultSet.getObject(1).toString();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static void compare(String testDataPath, String name, String actual) throws IOException {
    File expectedFile = new File(testDataPath + "/" + name + ".expected.txt");
    actual = replaceHostPort(replaceUuid(replaceId(actual))).trim();
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

  private static String replaceHostPort(String actual) {
    return actual.replaceAll(Pattern.quote(getHostPort(URL)), "%mongohostport%");
  }

  private static void run(Statement statement, String[] q) throws SQLException {
    if (q[0] != null && !q[0].isEmpty()) {
      run(statement, q[0]);
    }
  }

  private static void runIgnore(Statement statement, String[] q) {
    try {
      if (q[0] != null && !q[0].isEmpty()) {
        run(statement, q[0]);
      }
    }
    catch (Throwable ignored) {
    }
  }

  private static void run(Statement statement, String q) throws SQLException {
    if (q.startsWith("use ")) {
      int semicolon = q.indexOf(";");
      semicolon = semicolon == -1 ? q.length() : semicolon;
      statement.execute(q.substring(0, semicolon));
      if (semicolon >= q.length() - 1) return;
      q = q.substring(semicolon + 1);
    }
    statement.execute(q);
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
