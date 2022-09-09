package com.dbschema.mongo;

import org.junit.Test;

import static com.dbschema.mongo.Util.insertAuthMechanism;
import static org.junit.Assert.assertEquals;

public class InsertAuthMechanismToUriTest {
    private static final String NO_MECHANISM = "mongodb://localhost:27017/admin";

    @Test
    public void noInsert() {
        assertEquals(NO_MECHANISM, insertAuthMechanism(NO_MECHANISM, "does not exist"));
        assertEquals("jdbc:" + NO_MECHANISM, insertAuthMechanism("jdbc:" + NO_MECHANISM, "does not exist"));
        assertEquals("mongodb://localhost:27017/?authMechanism=SCRAM-SHA-1", insertAuthMechanism("mongodb://localhost:27017/?authMechanism=SCRAM-SHA-1", "SCRAM-SHA-256"));
        assertEquals("mongodb://localhost:27017/?hello=world&authMechanism=SCRAM-SHA-1", insertAuthMechanism("mongodb://localhost:27017/?hello=world&authMechanism=SCRAM-SHA-1", "SCRAM-SHA-256"));
    }

    @Test
    public void insert() {
        assertEquals("mongodb://localhost:27017/admin?authMechanism=SCRAM-SHA-256", insertAuthMechanism(NO_MECHANISM, "SCRAM-SHA-256"));
        assertEquals("jdbc:mongodb://localhost:27017/admin?authMechanism=SCRAM-SHA-256", insertAuthMechanism("jdbc:" + NO_MECHANISM, "SCRAM-SHA-256"));
        assertEquals("mongodb://localhost:27017/admin?authMechanism=SCRAM-SHA-256", insertAuthMechanism("mongodb://localhost:27017/admin?", "SCRAM-SHA-256"));
        assertEquals("mongodb://localhost:27017/?authMechanism=SCRAM-SHA-256", insertAuthMechanism("mongodb://localhost:27017/", "SCRAM-SHA-256"));
        assertEquals("mongodb://localhost:27017/?authMechanism=SCRAM-SHA-256", insertAuthMechanism("mongodb://localhost:27017", "SCRAM-SHA-256"));
        assertEquals("mongodb://localhost:27017/?hello=world&authMechanism=SCRAM-SHA-256", insertAuthMechanism("mongodb://localhost:27017/?hello=world", "SCRAM-SHA-256"));
        assertEquals("mongodb://localhost:27017/?hello=world&authMechanism=SCRAM-SHA-256", insertAuthMechanism("mongodb://localhost:27017/?hello=world&", "SCRAM-SHA-256"));
    }
}
