package com.jibo.apptoolkit_desktop_example;

import java.security.MessageDigest;
import java.security.cert.CertificateException;
import javax.net.ssl.X509TrustManager;

import com.jibo.apptoolkit.protocol.utils.Util;

// a class to validate the x509 certificate against the websocket connection
class JiboX509TrustManager implements X509TrustManager {
    private String mClientCertificateFingerprint = "";

    JiboX509TrustManager(String clientCertificateFingerprint) {
        mClientCertificateFingerprint = clientCertificateFingerprint;
    }

    @Override
    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
    }

    @Override
    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
        boolean isFound = true;
        for (int i = 0; i < chain.length; i++) {
            try {
                java.security.cert.X509Certificate certificate = chain[i];
                byte[] encCertInfo = certificate.getEncoded();
                MessageDigest md = MessageDigest.getInstance("SHA1");
                byte[] digest = md.digest(encCertInfo);
                String certificateFingerprint = Util.toHexString(digest);

                // check that the certificate we received from the jibo server is the same that the robot has replied with
                // this helps check against someone possibly spoofing an endpoint for us to connect to
                if (certificateFingerprint.equals(mClientCertificateFingerprint.toUpperCase())) {
                    isFound = true;
                    break;
                }
            } catch (Exception e) {
                System.out.println("Exception: " + e);
            }
        }
        if (!isFound) throw new CertificateException();
    }

    @Override
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[]{};
    }
};