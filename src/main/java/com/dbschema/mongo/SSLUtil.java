package com.dbschema.mongo;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;

import static com.dbschema.mongo.Util.isNullOrEmpty;

public class SSLUtil {
  public static SSLContext getTrustEverybodySSLContext(String clientCertificateKeyStoreUrl, String clientCertificateKeyStoreType, String clientCertificateKeyStorePassword) throws SSLParamsException {
    KeyManagerFactory kmf;
    KeyManager[] kms = null;

    try {
      kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    }
    catch (NoSuchAlgorithmException nsae) {
      throw new SSLParamsException("Default algorithm definitions for TrustManager and/or KeyManager are invalid.  Check java security properties file.", nsae);
    }

    if (!isNullOrEmpty(clientCertificateKeyStoreUrl)) {
      InputStream ksIS = null;
      try {
        if (!isNullOrEmpty(clientCertificateKeyStoreType)) {
          KeyStore clientKeyStore = KeyStore.getInstance(clientCertificateKeyStoreType);
          URL ksURL = new URL(clientCertificateKeyStoreUrl);
          char[] password = (clientCertificateKeyStorePassword == null) ? new char[0] : clientCertificateKeyStorePassword.toCharArray();
          ksIS = ksURL.openStream();
          clientKeyStore.load(ksIS, password);
          kmf.init(clientKeyStore, password);
          kms = kmf.getKeyManagers();
        }
      }
      catch (UnrecoverableKeyException uke) {
        throw new SSLParamsException("Could not recover keys from client keystore.  Check password?", uke);
      }
      catch (NoSuchAlgorithmException nsae) {
        throw new SSLParamsException("Unsupported keystore algorithm [" + nsae.getMessage() + "]", nsae);
      }
      catch (KeyStoreException kse) {
        throw new SSLParamsException("Could not create KeyStore instance [" + kse.getMessage() + "]", kse);
      }
      catch (CertificateException nsae) {
        throw new SSLParamsException("Could not load client" + clientCertificateKeyStoreType + " keystore from " + clientCertificateKeyStoreUrl, nsae);
      }
      catch (MalformedURLException mue) {
        throw new SSLParamsException(clientCertificateKeyStoreUrl + " does not appear to be a valid URL.", mue);
      }
      catch (IOException ioe) {
        throw new SSLParamsException("Cannot open " + clientCertificateKeyStoreUrl + " [" + ioe.getMessage() + "]", ioe);
      }
      finally {
        if (ksIS != null) {
          try {
            ksIS.close();
          }
          catch (IOException e) {
            // can't close input stream, but keystore can be properly initialized so we shouldn't throw this exception
          }
        }
      }
    }

    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(kms, new TrustManager[]{new MyTrustEverybodyManager()}, null);
      return sslContext;

    }
    catch (NoSuchAlgorithmException nsae) {
      throw new SSLParamsException("TLS is not a valid SSL protocol.", nsae);
    }
    catch (KeyManagementException kme) {
      throw new SSLParamsException("KeyManagementException: " + kme.getMessage(), kme);
    }
  }

  private static class MyTrustEverybodyManager implements X509TrustManager {
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
    }

    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }

  public static class SSLParamsException extends SQLException {
    public SSLParamsException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
