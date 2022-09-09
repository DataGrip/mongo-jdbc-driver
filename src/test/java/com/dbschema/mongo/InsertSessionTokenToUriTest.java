package com.dbschema.mongo;

import org.junit.Test;

import static com.dbschema.mongo.Util.insertSessionToken;
import static org.junit.Assert.assertEquals;

public class InsertSessionTokenToUriTest {
    private static final String NO_TOKEN = "mongodb://localhost:27017/admin";
    private static final String WITH_TOKEN = "mongodb://localhost:27017/admin?&authMechanismProperties=AWS_SESSION_TOKEN:abc";

    @Test
    public void noInsert() {
        assertEquals(NO_TOKEN, insertSessionToken(NO_TOKEN, ""));
        assertEquals(WITH_TOKEN, insertSessionToken(WITH_TOKEN, ""));
    }

    @Test
    public void insert() {
        assertEquals(WITH_TOKEN, insertSessionToken(NO_TOKEN, "abc"));
        assertEquals("mongodb+srv://A:B@cluster0-2ejqn.mongodb.net/d?authMechanism=MONGODB-AWS&authMechanismProperties=AWS_SESSION_TOKEN:abc",
                insertSessionToken("mongodb+srv://A:B@cluster0-2ejqn.mongodb.net/d?authMechanism=MONGODB-AWS", "abc"));
        assertEquals("mongodb+srv://A:B@cluster0-2ejqn.mongodb.net/d?authMechanism=MONGODB-AWS&authMechanismProperties=hello:world,AWS_SESSION_TOKEN:abc",
                insertSessionToken("mongodb+srv://A:B@cluster0-2ejqn.mongodb.net/d?authMechanism=MONGODB-AWS&authMechanismProperties=hello:world", "abc"));
    }
}
