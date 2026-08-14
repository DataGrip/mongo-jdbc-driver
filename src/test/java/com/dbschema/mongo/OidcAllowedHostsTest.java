package com.dbschema.mongo;

import com.dbschema.mongo.oidc.OidcCallback;
import com.mongodb.MongoCredential;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import static com.dbschema.mongo.DriverPropertyInfoHelper.OIDC_ALLOWED_HOSTS;
import static com.dbschema.mongo.DriverPropertyInfoHelper.OIDC_PRINCIPAL;
import static com.dbschema.mongo.MongoClientWrapper.buildOidcCredential;
import static com.dbschema.mongo.MongoClientWrapper.getAllowedHosts;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class OidcAllowedHostsTest {
    private static final OidcCallback CALLBACK = new OidcCallback("localhost", "27097");

    @Test
    public void noPropertiesKeepsDefaults() throws Exception {
        assertEquals(MongoCredential.DEFAULT_ALLOWED_HOSTS, getAllowedHosts(props()));
    }

    @Test
    public void explicitHostsAreAppended() throws Exception {
        Properties prop = props(OIDC_ALLOWED_HOSTS, " dockerdb.westeurope.cloudapp.azure.com , *.mycorp.net ");
        assertEquals(withDefaults("dockerdb.westeurope.cloudapp.azure.com", "*.mycorp.net"), getAllowedHosts(prop));
    }

    @Test
    public void duplicatesAreNotAddedTwice() throws Exception {
        Properties prop = props(OIDC_ALLOWED_HOSTS, "localhost,*.mongodb.net,,dockerdb.westeurope.cloudapp.azure.com");
        assertEquals(withDefaults("dockerdb.westeurope.cloudapp.azure.com"), getAllowedHosts(prop));
    }

    /**
     * The driver matches an entry without a wildcard against the hostname of the server, case sensitively
     * and without the port, so an entry carrying either used to match nothing at all -- silently, the only
     * sign being 'Host x not permitted by ALLOWED_HOSTS' listing an entry that looks like it should match.
     */
    @Test
    public void portsAndLetterCaseAreDropped() throws Exception {
        Properties prop = props(OIDC_ALLOWED_HOSTS, "db.mycorp.net:27017, DB.Other.NET , [::1]:27017");
        assertEquals(withDefaults("db.mycorp.net", "db.other.net"), getAllowedHosts(prop));
    }

    @Test
    public void ipv6LiteralsAndWildcardsArePassedThrough() throws Exception {
        Properties prop = props(OIDC_ALLOWED_HOSTS, "fe80::1,*.mycorp.net");
        assertEquals(withDefaults("fe80::1", "*.mycorp.net"), getAllowedHosts(prop));
    }

    /**
     * An entry the driver can only compare against a hostname, and would therefore never match, is refused
     * as well: it fails the same silent way a port used to, and a pasted connection URL is the usual case.
     */
    @Test
    public void anEntryThatIsNotAHostnameIsRefused() {
        for (String entry : asList("http://db.mycorp.net", "mongodb://db.mycorp.net:27017/admin",
            "db.mycorp.net/admin", "user@db.mycorp.net", "db.mycorp.net.", "db.mycorp.net:abc",
            "db mycorp net", "*.mycorp.net/admin")) {
            SQLException e = assertThrows(entry, SQLException.class,
                () -> getAllowedHosts(props(OIDC_ALLOWED_HOSTS, entry)));
            assertTrue(e.getMessage(), e.getMessage().contains(entry));
            assertTrue(e.getMessage(), e.getMessage().contains(OIDC_ALLOWED_HOSTS));
        }
    }

    /** The hostname of a pasted URL is the one correction worth offering. */
    @Test
    public void theHostnameOfAPastedUrlIsSuggested() {
        SQLException e = assertThrows(SQLException.class,
            () -> getAllowedHosts(props(OIDC_ALLOWED_HOSTS, "mongodb://alice@DB.MyCorp.NET:27017/admin")));
        assertTrue(e.getMessage(), e.getMessage().contains("Did you mean 'db.mycorp.net'?"));
    }

    @Test
    public void normalizationHappensBeforeTheDuplicateCheck() throws Exception {
        assertEquals(MongoCredential.DEFAULT_ALLOWED_HOSTS,
            getAllowedHosts(props(OIDC_ALLOWED_HOSTS, "localhost:27017,LOCALHOST,[::1]")));
    }

    /**
     * A wildcard anywhere but in a leading '*.' made the driver throw 'contains invalid wildcard' in the
     * middle of authenticating -- and only when no earlier entry had matched first, so whether it threw at
     * all depended on the order of the list. It is refused while the connection is still being set up now.
     */
    @Test
    public void aWildcardOutsideTheLeadingPrefixIsRefused() {
        for (String entry : asList("*mycorp.net", "db.*.net", "*", "*.", "*.*.net")) {
            SQLException e = assertThrows(SQLException.class, () -> getAllowedHosts(props(OIDC_ALLOWED_HOSTS, entry)));
            assertTrue(e.getMessage(), e.getMessage().contains(entry));
            assertTrue(e.getMessage(), e.getMessage().contains(OIDC_ALLOWED_HOSTS));
        }
    }

    @Test
    public void theNearMissOfAMissingDotIsSuggested() {
        SQLException e = assertThrows(SQLException.class,
            () -> getAllowedHosts(props(OIDC_ALLOWED_HOSTS, "*mycorp.net")));
        assertTrue(e.getMessage(), e.getMessage().contains("Did you mean '*.mycorp.net'?"));
    }

    /**
     * The principal name goes out in the very first handshake message and is what the server matches its
     * identity provider against, so dropping it -- as this driver did -- makes a server that selects a
     * provider by 'matchPattern' fail with a bare 'AuthenticationFailed' before any browser login.
     */
    @Test
    public void theUserNameBecomesTheOidcPrincipal() throws Exception {
        assertEquals("user@mycorp.net", principalOf("user@mycorp.net", props()));
        assertEquals("user@mycorp.net", principalOf("  user@mycorp.net  ", props()));
    }

    @Test
    public void noUserNameLeavesThePrincipalUnset() throws Exception {
        assertNull(principalOf(null, props()));
        assertNull(principalOf("", props()));
        assertNull(principalOf("   ", props()));
    }

    /**
     * The credential built here replaces the one the driver parsed out of the connection string, so a
     * username written as 'mongodb://alice@host/db' -- the only place it exists, since the URL is not
     * rewritten for OIDC -- used to be dropped along with it. The principal then went out unset and a
     * server selecting its identity provider by 'matchPattern' matched none, failing the connection with
     * a bare 'AuthenticationFailed'.
     */
    @Test
    public void aUserNameInTheUrlBecomesThePrincipal() throws Exception {
        assertEquals("alice@mycorp.net", principalOf(null, "alice@mycorp.net", props()));
        // as it does for every other mechanism, where insertCredentials leaves a URL carrying one alone
        assertEquals("alice@mycorp.net", principalOf("scram-user", "alice@mycorp.net", props()));
        assertEquals("scram-user", principalOf("scram-user", "   ", props()));
        assertNull(principalOf(null, "  ", props()));
    }

    @Test
    public void theOidcPrincipalPropertyStillOutranksTheUrl() throws Exception {
        assertEquals("svc@mycorp.net",
            principalOf(null, "alice@mycorp.net", props(OIDC_PRINCIPAL, "svc@mycorp.net")));
        assertNull(principalOf(null, "alice@mycorp.net", props(OIDC_PRINCIPAL, "none")));
    }

    @Test
    public void oidcPrincipalOverridesTheUserName() throws Exception {
        assertEquals("svc@mycorp.net",
            principalOf("scram-user", props(OIDC_PRINCIPAL, "svc@mycorp.net")));
        assertEquals("svc@mycorp.net",
            principalOf("scram-user", props(OIDC_PRINCIPAL, "  svc@mycorp.net  ")));
    }

    /**
     * The way out for a connection whose username is not the OIDC identity, a username kept after switching
     * from SCRAM being the usual case: sending it makes a server that selects its identity provider by
     * 'matchPattern' match none of them and fail with a bare 'AuthenticationFailed'.
     */
    @Test
    public void oidcPrincipalNoneSuppressesTheUserName() throws Exception {
        assertNull(principalOf("scram-user", props(OIDC_PRINCIPAL, "none")));
        assertNull(principalOf("scram-user", props(OIDC_PRINCIPAL, "NONE")));
        assertNull(principalOf("scram-user", props(OIDC_PRINCIPAL, " none ")));
        assertNull(principalOf("scram-user", props(OIDC_PRINCIPAL, "")));
    }

    @Test
    public void withoutTheOidcPrincipalPropertyTheUserNameIsUsed() throws Exception {
        assertEquals("user@mycorp.net", principalOf("user@mycorp.net", props()));
        assertNull(principalOf(null, props()));
    }

    @Test
    public void theCredentialCarriesTheCallbackAndTheAllowedHosts() throws Exception {
        MongoCredential credential =
            buildOidcCredential(null, null, CALLBACK, props(OIDC_ALLOWED_HOSTS, "*.mycorp.net"));

        assertEquals("MONGODB-OIDC", credential.getMechanism());
        assertEquals(CALLBACK, credential.getMechanismProperty(MongoCredential.OIDC_HUMAN_CALLBACK_KEY, null));
        assertEquals(withDefaults("*.mycorp.net"),
            credential.getMechanismProperty(MongoCredential.ALLOWED_HOSTS_KEY, null));
    }

    /**
     * The driver's own default is used when nothing is configured, rather than a copy of it: passing the
     * property at all is what the mechanism treats as an override.
     */
    @Test
    public void defaultAllowedHostsAreNotSetAsAnOverride() throws Exception {
        assertNull(buildOidcCredential(null, null, CALLBACK, props())
            .getMechanismProperty(MongoCredential.ALLOWED_HOSTS_KEY, null));
    }

    /** Asserted through the credential, so that what the handshake actually carries is what is pinned. */
    private static String principalOf(String username, Properties prop) throws Exception {
        return principalOf(username, null, prop);
    }

    private static String principalOf(String username, String urlUsername, Properties prop) throws Exception {
        return buildOidcCredential(username, urlUsername, CALLBACK, prop).getUserName();
    }

    private static Properties props(String... keyValue) {
        Properties prop = new Properties();
        for (int i = 0; i < keyValue.length; i += 2) {
            prop.setProperty(keyValue[i], keyValue[i + 1]);
        }
        return prop;
    }

    private static List<String> withDefaults(String... extra) {
        List<String> expected = new java.util.ArrayList<>(MongoCredential.DEFAULT_ALLOWED_HOSTS);
        expected.addAll(asList(extra));
        return expected;
    }
}
