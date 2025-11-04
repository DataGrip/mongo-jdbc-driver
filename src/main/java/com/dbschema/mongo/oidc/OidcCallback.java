package com.dbschema.mongo.oidc;

import com.mongodb.MongoCredential;
import com.mongodb.MongoCredential.OidcCallbackContext;
import com.mongodb.MongoCredential.OidcCallbackResult;
import javax.security.auth.RefreshFailedException;

public class OidcCallback implements MongoCredential.OidcCallback {
    private final OidcAuthFlow oidcAuthFlow;
    private OidcCallbackContext callbackContext;

    public OidcCallback(OidcCallbackContext callbackContext) {
        this.oidcAuthFlow = new OidcAuthFlow();
        this.callbackContext = callbackContext;
    }

    public OidcAuthFlow getOidcAuthFlow() {
        return this.oidcAuthFlow;
    }

    public OidcCallbackResult onRequest(OidcCallbackContext callbackContext) {

        if (this.callbackContext != null && this.callbackContext.getRefreshToken() != null && !this.callbackContext.getRefreshToken().isEmpty()) {
            try {
                return oidcAuthFlow.doRefresh(callbackContext);
            } catch (RefreshFailedException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.callbackContext = callbackContext;
            try {
                return oidcAuthFlow.doAuthCodeFlow(callbackContext);
            } catch (OidcTimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public OidcCallbackContext getCallbackContext() {
        return this.callbackContext;
    }
}
