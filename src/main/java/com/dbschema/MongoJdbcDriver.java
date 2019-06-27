
package com.dbschema;

import com.dbschema.mongo.MongoService;
import com.dbschema.mongo.parser.ScanStrategy;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Minimal implementation of the JDBC standards for MongoDb database.
 * This is customized for DbSchema database designer.
 * Connect to the database using a URL like :
 * jdbc:mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
 * The URL excepting the jdbc: prefix is passed as it is to the MongoDb native Java driver.
 */
public class MongoJdbcDriver implements Driver
{
    private DriverPropertyInfoHelper propertyInfoHelper = new DriverPropertyInfoHelper();

    static {
        try {
            DriverManager.registerDriver( new MongoJdbcDriver());
            Logger mongoLogger = Logger.getLogger( "com.mongodb" );
            if ( mongoLogger != null ) mongoLogger.setLevel(Level.SEVERE);
        } catch ( SQLException ex ){
            ex.printStackTrace();
        }
    }


    /**
     * Connect to the database using a URL like :
     * jdbc:mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
     * The URL excepting the jdbc: prefix is passed as it is to the MongoDb native Java driver.
     */
    public Connection connect(String url, Properties info) throws SQLException {
        if ( url != null && acceptsURL( url )){
            ScanStrategy scan = ScanStrategy.fast;
            for (ScanStrategy s : ScanStrategy.values() ){
                if ( url.contains( "?scan=" + s ) || url.contains( "&scan=" + s )){
                    scan = s;
                    url = url.replaceFirst("\\?scan=" + s, "" ).replaceFirst("&scan=" + s, "");
                }
            }
            try	{
                if ( url.startsWith("jdbc:")) {
                    url = url.substring("jdbc:".length());
                }
                final MongoService service = new MongoService(url, info, scan );

                return new MongoConnection(service);
            } catch (UnknownHostException e) {
                throw new SQLException("Unexpected exception: " + e.getMessage(), e);
            }
        }
        return null;
    }


    /**
     * URLs accepted are of the form: jdbc:mongodb[+srv]://<server>[:27017]/<db-name>
     *
     * @see java.sql.Driver#acceptsURL(java.lang.String)
     */
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("mongodb") || url.startsWith("jdbc:mongodb");
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
