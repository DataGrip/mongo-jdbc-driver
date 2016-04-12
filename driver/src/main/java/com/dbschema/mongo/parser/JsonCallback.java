package com.dbschema.mongo.parser;


public interface JsonCallback {

    /**
     * Signals the start of a BSON document, which usually maps onto some Java object.
     *
     * @mongodb.driver.manual core/document/ MongoDB Documents
     */
    void objectStart();

    /**
     * Signals the start of a BSON document, which usually maps onto some Java object.
     *
     * @param name the field name of the document.
     * @mongodb.driver.manual core/document/ MongoDB Documents
     */
    void objectStart(String name);

    /**
     * Called at the end of the document/array, and returns this object.
     *
     * @return the Object that has been read from this section of the document.
     */
    Object objectDone();

    /**
     * Resets the callback, clearing all state.
     */
    void reset();

    /**
     * Returns the finished top-level Document.
     *
     * @return the top level document read from the database.
     */
    Object get();

    /**
     * Factory method for BSONCallbacks.
     *
     * @return a new BSONCallback.
     */
    JsonCallback createBSONCallback();

    /**
     * Signals the start of a BSON array.
     *
     * @mongodb.driver.manual tutorial/query-documents/#read-operations-arrays Arrays
     */
    void arrayStart();

    /**
     * Signals the start of a BSON array, with its field name.
     *
     * @param name the name of this array field
     * @mongodb.driver.manual tutorial/query-documents/#read-operations-arrays Arrays
     */
    void arrayStart(String name);

    /**
     * Called the end of the array, and returns the completed array.
     *
     * @return an Object representing the array that has been read from this section of the document.
     */
    Object arrayDone();

    /**
     * Called when reading a BSON field that exists but has a null value.
     *
     * @param name the name of the field
     */
    void gotNull(String name);

    /**
     * Called when reading a field with a undefined value.
     *
     * @param name the name of the field
     */
    void gotUndefined(String name);

    /**
     * Called when reading a field with a MINKEY value.
     *
     * @param name the name of the field
     */
    void gotMinKey(String name);

    /**
     * Called when reading a field with a MAX_KEY value.
     *
     * @param name the name of the field
     */
    void gotMaxKey(String name);

    /**
     * Called when reading a field with a BOOLEAN value.
     *
     * @param name  the name of the field
     * @param value the field's value
     */
    void gotBoolean(String name, boolean value);

    /**
     * Called when reading a field with a DOUBLE value.
     *
     * @param name  the name of the field
     * @param value the field's value
     */
    void gotDouble(String name, double value);

    /**
     * Called when reading a field with a INT32 value.
     *
     * @param name  the name of the field
     * @param value the field's value
     */
    void gotInt(String name, int value);

    /**
     * Called when reading a field with a INT64 value.
     *
     * @param name  the name of the field
     * @param value the field's value
     */
    void gotLong(String name, long value);

    /**
     * Called when reading a field with a DATE_TIME value.
     *
     * @param name   the name of the field
     * @param millis the date and time in milliseconds
     */
    void gotDate(String name, long millis);

    /**
     * Called when reading a field with a STRING value.
     *
     * @param name  the name of the field
     * @param value the field's value
     */
    void gotString(String name, String value);

    /**
     * Called when reading a field with a SYMBOL value.
     *
     * @param name  the name of the field
     * @param value the field's value
     */
    void gotSymbol(String name, String value);

    /**
     * Called when reading a field with a REGULAR_EXPRESSION value.
     *
     * @param name    the name of the field
     * @param pattern the regex pattern
     * @param flags   the optional flags for the regular expression
     * @mongodb.driver.manual reference/operator/query/regex/ $regex
     */
    void gotRegex(String name, String pattern, String flags);

    /**
     * Called when reading a field with a TIMESTAMP value.
     *
     * @param name      the name of the field
     * @param time      the time in seconds since epoch
     * @param increment an incrementing ordinal for operations within a given second
     * @mongodb.driver.manual reference/bson-types/#timestamps Timestamps
     */
    void gotTimestamp(String name, int time, int increment);

    /**
     * Called when reading a field with a OBJECT_ID value.
     *
     * @param name the name of the field
     * @param id   the object ID
     */
    void gotObjectId(String name, Object id);

    /**
     * Invoked when BSONDecoder encountered a DB_POINTER type field in a byte sequence.
     *
     * @param name      the name of the field
     * @param namespace the namespace to which reference is pointing to
     * @param id        the if of the object to which reference is pointing to
     */
    void gotDBRef(String name, String namespace, Object id);

    /**
     * This method is not used.
     *
     * @param name the name of the field
     * @param data the field's value
     * @deprecated
     */
    @Deprecated
    void gotBinaryArray(String name, byte[] data);

    /**
     * Called when reading a field with a BINARY value. Note that binary values have a subtype, which may
     * determine how the value is processed.
     *
     * @param name the name of the field
     * @param type one of the binary subtypes:
     * @param data the field's value
     */
    void gotBinary(String name, byte type, byte[] data);

    /**
     * Called when reading a field with a {@link java.util.UUID} value.  This is a binary value of subtype
     *
     * @param name  the name of the field
     * @param part1 the first part of the UUID
     * @param part2 the second part of the UUID
     */
    void gotUUID(String name, long part1, long part2);

    /**
     * Called when reading a field with a JAVASCRIPT value.
     *
     * @param name the name of the field
     * @param code the JavaScript code
     */
    void gotCode(String name, String code);

    /**
     * Called when reading a field with a JAVASCRIPT_WITH_SCOPE value.
     *
     * @param name  the name of the field
     * @param code  the JavaScript code
     * @param scope a document representing the scope for the code
     */
    void gotCodeWScope(String name, String code, Object scope);

}
