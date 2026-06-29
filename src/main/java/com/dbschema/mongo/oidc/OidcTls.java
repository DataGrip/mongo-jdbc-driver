package com.dbschema.mongo.oidc;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
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
 *   <li>on Windows, the native {@code Windows-ROOT} store (which honors the OS trust settings);</li>
 *   <li>on macOS, certificates exported from the keychains — only when the caller opts in via
 *       {@code trustSystemKeychain} (see below).</li>
 * </ol>
 * A server certificate is accepted if any of these trust managers accepts it.
 *
 * <p><b>macOS keychain (opt-in).</b> Enumerating the keychain via {@code security find-certificate}
 * ignores per-certificate trust settings and would promote leaf / explicitly distrusted
 * certificates to trust anchors. It is therefore disabled by default and only enabled when the
 * {@code oidcTrustSystemKeychain} driver property is set to {@code true}.
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
   *                            keychains (ignores macOS trust settings — see class doc)
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

    // Native OS trust store.
    String osName = System.getProperty("os.name", "").toLowerCase();
    if (osName.contains("windows")) {
      // Windows-ROOT honors the OS trust configuration.
      addTrustManager(trustManagers, loadWindowsRootStore());
    }
    else if (osName.contains("mac") && trustSystemKeychain) {
      // Opt-in: the JDK "KeychainStore" type only exposes the user login keychain and misses
      // corporate roots in the System / System Roots keychains, so export them via the `security` CLI.
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
