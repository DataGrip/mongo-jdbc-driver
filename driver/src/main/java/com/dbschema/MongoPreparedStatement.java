
package com.dbschema;

import com.mongodb.*;
import com.mongodb.client.model.UpdateOptions;
import com.dbschema.mongo.JMongoCollection;
import com.dbschema.mongo.JMongoDatabase;
import com.dbschema.mongo.MongoService;
import com.dbschema.resultSet.AggregateResultSet;
import com.dbschema.resultSet.ArrayResultSet;
import com.dbschema.resultSet.ResultSetIterator;
import com.dbschema.resultSet.OkResultSet;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.bson.Document;

import javax.script.*;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Array;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MongoPreparedStatement implements PreparedStatement {

    private final MongoConnection con;
    private ResultSet lastResultSet;
    private boolean isClosed = false;
    private int maxRows = -1;
    private final String sql;

    public MongoPreparedStatement(final MongoConnection con) {
        this.con = con;
        this.sql = null;
    }

    public MongoPreparedStatement(final MongoConnection con, String sql) {
        this.con = con;
        this.sql = sql;
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return false;
    }


    private static final Pattern PATTERN_USE_DATABASE = Pattern.compile("USE\\s+(.*)", Pattern.CASE_INSENSITIVE );
    private static final Pattern PATTERN_CREATE_DATABASE = Pattern.compile("CREATE\\s+DATABASE\\s*'(.*)'\\s*", Pattern.CASE_INSENSITIVE );

    private static final Pattern PATTERN_SHOW_DATABASES = Pattern.compile("SHOW\\s+DATABASES\\s*", Pattern.CASE_INSENSITIVE );
    private static final Pattern PATTERN_SHOW_DBS = Pattern.compile("SHOW\\s+DBS\\s*", Pattern.CASE_INSENSITIVE );
    private static final Pattern PATTERN_SHOW_COLLECTIONS = Pattern.compile("SHOW\\s+COLLECTIONS\\s*", Pattern.CASE_INSENSITIVE );
    private static final Pattern PATTERN_SHOW_USERS = Pattern.compile("SHOW\\s+USERS\\s*", Pattern.CASE_INSENSITIVE );
    private static final Pattern PATTERN_SHOW_RULES = Pattern.compile("SHOW\\s+RULES\\s*", Pattern.CASE_INSENSITIVE );
    private static final Pattern PATTERN_SHOW_PROFILE = Pattern.compile("SHOW\\s+PROFILE\\s*", Pattern.CASE_INSENSITIVE );

    @Override
    public ResultSet executeQuery(String sql) throws SQLException	{
        checkClosed();
        if (lastResultSet != null ) {
            lastResultSet.close();
        }
        if ( sql == null ){
            throw new SQLException("Null statement.");
        }
        Matcher matcherSetDb = PATTERN_USE_DATABASE.matcher( sql );
        if ( matcherSetDb.matches() ){
            String db = matcherSetDb.group(1).trim();
            if ( ( db.startsWith("\"") && db.endsWith("\"")) || ( db.startsWith("'") && db.endsWith("'"))){
                db = db.substring( 1, db.length()-1);
            }
            con.setCatalog( db );
            return new OkResultSet();
        }
        Matcher matcherCreateDatabase = PATTERN_CREATE_DATABASE.matcher( sql );
        if ( matcherCreateDatabase.matches() ){
            final String dbName = matcherCreateDatabase.group(1);
            con.getDatabase(dbName);
            MongoService.createdDatabases.add(dbName);
            return new OkResultSet();
        }
        if ( sql.toLowerCase().startsWith("show ")){
            if ( PATTERN_SHOW_DATABASES.matcher( sql ).matches() || PATTERN_SHOW_DBS.matcher( sql ).matches() ){
                ArrayResultSet result = new ArrayResultSet();
                result.setColumnNames(new String[]{"DATABASE_NAME"});
                for ( String str : con.getDatabaseNames() ){
                    result.addRow( new String[]{ str });
                }
                return lastResultSet = result;
            }
            if ( PATTERN_SHOW_COLLECTIONS.matcher( sql ).matches()){
                ArrayResultSet result = new ArrayResultSet();
                result.setColumnNames(new String[]{"COLLECTION_NAME"});
                for ( String str : con.getService().getCollectionNames(con.getCatalog()) ){
                    result.addRow( new String[]{ str });
                }
                return lastResultSet = result;
            }
            if ( PATTERN_SHOW_USERS.matcher( sql ).matches()){
                sql = "db.runCommand(\"{usersInfo:'" + con.getCatalog() + "'}\")";
            }
            if ( PATTERN_SHOW_PROFILE.matcher( sql ).matches() || PATTERN_SHOW_RULES.matcher( sql ).matches() ){
                throw new SQLException("Not yet implemented in this driver.");
            }
            throw new SQLException("Invalid command : " + sql );
        }
        try {
            Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory");
        } catch (ClassNotFoundException ex) {
            throw new SQLException(
                    "Error: Java 1.8 or later from Oracle is required.\n" +
                            "MongoDb JDBC driver uses the Nashorn JavaScript engine delivered with Java.\n" +
                            "Check in DbSchema Help/About Dialog the current Java version.\n" +
                            "Cause : " + ex );
        }

        try {
            NashornScriptEngineFactory nsef = new NashornScriptEngineFactory();
            ScriptEngine engine = nsef.getScriptEngine( BasicDBObject.class.getClassLoader() );
            boolean dbIsSet = false;
            final Bindings binding = engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
            for ( JMongoDatabase db : con.getDatabases() ){
                binding.put( db.getName(), db);
                if ( con.getCatalog() != null && con.getCatalog().equals(db.getName())){
                    binding.put( "db", db );
                    dbIsSet = true;
                }
            }
            if ( !dbIsSet ){
                binding.put( "db", con.getDatabase("admin"));
            }
            engine.eval( "var ObjectId = function( oid ) { return new org.bson.types.ObjectId( oid );}");
            Object obj = engine.eval(sql);
            if ( obj instanceof Iterable){
                lastResultSet = new ResultSetIterator( ((Iterable)obj).iterator() );
            } else if ( obj instanceof Iterator){
                lastResultSet = new ResultSetIterator( (Iterator)obj );
            } else if ( obj instanceof AggregationOutput ){
                lastResultSet = new AggregateResultSet( (AggregationOutput)obj );
            }
            return lastResultSet;
        } catch ( Throwable ex ){
            ex.printStackTrace();
            if( sql.startsWith("mydatabase")){
                throw new SQLException( "Please replace 'mydatabase' and 'mycollection' with the name of the database and collection to be used. " + ex.getMessage() );
            }
            throw new SQLException( ex.getMessage() + "\nExecuted query: " + sql );
        }


        /*
        // https://dzone.com/articles/groovys-smooth-operators

        final Binding binding = new Binding();
        System.out.println("Connection catalog=" + con.getCatalog() );
        for ( JMongoDatabase db : con.getDatabases() ){
            binding.setProperty( db.getName(), db );
            if ( con.getCatalog() != null && con.getCatalog().equals(db.getName())){
                binding.setProperty( "db", db );
            }
        }

        final GroovyShell shell = new GroovyShell( NoSqlPreparedStatement.class.getClassLoader(), binding, GroovyConfiguration.CONFIG );
        final Script script = shell.parse( sql );
        Object obj = script.run();

        if ( obj instanceof Iterable){
            lastResultSet = new NoSqlIteratorResultSet( ((Iterable)obj).iterator() );
        } else if ( obj instanceof Iterator){
            lastResultSet = new NoSqlIteratorResultSet( (Iterator)obj );
        } else if ( obj instanceof AggregationOutput ){
            lastResultSet = new NoSqlAggregateResultSet( (AggregationOutput)obj );
        }
        return lastResultSet;
        */
    }

    public StringBuilder debug( Document doc, String prefix, StringBuilder out ){
        for ( String key : doc.keySet() ){
            Object value = doc.get( key );
            out.append(prefix ).append( key ).append( " = " ).append( getClassDetails( value ) ).append( " " ).append( value ).append( "\n");
            if ( value instanceof Document ){
                debug( (Document)value, prefix + "  ", out );
            }
        }
        return out;
    }

    private String getClassDetails( Object obj ){
        StringBuilder sb = new StringBuilder();
        if ( obj != null ){
            sb.append( "[ Class:").append( obj.getClass().getName() ).append( " implements ");
            for ( Class inf : obj.getClass().getInterfaces() ){
                sb.append( inf.getName());
            }
            sb.append( " ] ").append( obj );
        }
        return sb.toString();
    }


    @Override
    public boolean execute(final String sql) throws SQLException {
        executeQuery( sql );
        return lastResultSet != null;
    }

    private Document documentParam;

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        if ( x instanceof Document ){
            documentParam = (Document)x;
        } else if ( x instanceof Map ){
            documentParam = new Document( (Map)x);
        } else if ( x != null ) {
            throw new SQLException("Map object expected. You currently did setObject( " + x.getClass().getName() + " ) ");
        } else {
            throw new SQLException("Map object expected. You currently did setObject( NULL ) ");
        }
    }

    @Override
    public int executeUpdate() throws SQLException {
        return executeUpdate( sql );
    }

    private JMongoDatabase getDatabase(String name){
        for ( JMongoDatabase scan : con.getDatabases() ){
            if ( scan.getName().equalsIgnoreCase( name )){
                return scan;
            }
        }
        return null;
    }

    private static final Pattern PATTERN_UPDATE = Pattern.compile("UPDATE\\s+(\\w*)\\.(\\w*)", Pattern.CASE_INSENSITIVE );
    private static final Pattern PATTERN_DELETE = Pattern.compile("DELETE\\s+FROM\\s+(\\w*)\\.(\\w*)", Pattern.CASE_INSENSITIVE );
    private static final String ERROR_MESSAGE = "Allowed statements: update(<dbname>.<collectionName>) or delete(<dbname>.<collectionName>). Before calling this do setObject(0,<dbobject>).";

    @Override
    public int executeUpdate( String sql) throws SQLException	{
        if ( sql != null ) {
            if ( documentParam == null ){
                // IF HAS NO PARAMETERS, EXECUTE AS NORMAL SQL
                execute( sql );
                return 1;
            } else {
                sql = sql.trim();
                Matcher matcher = PATTERN_UPDATE.matcher( sql );
                final Object id = documentParam.get("_id");
                if ( matcher.matches() ){
                    JMongoCollection collection = getCollectionMandatory(matcher.group(1), matcher.group(2), true);
                    if (id == null) {
                        collection.insertOne(documentParam);
                    } else {
                        collection.replaceOne( new Document("_id", id), documentParam, new UpdateOptions().upsert(true));
                    }
                    return 1;
                }
                matcher = PATTERN_DELETE.matcher( sql );
                if ( matcher.matches() ){
                    JMongoCollection collection = getCollectionMandatory(matcher.group(1), matcher.group(2), false);
                    collection.deleteOne( (Map)(new Document().append("_id", id)) );
                    return 1;
                }
            }
        }
        throw new SQLException( ERROR_MESSAGE );
    }

    private JMongoCollection getCollectionMandatory( String databaseName, String collectionName, boolean createCollectionIfMissing ) throws SQLException {
        JMongoDatabase mongoDatabase = getDatabase(databaseName);
        if ( mongoDatabase == null ) throw new SQLException( "Cannot find database '" + databaseName + "'.");
        JMongoCollection collection = mongoDatabase.getCollection( collectionName);
        if ( collection == null && createCollectionIfMissing ) {
            mongoDatabase.createCollection( collectionName );
            collection = mongoDatabase.getCollection( collectionName);
        }
        if ( collection == null ) throw new SQLException( "Cannot find collection '" + collectionName + "'.");
        return collection;
    }

    @Override
    public void close() throws SQLException	{
        if (lastResultSet != null) {
            lastResultSet.close();
        }
        this.isClosed = true;
    }

    @Override
    public int getMaxFieldSize() throws SQLException
    {

        return 0;
    }

    @Override
    public void setMaxFieldSize(final int max) throws SQLException{	}

    @Override
    public int getMaxRows() throws SQLException	{
        return maxRows;
    }

    @Override
    public void setMaxRows(final int max) throws SQLException
    {
        this.maxRows = max;
    }

    @Override
    public void setEscapeProcessing(final boolean enable) throws SQLException{}

    @Override
    public int getQueryTimeout() throws SQLException {
        checkClosed();
        throw new SQLFeatureNotSupportedException("MongoDB provides no support for query timeouts.");
    }

    @Override
    public void setQueryTimeout(final int seconds) throws SQLException {
        checkClosed();
        throw new SQLFeatureNotSupportedException("MongoDB provides no support for query timeouts.");
    }

    @Override
    public void cancel() throws SQLException {
        checkClosed();
        throw new SQLFeatureNotSupportedException("MongoDB provides no support for interrupting an operation.");
    }

    @Override
    public SQLWarning getWarnings() throws SQLException	{
        checkClosed();
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException	{
        checkClosed();
    }

    @Override
    public void setCursorName(final String name) throws SQLException {
        checkClosed();
        // Driver doesn't support positioned updates for now, so no-op.
    }

    @Override
    public ResultSet getResultSet() throws SQLException	{
        checkClosed();
        return lastResultSet;
    }

    @Override
    public int getUpdateCount() throws SQLException	{
        checkClosed();
        return 0;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return false;
    }

    @Override
    public void setFetchDirection(final int direction) throws SQLException{}

    @Override
    public int getFetchDirection() throws SQLException {
        return 0;
    }

    @Override
    public void setFetchSize(final int rows) throws SQLException{}

    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return 0;
    }

    @Override
    public int getResultSetType() throws SQLException {
        return 0;
    }

    @Override
    public void addBatch(final String sql) throws SQLException{}

    @Override
    public void clearBatch() throws SQLException{}

    @Override
    public int[] executeBatch() throws SQLException	{
        checkClosed();
        return null;
    }

    @Override
    public Connection getConnection() throws SQLException {
        checkClosed();
        return this.con;
    }

    @Override
    public boolean getMoreResults(final int current) throws SQLException
    {
        checkClosed();
        return false;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException	{
        checkClosed();
        return null;
    }

    @Override
    public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException	{
        checkClosed();
        return 0;
    }

    @Override
    public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
        checkClosed();
        return 0;
    }

    @Override
    public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
        checkClosed();
        return 0;
    }

    @Override
    public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
        checkClosed();
        return false;
    }

    @Override
    public boolean execute(final String sql, final int[] columnIndexes) throws SQLException	{
        checkClosed();
        return false;
    }

    @Override
    public boolean execute(final String sql, final String[] columnNames) throws SQLException {
        checkClosed();
        return false;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    @Override
    public void setPoolable(final boolean poolable) throws SQLException	{}

    @Override
    public boolean isPoolable() throws SQLException	{
        return false;
    }

    private void checkClosed() throws SQLException {
        if (isClosed) {
            throw new SQLException("Statement was previously closed.");
        }
    }

    @Override
    public void closeOnCompletion() throws SQLException {
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return false;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        execute( sql );
        return lastResultSet;
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {

    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {

    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {

    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {

    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {

    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {

    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {

    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {

    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {

    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {

    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {

    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {

    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
    }

    @Override
    public void clearParameters() throws SQLException {
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
    }


    @Override
    public boolean execute() throws SQLException {
        return false;
    }

    @Override
    public void addBatch() throws SQLException {

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {

    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {

    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {

    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {

    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {

    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return null;
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {

    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {

    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {

    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {

    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return null;
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {

    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {

    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {

    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {

    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {

    }
}




/*
try {
ScriptEngineManager engineManager = new ScriptEngineManager();
ScriptEngine engine = engineManager.getEngineByName("nashorn");
//NashornScriptEngineFactory nsef = new NashornScriptEngineFactory();
//ScriptEngine engine = nsef.getScriptEngine( BasicDBObject.class.getClassLoader() );
for ( DB db : con.getDatabases() ){
    engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE).put(db.getName(), db);
}
Object obj = engine.eval(sql);
if ( obj instanceof DBCursor){
    lastResultSet = new NoSqlCursorResultSet( (DBCursor)obj );
} else if ( obj instanceof AggregationOutput ){
    lastResultSet = new NoSqlAggregateResultSet( (AggregationOutput)obj );
} else if ( obj instanceof List){
    lastResultSet = new NoSqlListResultSet( (List)obj );
} else if ( obj instanceof BasicDBObject ){
    lastResultSet = new NoSqlObjectResultSet( (BasicDBObject)obj );
}
System.out.println("Succeed ! " +  ( obj != null ? obj.getClass().getName() : "NULL" ));
return lastResultSet;
} catch ( ScriptException ex ){
System.out.println("Exception at " + ex.getLineNumber() + ", " + ex.getColumnNumber() + " " + ex.getMessage());
} catch ( Exception ex ){
ex.printStackTrace();
}
return null;

else {

ScriptEngineManager factory = new ScriptEngineManager();
// create a JavaScript engine
try {
    ScriptEngine engine = factory.getEngineByName("JavaScript");
    for ( DB db : con.getDatabases() ){
        engine.put( db.getName(), new Jongo( db ) );
        for ( String colName : db.getCollectionNames() ){
            engine.put( db.getName() + "." + colName, db.getCollection( colName ) );
        }
    }
    // evaluate JavaScript code from String
    Object obj = engine.eval( sql);
    if ( obj != null ) System.out.println("Got " + obj.getClass().getName());
    if ( obj instanceof DBCursor){
        lastResultSet = new NoSqlJSonResultSet( (DBCursor)obj );
    }
    return lastResultSet;
} catch ( ScriptException ex ){
    throw new SQLException(ex);
}
}
*/
