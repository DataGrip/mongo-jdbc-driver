package com.dbschema.mongo;

import java.sql.DriverPropertyInfo;
import java.util.ArrayList;

public class DriverPropertyInfoHelper {
  public static final String AUTH_MECHANISM = "authMechanism";
  public static final String[] AUTH_MECHANISM_CHOICES = new String[]{"GSSAPI", "MONGODB-AWS", "MONGODB-X509", "PLAIN", "SCRAM-SHA-1", "SCRAM-SHA-256"};
  public static final String AWS_SESSION_TOKEN = "AWS_SESSION_TOKEN";
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


  public DriverPropertyInfo[] getPropertyInfo() {
    ArrayList<DriverPropertyInfo> propInfos = new ArrayList<>();

    addPropInfo(propInfos, AUTH_MECHANISM, "", "MongoDB authentication mechanism", AUTH_MECHANISM_CHOICES);
    addPropInfo(propInfos, AWS_SESSION_TOKEN, "", "AWS session token", null);

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
