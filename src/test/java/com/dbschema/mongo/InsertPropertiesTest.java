package com.dbschema.mongo;

import org.junit.Test;

import static com.dbschema.mongo.Util.insertRetryWrites;
import static org.junit.Assert.assertEquals;

public class InsertPropertiesTest {
    private static final String URL = "mongodb://localhost:27017/admin";

    @Test
    public void testInsertRetryWrites() {
        assertEquals("mongodb://localhost:27017/admin?retryWrites=true", insertRetryWrites(URL, "true"));
        assertEquals(URL, insertRetryWrites(URL, ""));
        assertEquals(URL, insertRetryWrites(URL, "invalid"));
        assertEquals("mongodb://localhost:27017/admin?retryWrites=true", insertRetryWrites(URL + "?retryWrites=true", "false"));
        assertEquals("mongodb://localhost:27017/?hello=world&authMechanism=SCRAM-SHA-256&retryWrites=false", insertRetryWrites("mongodb://localhost:27017/?hello=world&authMechanism=SCRAM-SHA-256", "false"));
    }
}
