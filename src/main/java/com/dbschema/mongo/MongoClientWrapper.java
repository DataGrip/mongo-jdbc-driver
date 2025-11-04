package com.dbschema.mongo;

import com.dbschema.mongo.oidc.OidcCallback;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.UuidRepresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.dbschema.mongo.DriverPropertyInfoHelper.*;
import static com.dbschema.mongo.SSLUtil.getTrustEverybodySSLContext;
import static com.dbschema.mongo.Util.*;


public class MongoClientWrapper implements AutoCloseable {
  private boolean isClosed = false;
  private final MongoClient mongoClient;
  public final String databaseNameFromUrl;
  public final OidcCallback oidcCallback;

  public MongoClientWrapper(@NotNull String uri, @NotNull Properties prop, @Nullable String username, @Nullable String password, @Nullable MongoCredential.OidcCallbackContext callbackContext) throws SQLException {
      this.oidcCallback = new OidcCallback(callbackContext);
    try {
        boolean automaticEncoding = ENCODE_CREDENTIALS_DEFAULT;
        if (prop.getProperty(ENCODE_CREDENTIALS) != null) {
            automaticEncoding = Boolean.parseBoolean(prop.getProperty(ENCODE_CREDENTIALS));
        }

        uri = insertCredentials(uri, username, password, automaticEncoding);
        uri = insertAuthMechanism(uri, prop.getProperty(AUTH_MECHANISM));
        uri = insertAuthSource(uri, prop.getProperty(AUTH_SOURCE));
        uri = insertAuthProperty(uri, AWS_SESSION_TOKEN, prop.getProperty(AWS_SESSION_TOKEN));
        uri = insertAuthProperty(uri, SERVICE_NAME, prop.getProperty(SERVICE_NAME));
        uri = insertAuthProperty(uri, SERVICE_REALM, prop.getProperty(SERVICE_REALM));
        String canonicalizeHostName = prop.getProperty(CANONICALIZE_HOST_NAME);
        if (Boolean.TRUE.toString().equalsIgnoreCase(canonicalizeHostName) || Boolean.FALSE.toString().equalsIgnoreCase(canonicalizeHostName)) {
            uri = insertAuthProperty(uri, CANONICALIZE_HOST_NAME, canonicalizeHostName);
        }
        else if (canonicalizeHostName != null) {
            System.err.println("Unknown " + CANONICALIZE_HOST_NAME + " value. Must be true or false.");
        }
        uri = insertRetryWrites(uri, prop.getProperty(RETRY_WRITES));


        // Construct a ServerApi instance using the ServerApi.builder() method
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

      ConnectionString connectionString = new ConnectionString(uri);

        MongoCredential credential;

        credential =
                MongoCredential.createOidcCredential(
                                connectionString.getUsername())
                        .withMechanismProperty(
                                MongoCredential.OIDC_HUMAN_CALLBACK_KEY, oidcCallback);


        databaseNameFromUrl = connectionString.getDatabase();
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .serverApi(serverApi)
            .credential(credential)
            .uuidRepresentation(createUuidRepresentation(prop.getProperty(UUID_REPRESENTATION, UUID_REPRESENTATION_DEFAULT)))
            .applyToConnectionPoolSettings(b -> b.maxSize(getMaxPoolSize(prop)))

            .applyToSocketSettings(b -> b.connectTimeout(Integer.parseInt(prop.getProperty(CONNECT_TIMEOUT, CONNECT_TIMEOUT_DEFAULT)), TimeUnit.MILLISECONDS));

        String application = prop.getProperty(APPLICATION_NAME);
        if (!isNullOrEmpty(application)) {
            builder.applicationName(application);
        }
        if ("true".equals(prop.getProperty("ssl"))) {
            boolean allowInvalidCertificates = uri.contains("tlsAllowInvalidCertificates=true") || uri.contains("sslAllowInvalidCertificates=true")
                    || isTrue(prop.getProperty(ALLOW_INVALID_CERTIFICATES, Boolean.toString(ALLOW_INVALID_CERTIFICATES_DEFAULT)));
            builder.applyToSslSettings(s -> {
                s.enabled(true);
                boolean allowInvalidHostnames = isTrue(prop.getProperty(ALLOW_INVALID_HOSTNAMES, Boolean.toString(ALLOW_INVALID_HOSTNAMES_DEFAULT)));
                if (allowInvalidHostnames) s.invalidHostNameAllowed(true);
                if (allowInvalidCertificates) {
                    String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType());
                    String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword", "");
                    String keyStoreUrl = System.getProperty("javax.net.ssl.keyStore", "");
                    // check keyStoreUrl
                    if (!isNullOrEmpty(keyStoreUrl)) {
                        try {
                            new URL(keyStoreUrl);
                        } catch (MalformedURLException e) {
                            keyStoreUrl = "file:" + keyStoreUrl;
                        }
                    }
                    try {
                        s.context(getTrustEverybodySSLContext(keyStoreUrl, keyStoreType, keyStorePassword));
                    }
                    catch (SSLUtil.SSLParamsException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        if (connectionString.getUuidRepresentation() == null) {
            String uuidRepresentation = prop.getProperty(UUID_REPRESENTATION, UUID_REPRESENTATION_DEFAULT);
            builder.uuidRepresentation(createUuidRepresentation(uuidRepresentation));
        }
        if (connectionString.getServerSelectionTimeout() == null) {
            int timeout = Integer.parseInt(prop.getProperty(SERVER_SELECTION_TIMEOUT, SERVER_SELECTION_TIMEOUT_DEFAULT));
            builder.applyToClusterSettings(b -> b.serverSelectionTimeout(timeout, TimeUnit.MILLISECONDS));
        }
        if (connectionString.getConnectTimeout() == null) {
            int timeout = Integer.parseInt(prop.getProperty(CONNECT_TIMEOUT, CONNECT_TIMEOUT_DEFAULT));
            builder.applyToSocketSettings(b -> b.connectTimeout(timeout, TimeUnit.MILLISECONDS));
        }

      this.mongoClient = MongoClients.create(builder.build());
    }
    catch (Exception e) {
      throw new SQLException(e);
    }
  }

  private static UuidRepresentation createUuidRepresentation(String value) {
    if (value.equalsIgnoreCase("unspecified")) {
      return UuidRepresentation.UNSPECIFIED;
    }
    if (value.equalsIgnoreCase("javaLegacy")) {
      return UuidRepresentation.JAVA_LEGACY;
    }
    if (value.equalsIgnoreCase("csharpLegacy")) {
      return UuidRepresentation.C_SHARP_LEGACY;
    }
    if (value.equalsIgnoreCase("pythonLegacy")) {
      return UuidRepresentation.PYTHON_LEGACY;
    }
    if (value.equalsIgnoreCase("standard")) {
      return UuidRepresentation.STANDARD;
    }
    throw new IllegalArgumentException("Unknown uuid representation: " + value);
  }

  private int getMaxPoolSize(@NotNull Properties prop) {
    try {
      String str = prop.getProperty(MAX_POOL_SIZE);
      if (str != null) {
        int poolSize = Integer.parseInt(str);
        return poolSize > 0 ? poolSize : 1;
      }
    }
    catch (NumberFormatException e) {
      e.printStackTrace();
    }
    return MAX_POOL_SIZE_DEFAULT;
  }

  @Override
  public void close() throws SQLAlreadyClosedException {
    checkClosed();
    isClosed = true;
    mongoClient.close();
  }

  private void checkClosed() throws SQLAlreadyClosedException {
    if (isClosed) throw new SQLAlreadyClosedException(this.getClass().getSimpleName());
  }

  public MongoIterable<String> listDatabaseNames() throws SQLAlreadyClosedException {
    checkClosed();
    return mongoClient.listDatabaseNames();
  }

  public MongoDatabase getDatabase(String databaseName) throws SQLAlreadyClosedException {
    checkClosed();
    return mongoClient.getDatabase(databaseName);
  }

  public OidcCallback getOidcCallback() {
    return this.oidcCallback;
  }

  @NotNull
  public MongoClient getMongoClient() {
    return mongoClient;
  }
}
