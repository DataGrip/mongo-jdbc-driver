package com.dbschema;

import com.dbschema.resultSet.ArrayResultSet;
import com.dbschema.schema.*;

import java.sql.*;
import java.util.List;

/**
 * Mongo databases are equivalent to catalogs for this driver. Schemas aren't used. Mongo collections are
 * equivalent to tables, in that each collection is a table.
 */
public class MongoDatabaseMetaData implements DatabaseMetaData
{
    private static final String DB_NAME = "Mongo";
    private final MongoConnection con;

    public final static String OBJECT_ID_TYPE_NAME = "OBJECT_ID";
    public final static String DOCUMENT_TYPE_NAME = "DOCUMENT";




    public MongoDatabaseMetaData(MongoConnection con) {
        this.con = con;
    }

    /**
     * @see java.sql.DatabaseMetaData#getSchemas()
     */
    @Override
    public ResultSet getSchemas() throws SQLException
    {
        List<String> mongoDbs = con.getService().getDatabaseNames();
        ArrayResultSet retVal = new ArrayResultSet();
        retVal.setColumnNames(new String[]{"TABLE_SCHEM", "TABLE_CATALOG"});
        for (String mongoDb : mongoDbs) {
            retVal.addRow(new String[]{mongoDb, DB_NAME});
        }
        return retVal;
    }

    /**
     * @see java.sql.DatabaseMetaData#getCatalogs()
     */
    @Override
    public ResultSet getCatalogs()
    {
        ArrayResultSet retVal = new ArrayResultSet();
        retVal.setColumnNames(new String[] { "TABLE_CAT" });
        retVal.addRow(new String[]{DB_NAME});
        return retVal;
    }



    /**
     * @see java.sql.DatabaseMetaData#getTables(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String[])
     */
    public ResultSet getTables(String catalogName, String schemaPattern, String tableNamePattern, String[] types)
            throws SQLException
    {
        ArrayResultSet resultSet = new ArrayResultSet();
        resultSet.setColumnNames(new String[]{"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME",
                "TABLE_TYPE", "REMARKS", "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME",
                "REF_GENERATION"});
        if (schemaPattern == null) {
            for ( String cat : con.getService().getDatabaseNames() ) {
                for (String tableName : con.getService().getCollectionNames( cat ) ) {
                    resultSet.addRow(createTableRow(null, tableName));
                }
            }
        } else {
            for (String tableName : con.getService().getCollectionNames(schemaPattern)) {
                resultSet.addRow(createTableRow(schemaPattern, tableName));
            }
        }
        return resultSet;
    }

    private String[] createTableRow( String catalogName, String tableName ){
        String[] data = new String[10];
        data[0] = null; // TABLE_CAT
        data[1] = catalogName; // TABLE_SCHEM
        data[2] = tableName; // TABLE_NAME
        data[3] = "TABLE"; // TABLE_TYPE
        data[4] = ""; // REMARKS
        data[5] = ""; // TYPE_CAT
        data[6] = ""; // TYPE_SCHEM
        data[7] = ""; // TYPE_NAME
        data[8] = ""; // SELF_REFERENCING_COL_NAME
        data[9] = ""; // REF_GENERATION
        return data;
    }


    /**
     * @see java.sql.DatabaseMetaData#getColumns(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    @Override
    public ResultSet getColumns(String catalogName, String schemaName, String tableNamePattern, String columnNamePattern) throws SQLException
    {
        // As far as this driver implementation goes, every "table" in MongoDB is actually a collection, and
        // every collection "table" has two columns - "_id" column which is the primary key, and a "document"
        // column which is the JSON document corresponding to the "_id". An "_id" value can be specified on
        // insert, or it can be omitted, in which case MongoDB generates a unique value.
        MetaCollection collection = con.getService().getMetaCollection(catalogName, tableNamePattern);

        ArrayResultSet result = new ArrayResultSet();
        result.setColumnNames(new String[] { "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME",
                "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX",
                "NULLABLE", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH",
                "ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATLOG", "SCOPE_SCHEMA", "SCOPE_TABLE",
                "SOURCE_DATA_TYPE", "IS_AUTOINCREMENT" });

        if ( collection != null ){
            for ( MetaField field : collection.fields){
                if ( columnNamePattern == null || columnNamePattern.equals( field.name )){
                    exportColumnsRecursive(collection, result, field);
                }
            }
        }
        return result;
    }

    private void exportColumnsRecursive(MetaCollection collection, ArrayResultSet result, MetaField field) {
        result.addRow(new String[] { collection.name, // "TABLE_CAT",
                null, // "TABLE_SCHEMA",
                collection.name, // "TABLE_NAME", (i.e. MongoDB Collection Name)
                field.getNameWithPath(), // "COLUMN_NAME",
                "" + field.type, // "DATA_TYPE",
                field.typeName, // "TYPE_NAME",
                "800", // "COLUMN_SIZE",
                "0", // "BUFFER_LENGTH", (not used)
                "0", // "DECIMAL_DIGITS",
                "10", // "NUM_PREC_RADIX",
                "" + ( field.isMandatory() ? columnNoNulls : columnNullable ), // "NULLABLE",
                "", // "REMARKS",
                "", // "COLUMN_DEF",
                "0", // "SQL_DATA_TYPE", (not used)
                "0", // "SQL_DATETIME_SUB", (not used)
                "800", // "CHAR_OCTET_LENGTH",
                "1", // "ORDINAL_POSITION",
                "NO", // "IS_NULLABLE",
                null, // "SCOPE_CATLOG", (not a REF type)
                null, // "SCOPE_SCHEMA", (not a REF type)
                null, // "SCOPE_TABLE", (not a REF type)
                null, // "SOURCE_DATA_TYPE", (not a DISTINCT or REF type)
                "NO" // "IS_AUTOINCREMENT" (can be auto-generated, but can also be specified)
        });
        if( field instanceof MetaJson){
            MetaJson json = (MetaJson)field;
            for ( MetaField children : json.fields){
                exportColumnsRecursive( collection, result,  children );
            }
        }
    }


    /**
     * @see java.sql.DatabaseMetaData#getPrimaryKeys(java.lang.String, java.lang.String, java.lang.String)
     */
    public ResultSet getPrimaryKeys(String catalogName, String schemaName, String tableNamePattern) throws SQLException
    {
        /*
        * 	<LI><B>TABLE_CAT</B> String => table catalog (may be <code>null</code>)
       *	<LI><B>TABLE_SCHEM</B> String => table schema (may be <code>null</code>)
       *	<LI><B>TABLE_NAME</B> String => table name
       *	<LI><B>COLUMN_NAME</B> String => column name
       *	<LI><B>KEY_SEQ</B> short => sequence number within primary key( a value
       *  of 1 represents the first column of the primary key, a value of 2 would
       *  represent the second column within the primary key).
       *	<LI><B>PK_NAME</B> Stri
        *
        */

        ArrayResultSet result = new ArrayResultSet();
        result.setColumnNames(new String[] { "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME",
                "KEY_SEQ", "PK_NAME" });

        MetaCollection collection = con.getService().getMetaCollection(catalogName, tableNamePattern);
        if ( collection != null ){
            for ( MetaIndex index : collection.metaIndexes){
                if ( index.pk ) {
                    for ( MetaField field : index.metaFields){
                        result.addRow( new String[] {
                                collection.name, // "TABLE_CAT",
                                null, // "TABLE_SCHEMA",
                                collection.name, // "TABLE_NAME", (i.e. MongoDB Collection Name)
                                field.getNameWithPath(), // "COLUMN_NAME",
                                "" + index.metaFields.indexOf( field ) , // "ORDINAL_POSITION"
                                index.name // "INDEX_NAME",
                        });
                    }
                }
            }
        }
        return result;
    }


    /**
     * @see java.sql.DatabaseMetaData#getIndexInfo(java.lang.String, java.lang.String, java.lang.String,
     *      boolean, boolean)
     */
    public ResultSet getIndexInfo(String catalogName, String schemaName, String tableNamePattern, boolean unique,
                                  boolean approximate) throws SQLException
    {
        /*
        *      *  <OL>
            *	<LI><B>TABLE_CAT</B> String => table catalog (may be <code>null</code>)
            *	<LI><B>TABLE_SCHEMA</B> String => table schema (may be <code>null</code>)
            *	<LI><B>TABLE_NAME</B> String => table name
            *	<LI><B>NON_UNIQUE</B> boolean => Can index values be non-unique.
            *      false when TYPE is tableIndexStatistic
            *	<LI><B>INDEX_QUALIFIER</B> String => index catalog (may be <code>null</code>);
            *      <code>null</code> when TYPE is tableIndexStatistic
            *	<LI><B>INDEX_NAME</B> String => index name; <code>null</code> when TYPE is
            *      tableIndexStatistic
            *	<LI><B>TYPE</B> short => index type:
            *      <UL>
            *      <LI> tableIndexStatistic - this identifies table statistics that are
            *           returned in conjuction with a table's index descriptions
            *      <LI> tableIndexClustered - this is a clustered index
            *      <LI> tableIndexHashed - this is a hashed index
            *      <LI> tableIndexOther - this is some other style of index
            *      </UL>
            *	<LI><B>ORDINAL_POSITION</B> short => column sequence number
            *      within index; zero when TYPE is tableIndexStatistic
            *	<LI><B>COLUMN_NAME</B> String => column name; <code>null</code> when TYPE is
            *      tableIndexStatistic
            *	<LI><B>ASC_OR_DESC</B> String => column sort sequence, "A" => ascending,
            *      "D" => descending, may be <code>null</code> if sort sequence is not supported;
            *      <code>null</code> when TYPE is tableIndexStatistic
            *	<LI><B>CARDINALITY</B> int => When TYPE is tableIndexStatistic, then
            *      this is the number of rows in the table; otherwise, it is the
            *      number of unique values in the index.
            *	<LI><B>PAGES</B> int => When TYPE is  tableIndexStatisic then
            *      this is the number of pages used for the table, otherwise it
            *      is the number of pages used for the current index.
            *	<LI><B>FILTER_CONDITION</B> String => Filter condition, if any.
            *      (may be <code>null</code>)
            *  </OL>
        */
        ArrayResultSet result = new ArrayResultSet();
        result.setColumnNames(new String[]{"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE",
                "INDEX_QUALIFIER", "INDEX_NAME", "TYPE", "ORDINAL_POSITION", "COLUMN_NAME", "ASC_OR_DESC",
                "CARDINALITY", "PAGES", "FILTER_CONDITION"});

        MetaCollection collection = con.getService().getMetaCollection(catalogName, tableNamePattern);

        if ( collection != null ){
            for ( MetaIndex index : collection.metaIndexes){
                if ( !index.pk ){
                    for ( MetaField field : index.metaFields){
                        result.addRow(new String[] { collection.name, // "TABLE_CAT",
                                null, // "TABLE_SCHEMA",
                                collection.name, // "TABLE_NAME", (i.e. MongoDB Collection Name)
                                "YES", // "NON-UNIQUE",
                                collection.name, // "INDEX QUALIFIER",
                                index.name, // "INDEX_NAME",
                                "0", // "TYPE",
                                "" + index.metaFields.indexOf( field ) , // "ORDINAL_POSITION"
                                field.getNameWithPath(), // "COLUMN_NAME",
                                "A", // "ASC_OR_DESC",
                                "0", // "CARDINALITY",
                                "0", // "PAGES",
                                "" // "FILTER_CONDITION",
                        });
                    }
                }
            }
        }
        return result;
    }

    /**
     * @see java.sql.DatabaseMetaData#getTypeInfo()
     */
    public ResultSet getTypeInfo() throws SQLException
    {
        /*
            * <P>Each type description has the following columns:
            *  <OL>
            *	<LI><B>TYPE_NAME</B> String => Type name
            *	<LI><B>DATA_TYPE</B> int => SQL data type from java.sql.Types
            *	<LI><B>PRECISION</B> int => maximum precision
            *	<LI><B>LITERAL_PREFIX</B> String => prefix used to quote a literal
            *      (may be <code>null</code>)
            *	<LI><B>LITERAL_SUFFIX</B> String => suffix used to quote a literal
            (may be <code>null</code>)
            *	<LI><B>CREATE_PARAMS</B> String => parameters used in creating
            *      the type (may be <code>null</code>)
            *	<LI><B>NULLABLE</B> short => can you use NULL for this type.
            *      <UL>
            *      <LI> typeNoNulls - does not allow NULL values
            *      <LI> typeNullable - allows NULL values
            *      <LI> typeNullableUnknown - nullability unknown
            *      </UL>
            *	<LI><B>CASE_SENSITIVE</B> boolean=> is it case sensitive.
            *	<LI><B>SEARCHABLE</B> short => can you use "WHERE" based on this type:
            *      <UL>
            *      <LI> typePredNone - No support
            *      <LI> typePredChar - Only supported with WHERE .. LIKE
            *      <LI> typePredBasic - Supported except for WHERE .. LIKE
            *      <LI> typeSearchable - Supported for all WHERE ..
            *      </UL>
            *	<LI><B>UNSIGNED_ATTRIBUTE</B> boolean => is it unsigned.
            *	<LI><B>FIXED_PREC_SCALE</B> boolean => can it be a money value.
            *	<LI><B>AUTO_INCREMENT</B> boolean => can it be used for an
            *      auto-increment value.
            *	<LI><B>LOCAL_TYPE_NAME</B> String => localized version of type name
            *      (may be <code>null</code>)
            *	<LI><B>MINIMUM_SCALE</B> short => minimum scale supported
            *	<LI><B>MAXIMUM_SCALE</B> short => maximum scale supported
            *	<LI><B>SQL_DATA_TYPE</B> int => unused
            *	<LI><B>SQL_DATETIME_SUB</B> int => unused
            *	<LI><B>NUM_PREC_RADIX</B> int => usually 2 or 10
            *  </OL>
        */
        ArrayResultSet retVal = new ArrayResultSet();
        retVal.setColumnNames(new String[] { "TYPE_NAME", "DATA_TYPE", "PRECISION", "LITERAL_PREFIX",
                "LITERAL_SUFFIX", "CREATE_PARAMS", "NULLABLE", "CASE_SENSITIVE", "SEARCHABLE",
                "UNSIGNED_ATTRIBUTE", "FIXED_PREC_SCALE", "AUTO_INCREMENT", "LOCAL_TYPE_NAME", "MINIMUM_SCALE",
                "MAXIMUM_SCALE", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "NUM_PREC_RADIX" });

        retVal.addRow(new String[] { OBJECT_ID_TYPE_NAME, // "TYPE_NAME",
                "" + Types.VARCHAR, // "DATA_TYPE",
                "800", // "PRECISION",
                "'", // "LITERAL_PREFIX",
                "'", // "LITERAL_SUFFIX",
                null, // "CREATE_PARAMS",
                "" + typeNullable, // "NULLABLE",
                "true", // "CASE_SENSITIVE",
                "" + typeSearchable, // "SEARCHABLE",
                "false", // "UNSIGNED_ATTRIBUTE",
                "false", // "FIXED_PREC_SCALE",
                "false", // "AUTO_INCREMENT",
                OBJECT_ID_TYPE_NAME, // "LOCAL_TYPE_NAME",
                "0", // "MINIMUM_SCALE",
                "0", // "MAXIMUM_SCALE",
                null, // "SQL_DATA_TYPE", (not used)
                null, // "SQL_DATETIME_SUB", (not used)
                "10", // "NUM_PREC_RADIX" (javadoc says usually 2 or 10)
        });

        retVal.addRow(new String[] { DOCUMENT_TYPE_NAME, // "TYPE_NAME",
                "" + Types.CLOB, // "DATA_TYPE",
                "16777216", // "PRECISION",
                "'", // "LITERAL_PREFIX",
                "'", // "LITERAL_SUFFIX",
                null, // "CREATE_PARAMS",
                "" + typeNullable, // "NULLABLE",
                "true", // "CASE_SENSITIVE",
                "" + typeSearchable, // "SEARCHABLE",
                "false", // "UNSIGNED_ATTRIBUTE",
                "false", // "FIXED_PREC_SCALE",
                "false", // "AUTO_INCREMENT",
                DOCUMENT_TYPE_NAME, // "LOCAL_TYPE_NAME",
                "0", // "MINIMUM_SCALE",
                "0", // "MAXIMUM_SCALE",
                null, // "SQL_DATA_TYPE", (not used)
                null, // "SQL_DATETIME_SUB", (not used)
                "10", // "NUM_PREC_RADIX" (javadoc says usually 2 or 10)
        });
        return retVal;
    }








    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    public boolean allProceduresAreCallable() throws SQLException {
        return false;
    }

    public boolean allTablesAreSelectable() throws SQLException {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#getURL()
     */
    public String getURL() throws SQLException {
        return con.getUrl();
    }

    public String getUserName() throws SQLException {
        return null;
    }

    public boolean isReadOnly() throws SQLException {
        return false;
    }

    public boolean nullsAreSortedHigh() throws SQLException
    {
        return false;
    }

    public boolean nullsAreSortedLow() throws SQLException {
        return false;
    }

    public boolean nullsAreSortedAtStart() throws SQLException {
        return false;
    }

    public boolean nullsAreSortedAtEnd() throws SQLException {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#getDatabaseProductName()
     */
    public String getDatabaseProductName() throws SQLException {
        return "Mongo DB";
    }

    /**
     * @see java.sql.DatabaseMetaData#getDatabaseProductVersion()
     */
    public String getDatabaseProductVersion() throws SQLException {
        final String version = con.getService().getVersion();
        return version!= null ? version : "Unknown";
    }

    /**
     * @see java.sql.DatabaseMetaData#getDriverName()
     */
    public String getDriverName() throws SQLException
    {
        return "MongoDB JDBC Driver";
    }

    /**
     * @see java.sql.DatabaseMetaData#getDriverVersion()
     */
    public String getDriverVersion() throws SQLException
    {
        return "1.0";
    }

    /**
     * @see java.sql.DatabaseMetaData#getDriverMajorVersion()
     */
    public int getDriverMajorVersion()
    {
        return 1;
    }

    /**
     * @see java.sql.DatabaseMetaData#getDriverMinorVersion()
     */
    public int getDriverMinorVersion()
    {
        return 0;
    }

    public boolean usesLocalFiles() throws SQLException
    {

        return false;
    }

    public boolean usesLocalFilePerTable() throws SQLException
    {
        return false;
    }

    public boolean supportsMixedCaseIdentifiers() throws SQLException
    {
        return false;
    }

    public boolean storesUpperCaseIdentifiers() throws SQLException
    {
        return false;
    }

    public boolean storesLowerCaseIdentifiers() throws SQLException
    {
        return false;
    }

    public boolean storesMixedCaseIdentifiers() throws SQLException
    {
        return false;
    }

    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException
    {
        return false;
    }

    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException
    {
        return false;
    }

    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException
    {
        return false;
    }

    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException
    {

        return false;
    }

    public String getIdentifierQuoteString() throws SQLException
    {

        return null;
    }

    public String getSQLKeywords() throws SQLException
    {

        return null;
    }

    public String getNumericFunctions() throws SQLException
    {

        return null;
    }

    public String getStringFunctions() throws SQLException
    {

        return null;
    }

    public String getSystemFunctions() throws SQLException
    {

        return null;
    }

    public String getTimeDateFunctions() throws SQLException
    {
        return "date";
    }

    public String getSearchStringEscape() throws SQLException
    {

        return null;
    }

    public String getExtraNameCharacters() throws SQLException
    {

        return null;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsAlterTableWithAddColumn()
     */
    public boolean supportsAlterTableWithAddColumn() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsAlterTableWithDropColumn()
     */
    public boolean supportsAlterTableWithDropColumn() throws SQLException
    {
        return false;
    }

    public boolean supportsColumnAliasing() throws SQLException
    {

        return false;
    }

    public boolean nullPlusNonNullIsNull() throws SQLException
    {

        return false;
    }

    public boolean supportsConvert() throws SQLException
    {

        return false;
    }

    public boolean supportsConvert(int fromType, int toType) throws SQLException
    {

        return false;
    }

    public boolean supportsTableCorrelationNames() throws SQLException
    {

        return false;
    }

    public boolean supportsDifferentTableCorrelationNames() throws SQLException
    {

        return false;
    }

    public boolean supportsExpressionsInOrderBy() throws SQLException
    {

        return false;
    }

    public boolean supportsOrderByUnrelated() throws SQLException
    {

        return false;
    }

    public boolean supportsGroupBy() throws SQLException
    {

        return false;
    }

    public boolean supportsGroupByUnrelated() throws SQLException
    {

        return false;
    }

    public boolean supportsGroupByBeyondSelect() throws SQLException
    {

        return false;
    }

    public boolean supportsLikeEscapeClause() throws SQLException
    {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsMultipleResultSets()
     */
    public boolean supportsMultipleResultSets() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsMultipleTransactions()
     */
    public boolean supportsMultipleTransactions() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsNonNullableColumns()
     */
    public boolean supportsNonNullableColumns() throws SQLException
    {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsMinimumSQLGrammar()
     */
    public boolean supportsMinimumSQLGrammar() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCoreSQLGrammar()
     */
    public boolean supportsCoreSQLGrammar() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsExtendedSQLGrammar()
     */
    public boolean supportsExtendedSQLGrammar() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsANSI92EntryLevelSQL()
     */
    public boolean supportsANSI92EntryLevelSQL() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsANSI92IntermediateSQL()
     */
    public boolean supportsANSI92IntermediateSQL() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsANSI92FullSQL()
     */
    public boolean supportsANSI92FullSQL() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsIntegrityEnhancementFacility()
     */
    public boolean supportsIntegrityEnhancementFacility() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsOuterJoins()
     */
    public boolean supportsOuterJoins() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsFullOuterJoins()
     */
    public boolean supportsFullOuterJoins() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsLimitedOuterJoins()
     */
    public boolean supportsLimitedOuterJoins() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#getSchemaTerm()
     */
    public String getSchemaTerm() throws SQLException
    {
        return null;
    }

    /**
     * @see java.sql.DatabaseMetaData#getProcedureTerm()
     */
    public String getProcedureTerm() throws SQLException
    {
        return null;
    }

    /**
     * @see java.sql.DatabaseMetaData#getCatalogTerm()
     */
    public String getCatalogTerm() throws SQLException
    {
        return "database";
    }

    /**
     * @see java.sql.DatabaseMetaData#isCatalogAtStart()
     */
    public boolean isCatalogAtStart() throws SQLException
    {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#getCatalogSeparator()
     */
    public String getCatalogSeparator() throws SQLException
    {
        return ".";
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSchemasInDataManipulation()
     */
    public boolean supportsSchemasInDataManipulation() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSchemasInProcedureCalls()
     */
    public boolean supportsSchemasInProcedureCalls() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSchemasInTableDefinitions()
     */
    public boolean supportsSchemasInTableDefinitions() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSchemasInIndexDefinitions()
     */
    public boolean supportsSchemasInIndexDefinitions() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSchemasInPrivilegeDefinitions()
     */
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCatalogsInDataManipulation()
     */
    public boolean supportsCatalogsInDataManipulation() throws SQLException
    {
        return true;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCatalogsInProcedureCalls()
     */
    public boolean supportsCatalogsInProcedureCalls() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCatalogsInTableDefinitions()
     */
    public boolean supportsCatalogsInTableDefinitions() throws SQLException
    {
        return false;
    }

    public boolean supportsCatalogsInIndexDefinitions() throws SQLException
    {

        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsCatalogsInPrivilegeDefinitions()
     */
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsPositionedDelete()
     */
    public boolean supportsPositionedDelete() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsPositionedUpdate()
     */
    public boolean supportsPositionedUpdate() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSelectForUpdate()
     */
    public boolean supportsSelectForUpdate() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsStoredProcedures()
     */
    public boolean supportsStoredProcedures() throws SQLException
    {
        return false;
    }

    public boolean supportsSubqueriesInComparisons() throws SQLException
    {

        return false;
    }

    public boolean supportsSubqueriesInExists() throws SQLException
    {

        return false;
    }

    public boolean supportsSubqueriesInIns() throws SQLException
    {

        return false;
    }

    public boolean supportsSubqueriesInQuantifieds() throws SQLException
    {

        return false;
    }

    public boolean supportsCorrelatedSubqueries() throws SQLException
    {

        return false;
    }

    public boolean supportsUnion() throws SQLException
    {

        return false;
    }

    public boolean supportsUnionAll() throws SQLException
    {

        return false;
    }

    public boolean supportsOpenCursorsAcrossCommit() throws SQLException
    {

        return false;
    }

    public boolean supportsOpenCursorsAcrossRollback() throws SQLException
    {

        return false;
    }

    public boolean supportsOpenStatementsAcrossCommit() throws SQLException
    {

        return false;
    }

    public boolean supportsOpenStatementsAcrossRollback() throws SQLException
    {

        return false;
    }

    public int getMaxBinaryLiteralLength() throws SQLException
    {

        return 0;
    }

    public int getMaxCharLiteralLength() throws SQLException
    {

        return 0;
    }

    public int getMaxColumnNameLength() throws SQLException
    {

        return 0;
    }

    public int getMaxColumnsInGroupBy() throws SQLException
    {

        return 0;
    }

    public int getMaxColumnsInIndex() throws SQLException
    {

        return 0;
    }

    public int getMaxColumnsInOrderBy() throws SQLException
    {

        return 0;
    }

    public int getMaxColumnsInSelect() throws SQLException
    {

        return 0;
    }

    public int getMaxColumnsInTable() throws SQLException
    {

        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxConnections()
     */
    public int getMaxConnections() throws SQLException
    {
        return 0;
    }

    public int getMaxCursorNameLength() throws SQLException
    {

        return 0;
    }

    public int getMaxIndexLength() throws SQLException
    {

        return 0;
    }

    public int getMaxSchemaNameLength() throws SQLException
    {

        return 0;
    }

    public int getMaxProcedureNameLength() throws SQLException
    {

        return 0;
    }

    public int getMaxCatalogNameLength() throws SQLException
    {
        return 0;
    }

    public int getMaxRowSize() throws SQLException
    {

        return 0;
    }

    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException
    {

        return false;
    }

    public int getMaxStatementLength() throws SQLException
    {

        return 0;
    }

    public int getMaxStatements() throws SQLException
    {

        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxTableNameLength()
     */
    public int getMaxTableNameLength() throws SQLException
    {
        /*
        * The maximum size of a collection name is 128 characters (including the name of the db and indexes).
        * It is probably best to keep it under 80/90 chars.
        */
        return 90;
    }

    /**
     * @see java.sql.DatabaseMetaData#getMaxTablesInSelect()
     */
    public int getMaxTablesInSelect() throws SQLException
    {
        // MongoDB collections are represented as SQL tables in this driver. Mongo doesn't support joins.
        return 1;
    }

    public int getMaxUserNameLength() throws SQLException
    {

        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getDefaultTransactionIsolation()
     */
    public int getDefaultTransactionIsolation() throws SQLException
    {
        return Connection.TRANSACTION_NONE;
    }

    /**
     * MongoDB doesn't support transactions, but document updates are atomic.
     *
     * @see java.sql.DatabaseMetaData#supportsTransactions()
     */
    public boolean supportsTransactions() throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsTransactionIsolationLevel(int)
     */
    public boolean supportsTransactionIsolationLevel(int level) throws SQLException
    {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsDataDefinitionAndDataManipulationTransactions()
     */
    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException
    {
        return false;
    }

    public boolean supportsDataManipulationTransactionsOnly() throws SQLException
    {

        return false;
    }

    public boolean dataDefinitionCausesTransactionCommit() throws SQLException
    {

        return false;
    }

    public boolean dataDefinitionIgnoredInTransactions() throws SQLException
    {

        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#getProcedures(java.lang.String, java.lang.String, java.lang.String)
     */
    public ResultSet getProcedures(String catalogName, String schemaPattern, String procedureNamePattern)
            throws SQLException
    {
        ArrayResultSet retVal = new ArrayResultSet();
        retVal.setColumnNames(new String[] { "PROCEDURE_CAT", "PROCEDURE_SCHEMA", "PROCEDURE_NAME", "REMARKS",
                "PROCEDURE_TYPE", "SPECIFIC_NAME" });
        return retVal;
    }

    /**
     * @see java.sql.DatabaseMetaData#getProcedureColumns(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    @Override
    public ResultSet getProcedureColumns(String catalogName, String schemaPattern, String procedureNamePattern,
                                         String columnNamePattern) throws SQLException
    {
        return empty();
    }

    private static ResultSet empty()
    {
        return new ArrayResultSet();
    }

    /**
     * @see java.sql.DatabaseMetaData#getTableTypes()
     */
    @Override
    public ResultSet getTableTypes() throws SQLException
    {
        ArrayResultSet result = new ArrayResultSet();
        result.addRow(new String[] { "COLLECTION" });
        return result;
    }


    @Override
    public ResultSet getColumnPrivileges(String catalogName, String schemaName, String table, String columnNamePattern)
            throws SQLException
    {

        return null;
    }

    @Override
    public ResultSet getTablePrivileges(String catalogName, String schemaPattern, String tableNamePattern)
            throws SQLException
    {

        return null;
    }

    @Override
    public ResultSet getBestRowIdentifier(String catalogName, String schemaName, String table, int scope,
                                          boolean nullable) throws SQLException
    {

        return null;
    }

    /**
     * @see java.sql.DatabaseMetaData#getVersionColumns(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getVersionColumns(String catalogName, String schemaName, String table) throws SQLException
    {
        return empty();
    }

    @Override
    public ResultSet getExportedKeys(String catalogName, String schemaName, String tableNamePattern) throws SQLException {
        con.getService().discoverReferences();


        ArrayResultSet result = new ArrayResultSet();
        result.setColumnNames(new String[]{"PKTABLE_CAT", "PKTABLE_SCHEMA", "PKTABLE_NAME", "PKCOLUMN_NAME", "FKTABLE_CAT", "FKTABLE_SCHEM",
                "FKTABLE_NAME", "FKCOLUMN_NAME", "KEY_SEQ", "UPDATE_RULE", "DELETE_RULE", "FK_NAME", "PK_NAME", "DEFERRABILITY"});

        MetaCollection pkCollection = con.getService().getMetaCollection(catalogName, tableNamePattern);
        if ( pkCollection != null ){
            for ( MetaCollection fromCollection : con.getService().getMetaCollections() ){
                for ( MetaField fromFiled : fromCollection.fields ){
                    getExportedKeysRecursive(result, pkCollection, fromCollection, fromFiled);
                }
            }
        }
        return result;
    }

    private void getExportedKeysRecursive(ArrayResultSet result, MetaCollection pkCollection, MetaCollection fromCollection, MetaField fromFiled) {
        for ( MetaReference iReference : fromFiled.references){
            if ( iReference.pkCollection == pkCollection ){

                result.addRow(new String[] {
                        pkCollection.db, //PKTABLE_CAT
                        null, //PKTABLE_SCHEM
                        pkCollection.name,//PKTABLE_NAME
                        "_id", //PKCOLUMN_NAME
                        fromCollection.db,//FKTABLE_CAT
                        null, //FKTABLE_SCHEM
                        fromFiled.getMetaCollection().name, //FKTABLE_NAME
                        iReference.fromField.getNameWithPath(),//FKCOLUMN_NAME
                        "1",//KEY_SEQ 1,2
                        ""+ DatabaseMetaData.importedKeyNoAction, //UPDATE_RULE
                        ""+DatabaseMetaData.importedKeyNoAction, //DELETE_RULE
                        "Ref", //FK_NAME
                        null, //PK_NAME
                        ""+DatabaseMetaData.importedKeyInitiallyImmediate //DEFERRABILITY
                });
            }
        }
        if ( fromFiled instanceof MetaJson){
            for ( MetaField field : ((MetaJson) fromFiled).fields ){
                getExportedKeysRecursive(result, pkCollection, fromCollection, field);
            }
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getExportedKeys(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getImportedKeys(String catalogName, String schemaName, String tableNamePattern ) throws SQLException
    {
        con.getService().discoverReferences();


        ArrayResultSet result = new ArrayResultSet();
        result.setColumnNames(new String[]{"PKTABLE_CAT", "PKTABLE_SCHEM", "PKTABLE_NAME", "PKCOLUMN_NAME", "FKTABLE_CAT", "FKTABLE_SCHEM",
                "FKTABLE_NAME", "FKCOLUMN_NAME", "KEY_SEQ", "UPDATE_RULE", "DELETE_RULE", "FK_NAME", "PK_NAME", "DEFERRABILITY"});


        MetaCollection fromCollection = con.getService().getMetaCollection(catalogName, tableNamePattern);
        if ( fromCollection != null ){
            for ( MetaField fromFiled : fromCollection.fields ){
                getImportedKeysRecursive(result, fromFiled);
            }
        }
        return result;
    }

    private void getImportedKeysRecursive(ArrayResultSet result, MetaField fromFiled) {
        for ( MetaReference reference : fromFiled.references ){

            result.addRow(new String[] {
                    reference.pkCollection.db, //PKTABLE_CAT
                    null, //PKTABLE_SCHEMA
                    reference.pkCollection.name,//PKTABLE_NAME
                    "_id", //PKCOLUMN_NAME
                    reference.fromField.getMetaCollection().db,//FKTABLE_CAT
                    null, //FKTABLE_SCHEM
                    reference.fromField.getMetaCollection().name, //FKTABLE_NAME
                    reference.fromField.getNameWithPath(),//FKCOLUMN_NAME
                    "1",//KEY_SEQ 1,2
                    ""+ DatabaseMetaData.importedKeyNoAction, //UPDATE_RULE
                    ""+DatabaseMetaData.importedKeyNoAction, //DELETE_RULE
                    "Ref", //FK_NAME
                    null, //PK_NAME
                    ""+DatabaseMetaData.importedKeyInitiallyImmediate //DEFERRABILITY
            });
        }
        if ( fromFiled instanceof MetaJson){
            for ( MetaField field : ((MetaJson) fromFiled).fields ){
                getImportedKeysRecursive(result, field);
            }
        }
    }

    /**
     * @see java.sql.DatabaseMetaData#getCrossReference(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable,
                                       String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException
    {
        return empty();
    }


    /**
     * @see java.sql.DatabaseMetaData#supportsResultSetType(int)
     */
    @Override
    public boolean supportsResultSetType(int type) throws SQLException {
        return type == ResultSet.TYPE_FORWARD_ONLY;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsResultSetConcurrency(int, int)
     */
    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException	{
        return false;
    }

    @Override
    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean ownDeletesAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean ownInsertsAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean othersDeletesAreVisible(int type) throws SQLException
    {

        return false;
    }

    @Override
    public boolean othersInsertsAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean updatesAreDetected(int type) throws SQLException	{
        return false;
    }

    @Override
    public boolean deletesAreDetected(int type) throws SQLException	{
        return false;
    }

    @Override
    public boolean insertsAreDetected(int type) throws SQLException	{
        return false;
    }

    @Override
    public boolean supportsBatchUpdates() throws SQLException {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#getUDTs(java.lang.String, java.lang.String, java.lang.String, int[])
     */
    @Override
    public ResultSet getUDTs(String catalogName, String schemaPattern, String typeNamePattern, int[] types)
            throws SQLException	{
        ArrayResultSet retVal = new ArrayResultSet();
        retVal.setColumnNames(new String[] { "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "CLASS_NAME", "DATA_TYPE",
                "REMARKS", "BASE_TYPE", });
        return retVal;
    }

    /**
     * @see java.sql.DatabaseMetaData#getConnection()
     */
    @Override
    public Connection getConnection() throws SQLException {
        return con;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsSavepoints()
     */
    @Override
    public boolean supportsSavepoints() throws SQLException	{
        return false;
    }

    @Override
    public boolean supportsNamedParameters() throws SQLException {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsMultipleOpenResults()
     */
    @Override
    public boolean supportsMultipleOpenResults() throws SQLException {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
     */
    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#getSuperTypes(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getSuperTypes(String catalogName, String schemaPattern, String typeNamePattern)
            throws SQLException
    {
        ArrayResultSet retVal = new ArrayResultSet();
        retVal.setColumnNames(new String[] { "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SUPERTYPE_CAT",
                "SUPERTYPE_SCHEM", "SUPERTYPE_NAME" });
        return retVal;
    }

    /**
     * @see java.sql.DatabaseMetaData#getSuperTables(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getSuperTables(String catalogName, String schemaPattern, String tableNamePattern)
            throws SQLException
    {
        ArrayResultSet retVal = new ArrayResultSet();
        retVal.setColumnNames(new String[] { "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "SUPERTABLE_NAME" });
        return retVal;
    }

    /**
     * @see java.sql.DatabaseMetaData#getAttributes(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    @Override
    public ResultSet getAttributes(String catalogName, String schemaPattern, String typeNamePattern,
                                   String attributeNamePattern) throws SQLException {
        ArrayResultSet retVal = new ArrayResultSet();
        retVal.setColumnNames(new String[] { "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "ATTR_NAME", "DATA_TYPE",
                "ATTR_TYPE_NAME", "ATTR_SIZE", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "NULLABLE", "REMARKS",
                "ATTR_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION",
                "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE" });
        return retVal;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsResultSetHoldability(int)
     */
    @Override
    public boolean supportsResultSetHoldability(int holdability) throws SQLException {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#getResultSetHoldability()
     */
    @Override
    public int getResultSetHoldability() throws SQLException {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    /**
     * @see java.sql.DatabaseMetaData#getDatabaseMajorVersion()
     */
    @Override
    public int getDatabaseMajorVersion() throws SQLException {
        return Integer.parseInt((getDatabaseProductVersion().split("\\."))[0]);
    }

    /**
     * @see java.sql.DatabaseMetaData#getDatabaseMinorVersion()
     */
    @Override
    public int getDatabaseMinorVersion() throws SQLException {
        return Integer.parseInt((getDatabaseProductVersion().split("\\."))[1]);
    }

    /**
     * @see java.sql.DatabaseMetaData#getJDBCMajorVersion()
     */
    @Override
    public int getJDBCMajorVersion() throws SQLException {
        return 1;
    }

    /**
     * @see java.sql.DatabaseMetaData#getJDBCMinorVersion()
     */
    @Override
    public int getJDBCMinorVersion() throws SQLException {
        return 0;
    }

    /**
     * @see java.sql.DatabaseMetaData#getSQLStateType()
     */
    @Override
    public int getSQLStateType() throws SQLException {
        return DatabaseMetaData.sqlStateXOpen;
    }

    @Override
    public boolean locatorsUpdateCopy() throws SQLException	{
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsStatementPooling()
     */
    @Override
    public boolean supportsStatementPooling() throws SQLException {
        return false;
    }

    /**
     * @see java.sql.DatabaseMetaData#getRowIdLifetime()
     */
    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException	{
        return null;
    }

    /**
     * @see java.sql.DatabaseMetaData#getSchemas(java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getSchemas(String catalogName, String schemaPattern) throws SQLException {
        ArrayResultSet retVal = new ArrayResultSet();
        retVal.setColumnNames(new String[] { "TABLE_SCHEM", "TABLE_CATALOG" });
        return retVal;
    }

    /**
     * @see java.sql.DatabaseMetaData#supportsStoredFunctionsUsingCallSyntax()
     */
    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException	{
        return false;
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        return false;
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException	{
        return null;
    }

    @Override
    public ResultSet getFunctions(String catalogName, String schemaPattern, String functionNamePattern)
            throws SQLException	{
        return null;
    }

    @Override
    public ResultSet getFunctionColumns(String catalogName, String schemaPattern, String functionNamePattern,
                                        String columnNamePattern) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getPseudoColumns(String catalogName, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        return null;
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        return false;
    }
}
