package com.dbschema.mongo.oidc;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds an {@link SSLSocketFactory} that trusts the system certificate stores so that outgoing
 * OIDC HTTPS calls (issuer metadata resolution, token endpoint) succeed in environments with a
 * corporate / custom root CA.
 *
 * <p>The composite trust manager aggregates:
 * <ol>
 *   <li>the JRE default trust store — {@code cacerts}, or {@code javax.net.ssl.trustStore} when that
 *       system property is set;</li>
 *   <li>on Windows, the native {@code Windows-ROOT} store (honors the OS trust settings);</li>
 *   <li>on Linux, the curated system CA bundle (populated by {@code update-ca-certificates} /
 *       {@code update-ca-trust}), which Java itself does not read by default;</li>
 *   <li>on macOS, certificates exported from the keychains — only when the caller opts in via
 *       {@code trustSystemKeychain} (see below).</li>
 * </ol>
 * A server certificate is accepted if any of these trust managers accepts it.
 *
 * <p>Windows-ROOT and the Linux CA bundle are curated OS trust stores, so they are always trusted.
 *
 * <p><b>macOS keychain (opt-in).</b> Disabled by default and only enabled when the
 * {@code oidcTrustSystemKeychain} driver property is {@code true}. Enumerating the keychain via
 * {@code security find-certificate} ignores per-certificate trust settings and would promote
 * leaf / explicitly distrusted certificates to trust anchors.
 */
final class OidcTls {

  private static final Logger logger = Logger.getLogger(OidcTls.class.getName());

  /** Lazily built socket factories, keyed by the {@code trustSystemKeychain} flag. */
  private static final Map<Boolean, SSLSocketFactory> CACHE = new ConcurrentHashMap<>();

  private OidcTls() {
  }

  /**
   * Returns a cached {@link SSLSocketFactory} for the given trust configuration, building it on
   * first use.
   *
   * @param trustSystemKeychain when {@code true}, also trust certificates exported from the macOS
   *                            keychains (macOS only; ignores macOS trust settings — see class doc)
   */
  static SSLSocketFactory systemSocketFactory(boolean trustSystemKeychain) throws GeneralSecurityException {
    SSLSocketFactory cached = CACHE.get(trustSystemKeychain);
    if (cached != null) {
      return cached;
    }
    // Not using computeIfAbsent: building throws a checked exception.
    SSLSocketFactory factory = buildSocketFactory(trustSystemKeychain);
    CACHE.put(trustSystemKeychain, factory);
    return factory;
  }

  private static SSLSocketFactory buildSocketFactory(boolean trustSystemKeychain) throws GeneralSecurityException {
    List<X509TrustManager> trustManagers = new ArrayList<>();

    // JRE default trust store: cacerts, or javax.net.ssl.trustStore when that property is set
    // (init((KeyStore) null) honors it).
    addDefaultTrustManager(trustManagers);

    // Native OS trust store (curated, always trusted).
    String osName = System.getProperty("os.name", "").toLowerCase();
    if (osName.contains("windows")) {
      // Windows-ROOT honors the OS trust configuration.
      addTrustManager(trustManagers, loadWindowsRootStore());
    }
    else if (osName.contains("linux")) {
      // The curated system CA bundle; Java does not read it by default, but bundled JREs (e.g.
      // the IDE's JBR) rely on this to pick up CAs installed via update-ca-certificates.
      addTrustManager(trustManagers, loadLinuxSystemCertificates());
    }

    // macOS keychain: opt-in only (ignores per-certificate trust settings — see class doc).
    if (osName.contains("mac") && trustSystemKeychain) {
      // The JDK "KeychainStore" type only exposes the user login keychain and misses corporate
      // roots in the System / System Roots keychains, so export them via the `security` CLI.
      addTrustManager(trustManagers, loadMacSystemCertificates());
    }

    if (trustManagers.isEmpty()) {
      throw new GeneralSecurityException("No trust managers could be initialized");
    }

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[]{new CompositeTrustManager(trustManagers)}, null);
    return sslContext.getSocketFactory();
  }

  /** Adds the JRE default trust store (cacerts / {@code javax.net.ssl.trustStore}). */
  private static void addDefaultTrustManager(List<X509TrustManager> target) {
    initTrustManager(target, null);
  }

  /** Adds the given store, or does nothing when {@code keyStore} is null (e.g. it failed to load). */
  private static void addTrustManager(List<X509TrustManager> target, KeyStore keyStore) {
    if (keyStore == null) {
      return;
    }
    initTrustManager(target, keyStore);
  }

  private static void initTrustManager(List<X509TrustManager> target, KeyStore keyStore) {
    try {
      TrustManagerFactory factory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      factory.init(keyStore);
      for (TrustManager tm : factory.getTrustManagers()) {
        if (tm instanceof X509TrustManager) {
          target.add((X509TrustManager) tm);
        }
      }
    }
    catch (GeneralSecurityException e) {
      logger.log(Level.WARNING, "Skipping trust store that failed to initialize: " + e.getMessage());
    }
  }

  /** Loads the native Windows root store, or {@code null} if unavailable. */
  private static KeyStore loadWindowsRootStore() {
    try {
      KeyStore keyStore = KeyStore.getInstance("Windows-ROOT");
      keyStore.load(null, null);
      return keyStore;
    }
    catch (Exception e) {
      logger.log(Level.WARNING, "Failed to load Windows-ROOT trust store: " + e.getMessage());
      return null;
    }
  }

  /** Curated system CA bundles populated by update-ca-certificates / update-ca-trust, by distro. */
  private static final String[] LINUX_CA_BUNDLES = {
      "/etc/ssl/certs/ca-certificates.crt",                  // Debian, Ubuntu, Alpine
      "/etc/pki/tls/certs/ca-bundle.crt",                    // RHEL, CentOS, Fedora
      "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem",   // RHEL (extracted)
      "/etc/ssl/ca-bundle.pem",                              // openSUSE
      "/etc/ssl/cert.pem",                                   // misc
  };

  /**
   * Loads the first available Linux system CA bundle into an in-memory trust store, or {@code null}
   * when none is readable. The bundle is curated by the OS, so its contents are safe trust anchors.
   */
  private static KeyStore loadLinuxSystemCertificates() {
    for (String path : LINUX_CA_BUNDLES) {
      Path bundle = Path.of(path);
      if (!Files.isReadable(bundle)) {
        continue;
      }
      try (InputStream is = Files.newInputStream(bundle)) {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certs = certFactory.generateCertificates(is);
        if (certs.isEmpty()) {
          continue;
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        int index = 0;
        for (Certificate cert : certs) {
          keyStore.setCertificateEntry("linux-" + (index++), cert);
        }
        logger.log(Level.FINE, "Loaded {0} certificates from {1}", new Object[]{index, path});
        return keyStore;
      }
      catch (Exception e) {
        logger.log(Level.WARNING, "Failed to load CA bundle " + path + ": " + e.getMessage());
      }
    }
    return null;
  }

  /** Keychains scanned for trusted roots on macOS when {@code oidcTrustSystemKeychain} is enabled. */
  private static final String[] MAC_KEYCHAINS = {
      "/System/Library/Keychains/SystemRootCertificates.keychain",
      "/Library/Keychains/System.keychain",
      System.getProperty("user.home", "") + "/Library/Keychains/login.keychain-db",
  };

  /**
   * Exports certificates from the macOS keychains via {@code /usr/bin/security} and loads them into
   * an in-memory trust store. Returns {@code null} when nothing could be read.
   */
  private static KeyStore loadMacSystemCertificates() {
    try {
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);

      int index = 0;
      for (String keychain : MAC_KEYCHAINS) {
        for (Certificate cert : exportKeychainCertificates(certFactory, keychain)) {
          keyStore.setCertificateEntry("mac-" + (index++), cert);
        }
      }

      if (index == 0) {
        return null;
      }
      logger.log(Level.FINE, "Loaded {0} certificates from macOS keychains", index);
      return keyStore;
    }
    catch (Exception e) {
      logger.log(Level.WARNING, "Failed to load macOS system certificates: " + e.getMessage());
      return null;
    }
  }

  private static Collection<? extends Certificate> exportKeychainCertificates(
      CertificateFactory certFactory, String keychain) {
    try {
      Process process = new ProcessBuilder("/usr/bin/security", "find-certificate", "-a", "-p", keychain)
          .redirectErrorStream(false)
          .start();
      byte[] pem = process.getInputStream().readAllBytes();
      process.waitFor();
      if (pem.length == 0) {
        return List.of();
      }
      return certFactory.generateCertificates(new ByteArrayInputStream(pem));
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return List.of();
    }
    catch (Exception e) {
      logger.log(Level.WARNING, "Failed to export certificates from " + keychain + ": " + e.getMessage());
      return List.of();
    }
  }

  private static final class CompositeTrustManager implements X509TrustManager {

    private final List<X509TrustManager> delegates;

    private CompositeTrustManager(List<X509TrustManager> delegates) {
      this.delegates = delegates;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      CertificateException last = null;
      for (X509TrustManager delegate : delegates) {
        try {
          delegate.checkClientTrusted(chain, authType);
          return;
        }
        catch (CertificateException e) {
          last = e;
        }
      }
      throw last != null ? last : new CertificateException("No trust manager accepted the client certificate");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      CertificateException last = null;
      for (X509TrustManager delegate : delegates) {
        try {
          delegate.checkServerTrusted(chain, authType);
          return;
        }
        catch (CertificateException e) {
          last = e;
        }
      }
      throw last != null ? last : new CertificateException("No trust manager accepted the server certificate");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      List<X509Certificate> issuers = new ArrayList<>();
      for (X509TrustManager delegate : delegates) {
        java.util.Collections.addAll(issuers, delegate.getAcceptedIssuers());
      }
      return issuers.toArray(new X509Certificate[0]);
    }
  }
}
