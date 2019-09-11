package com.dbschema.mongo;

import com.mongodb.AuthenticationMechanism;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoIterable;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbschema.mongo.JMongoUtil.nullize;


public class JMongoClient {
    private static final Pattern AUTH_MECH_PATTERN = Pattern.compile("([?&])authMechanism=([\\w_-]+)&?");

    private final MongoClient mongoClient;
    public final String databaseName;

    public JMongoClient(String uri, Properties prop)
    {
        Matcher matcher = AUTH_MECH_PATTERN.matcher(uri);
        AuthenticationMechanism authMechanism = null;
        if (matcher.find()) {
            String group = matcher.group();
            uri = uri.replace(group, matcher.group(1));
            if (uri.endsWith("?") || uri.endsWith("&")) uri = uri.substring(0, uri.length() - 1);
            authMechanism = AuthenticationMechanism.fromMechanismName(matcher.group(2));
        }
        ConnectionString connectionString = new ConnectionString(uri);
        databaseName = nullize(connectionString.getDatabase());
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyConnectionString(connectionString);
        if (prop != null && (prop.getProperty("user") != null || prop.getProperty("password") != null)) {
            String user = prop.getProperty("user");
            String password = prop.getProperty("password");
            MongoCredential credentialsFromUrl = connectionString.getCredential();
            String source = credentialsFromUrl != null ?
                    credentialsFromUrl.getSource() :
                    databaseName != null ? databaseName : "$external";
            builder.credential(createCredential(authMechanism, user, source, password == null ? null : password.toCharArray()));
        }
        this.mongoClient = MongoClients.create(builder.build());
    }

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

    public MongoIterable<String> listDatabaseNames()
    {
        return mongoClient.listDatabaseNames();
    }

    public JMongoDatabase getDatabase(String databaseName)
    {
        return new JMongoDatabase(mongoClient.getDatabase(databaseName));
    }

    public void testConnectivity()
    {
        mongoClient.getClusterDescription();
    }
}
