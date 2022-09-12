package com.dbschema.mongo;

import org.junit.Test;

import static com.dbschema.mongo.Util.insertAuthMechanism;
import static com.dbschema.mongo.Util.insertAuthSource;
import static org.junit.Assert.assertEquals;

public class InsertAuthSourceToUriTest {
    private static final String NO_SOURCE = "mongodb://localhost:27017/admin";

    @Test
    public void noInsert() {
        assertEquals(NO_SOURCE, insertAuthSource(NO_SOURCE, null));
        assertEquals(NO_SOURCE, insertAuthSource(NO_SOURCE, ""));
        assertEquals("jdbc:" + NO_SOURCE, insertAuthSource("jdbc:" + NO_SOURCE, ""));
        assertEquals("mongodb://localhost:27017/?authSource=s", insertAuthSource("mongodb://localhost:27017/?authSource=s", "new"));
        assertEquals("mongodb://localhost:27017/?hello=world&authSource=s", insertAuthSource("mongodb://localhost:27017/?hello=world&authSource=s", "new"));
    }

    @Test
    public void insert() {
        assertEquals("mongodb://localhost:27017/admin?authSource=s", insertAuthSource(NO_SOURCE, "s"));
        assertEquals("mongodb://localhost:27017/admin?authSource=s", insertAuthSource("mongodb://localhost:27017/admin?", "s"));
        assertEquals("mongodb://localhost:27017/?authSource=s", insertAuthSource("mongodb://localhost:27017/", "s"));
        assertEquals("mongodb://localhost:27017/?authSource=s", insertAuthSource("mongodb://localhost:27017", "s"));
        assertEquals("mongodb://localhost:27017/?hello=world&authMechanism=SCRAM-SHA-256&authSource=s", insertAuthSource("mongodb://localhost:27017/?hello=world&authMechanism=SCRAM-SHA-256", "s"));
    }
}
