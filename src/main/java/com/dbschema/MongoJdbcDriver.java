package com.dbschema;

import com.dbschema.mongo.DriverPropertyInfoHelper;
import com.dbschema.mongo.MongoConnection;
import com.dbschema.mongo.mongosh.LazyShellHolder;
import com.dbschema.mongo.mongosh.PrecalculatingShellHolder;
import com.dbschema.mongo.mongosh.ShellHolder;
import org.graalvm.polyglot.Engine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import static com.dbschema.mongo.DriverPropertyInfoHelper.FETCH_DOCUMENTS_FOR_METAINFO;
import static com.dbschema.mongo.DriverPropertyInfoHelper.FETCH_DOCUMENTS_FOR_METAINFO_DEFAULT;
import static com.dbschema.mongo.Util.newNamedThreadFactory;
import static java.util.concurrent.Executors.newFixedThreadPool;


/**
 * Minimal implementation of the JDBC standards for MongoDb database.
 * This is customized for DbSchema database designer.
 * Connect to the database using a URL like :
 * jdbc:mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
 * The URL excepting the jdbc: prefix is passed as it is to the MongoDb native Java driver.
 */
public class MongoJdbcDriver implements Driver {
  private final DriverPropertyInfoHelper propertyInfoHelper = new DriverPropertyInfoHelper();
  private @Nullable ExecutorService executorService;
  private @Nullable Engine sharedEngine;
  private @NotNull ShellHolder shellHolder;

  static {
    try {
      DriverManager.registerDriver(new MongoJdbcDriver());
    }
    catch (SQLException ex) {
      ex.printStackTrace();
    }
  }

  public MongoJdbcDriver() {
    shellHolder = createShellHolder();
  }

  @NotNull
  private ShellHolder createShellHolder() {
    if ("true".equals(System.getProperty("mongosh.disableShellPrecalculation"))) {
      return new LazyShellHolder();
    }
    if (executorService == null) {
      executorService = newFixedThreadPool(10, newNamedThreadFactory("MongoShell ExecutorService"));
    }
    Engine engine = null;
    if (!"true".equals(System.getProperty("mongosh.disableSharedEngine"))) {
      if (sharedEngine == null) {
        sharedEngine = Engine.create("js");
      }
      engine = sharedEngine;
    }
    return new PrecalculatingShellHolder(executorService, engine);
  }

  /**
   * Connect to the database using a URL like :
   * jdbc:mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
   * The URL excepting the jdbc: prefix is passed as it is to the MongoDb native Java driver.
   */
  public Connection connect(String url, Properties info) throws SQLException {
    if (url == null || !acceptsURL(url)) return null;

    int fetchDocumentsForMeta = FETCH_DOCUMENTS_FOR_METAINFO_DEFAULT;
    if (info.getProperty(FETCH_DOCUMENTS_FOR_METAINFO) != null) {
      try {
        fetchDocumentsForMeta = Integer.parseInt(info.getProperty(FETCH_DOCUMENTS_FOR_METAINFO));
      }
      catch (NumberFormatException ignored) {
      }
    }
    if (fetchDocumentsForMeta < 0) fetchDocumentsForMeta = 0;

    if (url.startsWith("jdbc:")) {
      url = url.substring("jdbc:".length());
    }

    String username = info.getProperty("user");
    String password = info.getProperty("password");
    synchronized (this) {
      ShellHolder shellHolder = this.shellHolder;
      this.shellHolder = createShellHolder();
      return new MongoConnection(url, info, username, password, fetchDocumentsForMeta, shellHolder);
    }
  }


  /**
   * URLs accepted are of the form: jdbc:mongodb[+srv]://<server>[:27017]/<db-name>
   *
   * @see java.sql.Driver#acceptsURL(java.lang.String)
   */
  @Override
  public boolean acceptsURL(String url) {
    return url.startsWith("mongodb") || url.startsWith("jdbc:mongodb");
  }

  /**
   * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
   */
  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
    return propertyInfoHelper.getPropertyInfo();
  }

  /**
   * @see java.sql.Driver#getMajorVersion()
   */
  @Override
  public int getMajorVersion() {
    return 1;
  }

  /**
   * @see java.sql.Driver#getMinorVersion()
   */
  @Override
  public int getMinorVersion() {
    return 0;
  }

  /**
   * @see java.sql.Driver#jdbcCompliant()
   */
  @Override
  public boolean jdbcCompliant() {
    return true;
  }

  @Override
  public Logger getParentLogger() {
    return null;
  }

  public void close() {
    shellHolder.close();
    if (sharedEngine != null) sharedEngine.close();
    if (executorService != null) executorService.shutdownNow();
  }
}
