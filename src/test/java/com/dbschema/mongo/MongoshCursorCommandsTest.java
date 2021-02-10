package com.dbschema.mongo;

import com.dbschema.MongoJdbcDriver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static com.dbschema.mongo.TestUtil.URL;
import static com.dbschema.mongo.TestUtil.doTest;

/**
 * @author Liudmila Kornilova
 **/
@RunWith(Parameterized.class)
public class MongoshCursorCommandsTest {
  private static final String TEST_DATA_PATH = "src/test/resources/test-data/cursor";
  private static Connection connection;
  private final String testName;

  public MongoshCursorCommandsTest(String testName) {
    this.testName = testName;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<?> fileNames() {
    File testsDir = new File(TEST_DATA_PATH);
    File[] files = testsDir.listFiles();
    assert files != null;
    return Arrays.asList(Util.map(
        Util.filter(files,
            file -> file.getName().endsWith(".js")),
        file -> new Object[]{file.getName().substring(0, file.getName().length() - ".js".length())}));
  }

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
  public void test() throws IOException, SQLException {
    doTest(testName, connection, TEST_DATA_PATH);
  }
}
