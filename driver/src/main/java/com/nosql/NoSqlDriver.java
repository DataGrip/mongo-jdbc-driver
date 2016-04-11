
package com.nosql;

import com.nosql.mongo.MongoService;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * Driver implementation that provides minimal JDBC implementation for accessing MongoDB instances. The jdbc
 * driver url spec is as follows: jdbc:mongodb://<server>[:27017]/<db-name> See getPropertyInfo method for
 * driver properties that can be specified when getting a Connection to alter the semantics of that
 * Connection.
 */
public class NoSqlDriver implements Driver
{
    private DriverPropertyInfoHelper propertyInfoHelper = new DriverPropertyInfoHelper();

    static {
        try {
            DriverManager.registerDriver( new NoSqlDriver());
        } catch ( SQLException ex ){
            ex.printStackTrace();
        }
    }


    /**
     * @see java.sql.Driver#connect(java.lang.String, java.util.Properties)
     */
    public Connection connect(String url, Properties info) throws SQLException {
        if ( url != null && acceptsURL( url )){
            try	{
                final MongoService service = new MongoService( url.substring("jdbc:".length()), info );
                return new NoSqlConnection(service);
            } catch (UnknownHostException e) {
                throw new SQLException("Unexpected exception: " + e.getMessage(), e);
            }
        }
        return null;
    }


    /**
     * URLs accepted are of the form: jdbc:mongodb://<server>[:27017]/<db-name>
     *
     * @see java.sql.Driver#acceptsURL(java.lang.String)
     */
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("jdbc:mongodb:");
    }

    /**
     * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException
    {
        return propertyInfoHelper.getPropertyInfo();
    }

    /**
     * @see java.sql.Driver#getMajorVersion()
     */
    @Override
    public int getMajorVersion()
    {
        return 1;
    }

    /**
     * @see java.sql.Driver#getMinorVersion()
     */
    @Override
    public int getMinorVersion()
    {
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
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }


    public static void logClassLoader( Class cls ){
        ClassLoader cl = cls.getClassLoader();
        StringBuilder sb = new StringBuilder("#### DRV ").append( cls ).append(" ###");
        do {
            sb.append("\n\t").append( cl ).append( " ");
            if ( cl instanceof URLClassLoader){
                URLClassLoader ucl = (URLClassLoader)cl;
                for( URL url : ucl.getURLs()){
                    sb.append( url ).append(" ");
                }
            }
        } while ( ( cl = cl.getParent() ) != null );
        System.out.println( sb  );
    }
}
