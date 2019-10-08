package com.dbschema.mongo.java;

import com.dbschema.mongo.ConnectionParameters;
import com.mongodb.AuthenticationMechanism;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;
import java.util.regex.Matcher;


public class JMongoClient {
  private final MongoClient mongoClient;
  public final String databaseNameFromUrl;

  public JMongoClient(@NotNull String uri, @NotNull Properties prop, @NotNull ConnectionParameters parameters) {
    ConnectionString connectionString = new ConnectionString(uri);
    databaseNameFromUrl = parameters.database;
    MongoClientSettings.Builder builder = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .applyToConnectionPoolSettings(b -> b.maxSize(1));
    if (parameters.username != null || parameters.password != null) {
      builder.credential(createCredential(parameters.mechanism, parameters.username, parameters.authSource, parameters.password));
    }
    if ("true".equals(prop.getProperty("ssl"))) {
      builder.applyToSslSettings(s -> s.enabled(true));
    }
    this.mongoClient = MongoClients.create(builder.build());
  }

  @NotNull
  public static String removeParameter(@NotNull String uri, @NotNull Matcher matcher) {
    String group = matcher.group();
    uri = uri.replace(group, matcher.group(1));
    if (uri.endsWith("?") || uri.endsWith("&")) uri = uri.substring(0, uri.length() - 1);
    return uri;
  }

  @SuppressWarnings("deprecation")
  private MongoCredential createCredential(@Nullable AuthenticationMechanism mechanism, String user, String source, char[] password) {
    if (mechanism == null) return MongoCredential.createCredential(user, source, password);
    switch (mechanism) {
      case GSSAPI:
        return MongoCredential.createGSSAPICredential(user);
      case MONGODB_X509:
        return MongoCredential.createMongoX509Credential(user);
      case SCRAM_SHA_1:
        return MongoCredential.createScramSha1Credential(user, source, password);
      case SCRAM_SHA_256:
        return MongoCredential.createScramSha256Credential(user, source, password);
      case MONGODB_CR:
        return MongoCredential.createMongoCRCredential(user, source, password);
      case PLAIN:
        return MongoCredential.createPlainCredential(user, source, password);
      default:
        return MongoCredential.createCredential(user, source, password);
    }
  }

  public MongoIterable<String> listDatabaseNames() {
    return mongoClient.listDatabaseNames();
  }

  public JMongoDatabase getDatabase(String databaseName) {
    return new JMongoDatabase(mongoClient.getDatabase(databaseName));
  }

  public void testConnectivity() {
    mongoClient.getClusterDescription();
  }
}
