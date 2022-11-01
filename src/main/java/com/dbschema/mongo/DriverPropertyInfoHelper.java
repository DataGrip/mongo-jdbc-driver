package com.dbschema.mongo;

import java.sql.DriverPropertyInfo;
import java.util.ArrayList;

public class DriverPropertyInfoHelper {
  public static final String AUTH_MECHANISM = "authMechanism";
  public static final String[] AUTH_MECHANISM_CHOICES = new String[]{"GSSAPI", "MONGODB-AWS", "MONGODB-X509", "PLAIN", "SCRAM-SHA-1", "SCRAM-SHA-256"};
  public static final String AUTH_SOURCE = "authSource";
  public static final String AWS_SESSION_TOKEN = "AWS_SESSION_TOKEN";
  public static final String SERVICE_NAME = "SERVICE_NAME";
  public static final String SERVICE_REALM = "SERVICE_REALM";
  public static final String CANONICALIZE_HOST_NAME = "CANONICALIZE_HOST_NAME";
  public static final String[] CANONICALIZE_HOST_NAME_CHOICES = new String[]{Boolean.toString(false), Boolean.toString(true)};
  public static final String UUID_REPRESENTATION = "uuidRepresentation";
  public static final String UUID_REPRESENTATION_DEFAULT = "standard";
  public static final String[] UUID_REPRESENTATION_CHOICES = new String[]{"standard", "javaLegacy", "csharpLegacy", "pythonLegacy"};
  public static final String SERVER_SELECTION_TIMEOUT = "serverSelectionTimeoutMS";
  public static final String SERVER_SELECTION_TIMEOUT_DEFAULT = "10000";
  public static final String CONNECT_TIMEOUT = "connectTimeoutMS";
  public static final String CONNECT_TIMEOUT_DEFAULT = "10000";
  public static final String FETCH_DOCUMENTS_FOR_METAINFO = "fetch_documents_for_metainfo";
  public static final int FETCH_DOCUMENTS_FOR_METAINFO_DEFAULT = 10;
  public static final String MAX_POOL_SIZE = "max_connection_pool_size";
  public static final int MAX_POOL_SIZE_DEFAULT = 3;
  private static final String GET_MAX_SIZE_DOCS = "https://mongodb.github.io/mongo-java-driver/3.6/javadoc/com/mongodb/connection/ConnectionPoolSettings.html#getMaxSize--";
  public static final String ENCODE_CREDENTIALS = "auto_encode_username_and_password";
  public static final boolean ENCODE_CREDENTIALS_DEFAULT = true;
  public static final String APPLICATION_NAME = "appName";
  public static final String ALLOW_INVALID_CERTIFICATES = "tlsAllowInvalidCertificates";
  public static final boolean ALLOW_INVALID_CERTIFICATES_DEFAULT = false;
  public static final String ALLOW_INVALID_HOSTNAMES = "tlsAllowInvalidHostnames";
  public static final boolean ALLOW_INVALID_HOSTNAMES_DEFAULT = false;
  public static final String RETRY_WRITES = "retryWrites";
  public static final String[] RETRY_WRITES_CHOICES = new String[]{Boolean.toString(false), Boolean.toString(true)};
  private static final String RETRY_WRITES_DOCS = "https://www.mongodb.com/docs/manual/core/retryable-writes/";


  public DriverPropertyInfo[] getPropertyInfo() {
    ArrayList<DriverPropertyInfo> propInfos = new ArrayList<>();

    addPropInfo(propInfos, AUTH_MECHANISM, "", "MongoDB authentication mechanism", AUTH_MECHANISM_CHOICES);
    addPropInfo(propInfos, AUTH_SOURCE, "", "Specify the database name associated with the user's credentials.\n" +
            "If authSource is unspecified, authSource defaults to the defaultauthdb specified in the connection string.\n" +
            "If defaultauthdb is unspecified, then authSource defaults to admin.\n" +
            "MongoDB will ignore authSource values if no username is provided.", null);
    addPropInfo(propInfos, AWS_SESSION_TOKEN, "", "AWS session token", null);
    addPropInfo(propInfos, SERVICE_NAME, "", "Set the Kerberos service name when connecting to Kerberized MongoDB instances. This value must match the service name set on MongoDB instances to which you are connecting. Only valid when using the GSSAPI authentication mechanism.\n" +
            "SERVICE_NAME defaults to mongodb for all clients and MongoDB instances. If you change the saslServiceName setting on a MongoDB instance, you must set SERVICE_NAME to match that setting. Only valid when using the GSSAPI authentication mechanism.", null);
    addPropInfo(propInfos, CANONICALIZE_HOST_NAME, "", "Canonicalize the hostname of the client host machine when connecting to the Kerberos server. This may be required when hosts report different hostnames than what is in the Kerberos database. Defaults to false. Only valid when using the GSSAPI authentication mechanism.", CANONICALIZE_HOST_NAME_CHOICES);
    addPropInfo(propInfos, SERVICE_REALM, "", "Set the Kerberos realm for the MongoDB service. This may be necessary to support cross-realm authentication where the user exists in one realm and the service in another. Only valid when using the GSSAPI authentication mechanism.", null);

    addPropInfo(propInfos, ENCODE_CREDENTIALS, Boolean.toString(ENCODE_CREDENTIALS_DEFAULT), "Connection url requires username and password to be url encoded." +
        " This setting turns on automatic url-encoding", null);

    addPropInfo(propInfos, UUID_REPRESENTATION, UUID_REPRESENTATION_DEFAULT, "UUID representation defines how UUIDs are decoded and encoded.\n" +
            "'standard' - newly created UUIDs are encoded using binary subtype 4. All UUIDs of subtype 3 are shown as raw binary values without decoding to UUID.\n" +
            "'javaLegacy', 'csharpLegacy', 'pythonLegacy' - newly created UUIDs are encoded using corresponding legacy format (subtype 3). UUIDs of subtype 3 are decoded using corresponding legacy format despite of their actual format. UUIDs of subtype 4 are decoded using 'standard' format.",
        UUID_REPRESENTATION_CHOICES);

    addPropInfo(propInfos, SERVER_SELECTION_TIMEOUT, SERVER_SELECTION_TIMEOUT_DEFAULT, "How long the driver will wait for server selection to succeed before throwing an exception.", null);
    addPropInfo(propInfos, CONNECT_TIMEOUT, CONNECT_TIMEOUT_DEFAULT, "How long a connection can take to be opened before timing out.", null);
    addPropInfo(propInfos, ALLOW_INVALID_CERTIFICATES, Boolean.toString(ALLOW_INVALID_CERTIFICATES_DEFAULT),
        "Disables the validation of server certificate", new String[]{"true", "false"});
    addPropInfo(propInfos, ALLOW_INVALID_HOSTNAMES, Boolean.toString(ALLOW_INVALID_HOSTNAMES_DEFAULT),
        "Disables the validation of hostnames in server certificate", new String[]{"true", "false"});

    addPropInfo(propInfos, FETCH_DOCUMENTS_FOR_METAINFO, Integer.toString(FETCH_DOCUMENTS_FOR_METAINFO_DEFAULT), "Number of documents that will be fetched per collection in order " +
        "to return meta information from DatabaseMetaData.getColumns method.", null);

    addPropInfo(propInfos, MAX_POOL_SIZE, Integer.toString(MAX_POOL_SIZE_DEFAULT), "MongoDB connections pool size per one connection from IDE. See " + GET_MAX_SIZE_DOCS, null);

    addPropInfo(propInfos, RETRY_WRITES, null, "See " + RETRY_WRITES_DOCS, RETRY_WRITES_CHOICES);

    addPropInfo(propInfos, APPLICATION_NAME, null, "Sets the logical name of the application.", null);

    return propInfos.toArray(new DriverPropertyInfo[0]);
  }

  private void addPropInfo(final ArrayList<DriverPropertyInfo> propInfos, final String propName,
                           final String defaultVal, final String description, final String[] choices) {
    DriverPropertyInfo newProp = new DriverPropertyInfo(propName, defaultVal);
    newProp.description = description;
    if (choices != null) {
      newProp.choices = choices;
    }
    propInfos.add(newProp);
  }
}
