package com.dbschema.mongo;

import com.dbschema.mongo.oidc.OidcCallback;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dbschema.mongo.DriverPropertyInfoHelper.*;
import static com.dbschema.mongo.SSLUtil.getTrustEverybodySSLContext;
import static com.dbschema.mongo.Util.*;


public class MongoClientWrapper implements AutoCloseable {
  private static final Logger logger = Logger.getLogger(MongoClientWrapper.class.getName());
  private static final String WILDCARD_PREFIX = "*.";
  /** What an entry that is meant to be a hostname cannot contain; whitespace is refused on top of these. */
  private static final String INVALID_HOSTNAME_CHARS = "/@?#[]";

  private boolean isClosed = false;
  private final MongoClient mongoClient;
  public final String databaseNameFromUrl;

  public MongoClientWrapper(@NotNull String uri, @NotNull Properties prop, @Nullable String username, @Nullable String password) throws SQLException {
    try {
      boolean automaticEncoding = ENCODE_CREDENTIALS_DEFAULT;
      if (prop.getProperty(ENCODE_CREDENTIALS) != null) {
        automaticEncoding = Boolean.parseBoolean(prop.getProperty(ENCODE_CREDENTIALS));
      }

      String authMechanism = prop.getProperty(AUTH_MECHANISM);
      boolean oidc = "MONGODB-OIDC".equals(authMechanism);
      if (!oidc) {
        uri = insertCredentials(uri, username, password, automaticEncoding);
      }
      uri = insertAuthMechanism(uri, authMechanism);
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

      ConnectionString connectionString = new ConnectionString(uri);
      databaseNameFromUrl = connectionString.getDatabase();
      int maxPoolSize = getMaxPoolSize(prop);
      MongoClientSettings.Builder builder = MongoClientSettings.builder()
          .applyConnectionString(connectionString)
          .applyToConnectionPoolSettings(b -> b.maxSize(maxPoolSize));

      if (oidc) {
        OidcCallback oidcCallback =
                new OidcCallback(getRedirectHost(prop), getRedirectPort(prop), getTrustSystemKeychain(prop));
        // this credential replaces the one applyConnectionString derived, so the username the URL carries
        // has to be carried over by hand -- for OIDC the URL is never rewritten (see insertCredentials above)
        builder.credential(buildOidcCredential(username, connectionString.getUsername(), oidcCallback, prop));
      }

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
    // a configuration error already says what is wrong, wrapping it again only buries the message
    catch (SQLException e) {
      throw e;
    }
    catch (Exception e) {
      throw new SQLException(e);
    }
  }

  /**
   * The credential for MONGODB-OIDC. The principal name is what the server matches its identity provider
   * against (its 'matchPattern'), and it travels in the very first handshake message, before any browser
   * login; without it a server that selects a provider by pattern cannot match one at all. It defaults to
   * the username of the connection, which the OIDC_PRINCIPAL property overrides or switches off entirely.
   *
   * @param username    the username of the connection, from the 'user' property
   * @param urlUsername the username embedded in the connection URL, which wins over the property: that is
   *                    what a URL username does for every other mechanism, where insertCredentials leaves
   *                    the URI alone as soon as it carries one
   */
  @NotNull
  static MongoCredential buildOidcCredential(@Nullable String username, @Nullable String urlUsername,
                                             @NotNull MongoCredential.OidcCallback callback,
                                             @NotNull Properties prop) throws SQLException {
    Principal principal = resolveOidcPrincipal(username, urlUsername, prop);
    MongoCredential credential = MongoCredential.createOidcCredential(principal.value())
        .withMechanismProperty(MongoCredential.OIDC_HUMAN_CALLBACK_KEY, callback);
    List<String> allowedHosts = getAllowedHosts(prop);
    // the driver version tells a stale jar apart from an unset property: both look like 'AuthenticationFailed'.
    // Only whether the principal is set is reported here: its value is a personal identifier (an email or a
    // UPN, usually) and this record is written on every connection and ends up in shared bug reports.
    logger.log(Level.INFO, "MongoDB JDBC driver {0} authenticating with MONGODB-OIDC: principal={1}, "
            + "taken from {2}, allowed hosts={3}", new Object[]{
        MongoDatabaseMetaData.DRIVER_VERSION, principal.value() == null ? "<unset>" : "<set>",
        principal.source(), allowedHosts});
    logger.log(Level.FINE, "MONGODB-OIDC principal: {0}", principal.value());
    if (!allowedHosts.equals(MongoCredential.DEFAULT_ALLOWED_HOSTS)) {
      credential = credential.withMechanismProperty(MongoCredential.ALLOWED_HOSTS_KEY, allowedHosts);
    }
    return credential;
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

  private String getRedirectPort(@NotNull Properties prop) {
    try{
      String redirectPort = prop.getProperty(OIDC_CALLBACK_PORT);
      if (redirectPort != null) {
        return redirectPort;
      }
      return Integer.toString(OIDC_CALLBACK_PORT_DEFAULT);
    } catch (Exception e) {
      e.printStackTrace();
      return Integer.toString(OIDC_CALLBACK_PORT_DEFAULT);
    }
  }

  private String getRedirectHost(@NotNull Properties prop) {
    try{
      String redirectPort = prop.getProperty(OIDC_CALLBACK_HOST);
      if (redirectPort != null) {
        return redirectPort;
      }
      return OIDC_CALLBACK_HOST_DEFAULT;
    } catch (Exception e) {
      e.printStackTrace();
      return OIDC_CALLBACK_HOST_DEFAULT;
    }
  }

  private boolean getTrustSystemKeychain(@NotNull Properties prop) {
    String value = prop.getProperty(OIDC_TRUST_SYSTEM_KEYCHAIN);
    return value == null ? OIDC_TRUST_SYSTEM_KEYCHAIN_DEFAULT : Boolean.parseBoolean(value);
  }

  /**
   * Hosts allowed for the MONGODB-OIDC browser login flow: the driver defaults plus whatever the user
   * trusts explicitly. ALLOWED_HOSTS cannot be passed in the connection string, so it must come from the
   * jdbc properties.
   */
  @NotNull
  static List<String> getAllowedHosts(@NotNull Properties prop) throws SQLException {
    List<String> hosts = new ArrayList<>(MongoCredential.DEFAULT_ALLOWED_HOSTS);
    String configured = prop.getProperty(OIDC_ALLOWED_HOSTS);
    if (!isNullOrEmpty(configured)) {
      for (String host : configured.split(",")) {
        // normalized before the duplicate check, so that 'localhost:27017' collapses onto the default
        // 'localhost' instead of being added as a second entry that can never match
        addHost(hosts, normalizeAllowedHost(host));
      }
    }
    return hosts;
  }

  /**
   * Brings one OIDC_ALLOWED_HOSTS entry into the only shape the driver can use, and rejects the shapes it
   * cannot. The driver compares an entry without a wildcard to the hostname of the server -- case
   * sensitively, and without the port -- so a port and the letter case are dropped here. A wildcard is
   * only understood as a leading '*.', matching subdomains; anywhere else in the entry the driver throws
   * 'contains invalid wildcard' in the middle of authenticating, and only if no earlier entry matched
   * first, so such an entry is refused here instead, while the connection is still being set up. Every
   * other entry that is not a hostname is refused there too: the driver has nothing to compare it to and
   * would just never match it.
   */
  @Nullable
  static String normalizeAllowedHost(@Nullable String entry) throws SQLException {
    if (entry == null) return null;
    String trimmed = entry.trim();
    // an empty entry is nothing to add rather than something to complain about: 'a,,b', or a trailing comma
    if (trimmed.isEmpty()) return null;
    String host = stripPort(trimmed).toLowerCase(Locale.ROOT);
    String hostname = host;
    if (host.indexOf('*') >= 0) {
      String subdomainsOf = host.startsWith(WILDCARD_PREFIX) ? host.substring(WILDCARD_PREFIX.length()) : null;
      if (subdomainsOf == null || subdomainsOf.isEmpty() || subdomainsOf.indexOf('*') >= 0) {
        throw new SQLException(String.format(
            "Invalid %s entry '%s': a wildcard is only allowed as a leading '%s', which matches the "
                + "subdomains of the rest of the entry.%s",
            OIDC_ALLOWED_HOSTS, trimmed, WILDCARD_PREFIX, suggestion(host)));
      }
      hostname = subdomainsOf;
    }
    String problem = hostnameProblem(hostname);
    if (problem != null) {
      throw new SQLException(String.format(
          "Invalid %s entry '%s': %s. An entry is a hostname or an ip-address -- no scheme, no path and "
              + "no credentials, and a port is dropped -- optionally prefixed with '%s' to match its "
              + "subdomains.%s",
          OIDC_ALLOWED_HOSTS, trimmed, problem, WILDCARD_PREFIX, hostnameSuggestion(trimmed)));
    }
    if (!host.equals(trimmed)) {
      logger.log(Level.FINE, "{0} entry ''{1}'' is used as ''{2}''", new Object[]{OIDC_ALLOWED_HOSTS, trimmed, host});
    }
    return host;
  }

  /**
   * Why this entry could never be matched against the hostname of a server, or {@code null} when it can.
   * A leading '*.' is off already, and so is a port, so what is left has to be a hostname or an
   * ip-address literal.
   */
  @Nullable
  private static String hostnameProblem(@NotNull String hostname) {
    if (hostname.isEmpty()) return "there is no hostname in it";
    for (int i = 0; i < hostname.length(); i++) {
      char c = hostname.charAt(i);
      if (INVALID_HOSTNAME_CHARS.indexOf(c) >= 0 || Character.isWhitespace(c)) {
        return String.format("'%s' is not part of a hostname", c);
      }
    }
    // '.' terminates the root of a domain name, which the driver does not strip before comparing
    if (hostname.endsWith(".")) return "it ends with a dot";
    if (hostname.indexOf(':') >= 0 && !isIpv6Literal(hostname)) {
      return "what follows the ':' is neither a port nor part of an ip-v6 address";
    }
    return null;
  }

  private static boolean isIpv6Literal(@NotNull String hostname) {
    for (int i = 0; i < hostname.length(); i++) {
      char c = hostname.charAt(i);
      // the dot is there for the ip-v4-mapped form, '::ffff:127.0.0.1'
      if (c != ':' && c != '.' && Character.digit(c, 16) < 0) return false;
    }
    return true;
  }

  @NotNull
  private static String suggestion(@NotNull String host) {
    // '*mycorp.net' is the near miss worth naming; 'db.*.net' has no single obvious correction
    String rest = host.startsWith("*") ? host.substring(1) : "";
    return rest.isEmpty() || rest.startsWith(".") || rest.indexOf('*') >= 0
        ? "" : String.format(" Did you mean '%s%s'?", WILDCARD_PREFIX, rest);
  }

  /** The hostname of an entry that carries more than one, a pasted connection URL being the usual case. */
  @NotNull
  private static String hostnameSuggestion(@NotNull String entry) {
    String rest = entry;
    int scheme = rest.indexOf("://");
    if (scheme >= 0) rest = rest.substring(scheme + "://".length());
    int path = rest.indexOf('/');
    if (path >= 0) rest = rest.substring(0, path);
    int credentials = rest.lastIndexOf('@');
    if (credentials >= 0) rest = rest.substring(credentials + 1);
    rest = stripPort(rest.trim()).toLowerCase(Locale.ROOT);
    return rest.isEmpty() || rest.equals(entry) || hostnameProblem(rest) != null
        ? "" : String.format(" Did you mean '%s'?", rest);
  }

  /**
   * Drops the port of a 'host:port' entry, leaving a bare IPv6 literal such as the default '::1' alone:
   * only a single colon followed by digits is a port, while the bracketed form is unwrapped.
   */
  @NotNull
  private static String stripPort(@NotNull String entry) {
    if (entry.startsWith("[")) {
      int closing = entry.indexOf(']');
      return closing < 0 ? entry : entry.substring(1, closing);
    }
    int colon = entry.indexOf(':');
    if (colon < 0 || colon != entry.lastIndexOf(':')) return entry;
    String port = entry.substring(colon + 1);
    if (port.isEmpty()) return entry;
    for (int i = 0; i < port.length(); i++) {
      if (!Character.isDigit(port.charAt(i))) return entry;
    }
    return entry.substring(0, colon);
  }

  /**
   * A principal name to send, and where it was taken from. The two are decided together so that the log
   * record cannot name a source that supplied nothing.
   */
  private record Principal(@Nullable String value, @NotNull String source) {
  }

  /**
   * The MONGODB-OIDC principal name: the OIDC_PRINCIPAL property when it is set, the username written into
   * the connection URL next, the username of the connection last. The property is the way out for a
   * connection whose username is not the OIDC identity -- a username kept from SCRAM authentication, say:
   * sending it makes a server that selects its identity provider by 'matchPattern' match none of them and
   * fail with a bare 'AuthenticationFailed'. The value
   * '{@value DriverPropertyInfoHelper#OIDC_PRINCIPAL_NONE}', and an empty value, mean no principal is
   * sent; a user actually named that way therefore cannot be passed here.
   */
  @NotNull
  private static Principal resolveOidcPrincipal(@Nullable String username, @Nullable String urlUsername,
                                                @NotNull Properties prop) {
    String configured = prop.getProperty(OIDC_PRINCIPAL);
    if (configured != null) {
      String principal = configured.trim();
      boolean none = principal.isEmpty() || principal.equalsIgnoreCase(OIDC_PRINCIPAL_NONE);
      return new Principal(none ? null : principal, OIDC_PRINCIPAL);
    }
    String fromUrl = trimToNull(urlUsername);
    if (fromUrl != null) return new Principal(fromUrl, "the username in the connection URL");
    String fromProperty = trimToNull(username);
    if (fromProperty != null) return new Principal(fromProperty, "the username of the connection");
    return new Principal(null, "nowhere: neither " + OIDC_PRINCIPAL + " nor a username is set");
  }

  @Nullable
  private static String trimToNull(@Nullable String value) {
    if (isNullOrEmpty(value)) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static void addHost(@NotNull List<String> hosts, @Nullable String host) {
    if (host == null) return;
    String trimmed = host.trim();
    if (!trimmed.isEmpty() && !hosts.contains(trimmed)) hosts.add(trimmed);
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

  @NotNull
  public MongoClient getMongoClient() {
    return mongoClient;
  }
}
