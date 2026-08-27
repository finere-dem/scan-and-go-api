package com.finere.scan_and_go_api.security;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Loads a PEM-encoded RSA key pair used to sign/verify JWTs asymmetrically. */
public final class RsaKeyLoader {

    private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

    private RsaKeyLoader() {
    }

    public static RSAPrivateKey loadPrivateKey(String path) {
        try {
            byte[] decoded = decodePemBody(readResourceText(path));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to load RSA private key from " + path, e);
        }
    }

    public static RSAPublicKey loadPublicKey(String path) {
        try {
            byte[] decoded = decodePemBody(readResourceText(path));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to load RSA public key from " + path, e);
        }
    }

    /** Same as {@link #loadPrivateKey} but takes the PEM text directly rather than a resource
     * location - used when the key is supplied via an environment variable (cloud deployments
     * where the key file itself is gitignored and never present in the built image). */
    public static RSAPrivateKey loadPrivateKeyFromPem(String pemText) {
        try {
            byte[] decoded = decodePemBody(pemText);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to load RSA private key from PEM text", e);
        }
    }

    public static RSAPublicKey loadPublicKeyFromPem(String pemText) {
        try {
            byte[] decoded = decodePemBody(pemText);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to load RSA public key from PEM text", e);
        }
    }

    private static String readResourceText(String path) throws IOException {
        Resource resource = RESOURCE_LOADER.getResource(path);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes());
        }
    }

    private static byte[] decodePemBody(String pemText) {
        String pem = pemText
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(pem);
    }
}
