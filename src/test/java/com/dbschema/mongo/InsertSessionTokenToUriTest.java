package com.dbschema.mongo;

import org.junit.Test;

import static com.dbschema.mongo.DriverPropertyInfoHelper.*;
import static com.dbschema.mongo.Util.insertAuthProperty;
import static org.junit.Assert.assertEquals;

public class InsertSessionTokenToUriTest {
    private static final String NO_TOKEN = "mongodb://localhost:27017/admin";
    private static final String WITH_TOKEN = "mongodb://localhost:27017/admin?authMechanismProperties=AWS_SESSION_TOKEN:abc";

    @Test
    public void noInsert() {
        assertEquals(NO_TOKEN, insertAuthProperty(NO_TOKEN, AWS_SESSION_TOKEN, ""));
        assertEquals(WITH_TOKEN, insertAuthProperty(WITH_TOKEN, AWS_SESSION_TOKEN, ""));
    }

    @Test
    public void insert() {
        assertEquals(WITH_TOKEN, insertAuthProperty(NO_TOKEN, AWS_SESSION_TOKEN, "abc"));
        assertEquals("mongodb+srv://A:B@cluster0-2ejqn.mongodb.net/d?authMechanism=MONGODB-AWS&authMechanismProperties=AWS_SESSION_TOKEN:abc",
                insertAuthProperty("mongodb+srv://A:B@cluster0-2ejqn.mongodb.net/d?authMechanism=MONGODB-AWS", AWS_SESSION_TOKEN, "abc"));
        assertEquals("mongodb+srv://A:B@cluster0-2ejqn.mongodb.net/d?authMechanism=MONGODB-AWS&authMechanismProperties=hello:world,AWS_SESSION_TOKEN:abc",
                insertAuthProperty("mongodb+srv://A:B@cluster0-2ejqn.mongodb.net/d?authMechanism=MONGODB-AWS&authMechanismProperties=hello:world", AWS_SESSION_TOKEN, "abc"));
        assertEquals("mongodb://localhost:27017/admin?authMechanismProperties=SERVICE_NAME:name,SERVICE_REALM:realm,CANONICALIZE_HOST_NAME:true", insertAuthProperty(insertAuthProperty(insertAuthProperty(NO_TOKEN, SERVICE_NAME, "name"), SERVICE_REALM, "realm"), CANONICALIZE_HOST_NAME, "true"));
    }
}
