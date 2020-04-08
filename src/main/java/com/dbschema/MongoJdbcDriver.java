package com.dbschema;

import com.dbschema.mongo.DriverPropertyInfoHelper;
import com.dbschema.mongo.MongoConnection;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dbschema.mongo.DriverPropertyInfoHelper.*;


/**
 * Minimal implementation of the JDBC standards for MongoDb database.
 * This is customized for DbSchema database designer.
 * Connect to the database using a URL like :
 * jdbc:mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
 * The URL excepting the jdbc: prefix is passed as it is to the MongoDb native Java driver.
 */
public class MongoJdbcDriver implements Driver {
  private final DriverPropertyInfoHelper propertyInfoHelper = new DriverPropertyInfoHelper();

  static {
    try {
      DriverManager.registerDriver(new MongoJdbcDriver());
      Logger mongoLogger = Logger.getLogger("com.mongodb");
      if (mongoLogger != null) mongoLogger.setLevel(Level.SEVERE);
    }
    catch (SQLException ex) {
      ex.printStackTrace();
    }
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
    boolean useEs6 = USE_ES6_DEFAULT;
    if (info.getProperty(USE_ES6) != null) {
      useEs6 = Boolean.parseBoolean(info.getProperty(USE_ES6));
    }

    if (url.startsWith("jdbc:")) {
      url = url.substring("jdbc:".length());
    }

    String username = info.getProperty("user");
    String password = info.getProperty("password");

    MongoConnection mongoConnection = new MongoConnection(url, info, username, password, fetchDocumentsForMeta, useEs6);
    mongoConnection.getService().getClient().testConnectivity();
    return mongoConnection;
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
}
