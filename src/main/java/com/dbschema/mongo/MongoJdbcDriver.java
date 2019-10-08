package com.dbschema.mongo;

import com.dbschema.mongo.java.JMongoService;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbschema.mongo.DriverPropertyInfoHelper.FETCH_DOCUMENTS_FOR_METAINFO;
import static com.dbschema.mongo.DriverPropertyInfoHelper.FETCH_DOCUMENTS_FOR_METAINFO_DEFAULT;
import static com.dbschema.mongo.java.JMongoClient.removeParameter;


/**
 * Minimal implementation of the JDBC standards for MongoDb database.
 * This is customized for DbSchema database designer.
 * Connect to the database using a URL like :
 * jdbc:mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
 * The URL excepting the jdbc: prefix is passed as it is to the MongoDb native Java driver.
 */
public class MongoJdbcDriver implements Driver {
  private static final Pattern FETCH_DOCUMENTS_FOR_META_PATTERN = Pattern.compile("([?&])" + FETCH_DOCUMENTS_FOR_METAINFO + "=(\\d+)&?");
  private DriverPropertyInfoHelper propertyInfoHelper = new DriverPropertyInfoHelper();

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
  public Connection connect(String url, Properties info) {
    if (url == null || !acceptsURL(url)) return null;

    int fetchDocumentsForMeta = FETCH_DOCUMENTS_FOR_METAINFO_DEFAULT;
    if (info.getProperty(FETCH_DOCUMENTS_FOR_METAINFO) != null) {
      try {
        fetchDocumentsForMeta = Integer.parseInt(info.getProperty(FETCH_DOCUMENTS_FOR_METAINFO));
      }
      catch (NumberFormatException ignored) {
      }
    }
    Matcher matcher = FETCH_DOCUMENTS_FOR_META_PATTERN.matcher(url);
    if (matcher.find()) {
      url = removeParameter(url, matcher);
      try {
        fetchDocumentsForMeta = Integer.parseInt(matcher.group(2));
      }
      catch (NumberFormatException ignored) {
      }
    }
    if (fetchDocumentsForMeta < 0) fetchDocumentsForMeta = 0;

    if (url.startsWith("jdbc:")) {
      url = url.substring("jdbc:".length());
    }
    final JMongoService service = new JMongoService(url, info, fetchDocumentsForMeta);

    return new MongoConnection(service);
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
