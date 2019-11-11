package com.dbschema;

import com.dbschema.mongo.DriverPropertyInfoHelper;
import com.dbschema.mongo.MongoConnection;
import com.dbschema.mongo.MongoConnectionParameters;
import com.mongodb.AuthenticationMechanism;
import com.mongodb.ConnectionString;
import com.mongodb.MongoCredential;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbschema.mongo.DriverPropertyInfoHelper.*;
import static com.dbschema.mongo.Util.nullize;
import static com.dbschema.mongo.nashorn.JMongoClient.removeParameter;


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
  public static final String DEFAULT_DB = "admin";
  private static final Pattern AUTH_MECH_PATTERN = Pattern.compile("([?&])authMechanism=([\\w_-]+)&?");
  private static final Pattern AUTH_SOURCE_PATTERN = Pattern.compile("([?&])authSource=([\\w_-]+)&?");

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
  public Connection connect(String url, Properties info) throws SQLInvalidAuthorizationSpecException {
    if (url == null || !acceptsURL(url)) return null;

    int fetchDocumentsForMeta = FETCH_DOCUMENTS_FOR_METAINFO_DEFAULT;
    if (info.getProperty(FETCH_DOCUMENTS_FOR_METAINFO) != null) {
      try {
        fetchDocumentsForMeta = Integer.parseInt(info.getProperty(FETCH_DOCUMENTS_FOR_METAINFO));
      }
      catch (NumberFormatException ignored) {
      }
    }
    boolean useMongoShell = USE_MONGO_SHELL_DEFAULT;
    if (info.getProperty(USE_MONGO_SHELL) != null) {
      useMongoShell = Boolean.parseBoolean(info.getProperty(USE_MONGO_SHELL));
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

    ConnectionString connectionString = new ConnectionString(url);
    AuthenticationMechanism authMechanism = null;
    matcher = AUTH_MECH_PATTERN.matcher(url);
    if (matcher.find()) {
      url = removeParameter(url, matcher);
      authMechanism = AuthenticationMechanism.fromMechanismName(matcher.group(2));
    }
    String authSource = null;
    matcher = AUTH_SOURCE_PATTERN.matcher(url);
    if (matcher.find()) {
      url = removeParameter(url, matcher);
      authSource = matcher.group(2);
    }
    String databaseNameFromUrl = nullize(connectionString.getDatabase());
    MongoCredential credentialsFromUrl = connectionString.getCredential();
    String source = credentialsFromUrl != null ? credentialsFromUrl.getSource() :
                    authSource != null ? authSource :
                    databaseNameFromUrl != null ? databaseNameFromUrl :
                    DEFAULT_DB;

    String username = info.getProperty("user");
    String password = info.getProperty("password");
    MongoConnectionParameters parameters = new MongoConnectionParameters(username, password == null ? null : password.toCharArray(),
        source, databaseNameFromUrl, authMechanism);

    return new MongoConnection(url, info, parameters, fetchDocumentsForMeta, useMongoShell);
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
