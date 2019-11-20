package com.dbschema.mongo;

import com.dbschema.MongoJdbcDriver;
import com.dbschema.mongo.TestDataReader.SectionHandler;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static com.dbschema.mongo.TestUtil.*;
import static org.junit.Assert.*;

/**
 * @author Liudmila Kornilova
 **/
@RunWith(Parameterized.class)
public class MongoCollectionCommandsTest {
  private static final String TEST_DATA_PATH = "src/test/resources/commands/collections";
  private static Connection connection;
  private final String testName;

  public MongoCollectionCommandsTest(String testName) {
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
  public static void before() throws SQLInvalidAuthorizationSpecException {
    connection = new MongoJdbcDriver().connect(URL, new Properties());
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
