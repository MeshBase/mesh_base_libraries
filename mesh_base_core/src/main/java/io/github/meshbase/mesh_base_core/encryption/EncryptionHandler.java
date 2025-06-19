package io.github.meshbase.mesh_base_core.encryption;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

public class EncryptionHandler {
    private static final String TAG = "my_encryption_handler";
    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEY_SIZE = 2048; // Use a strong key size
    private KeyPair keyPair;

    public void generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyPairGenerator.initialize(RSA_KEY_SIZE);
            this.keyPair = keyPairGenerator.generateKeyPair();
            Log.d(TAG, "Successfully generated key pairs");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "RSA algorithm not found", e);
            this.keyPair = null; // Ensure keyPair is null on failure
        }
    }

    public PublicKey getPublicKey() {
        return (this.keyPair != null) ? this.keyPair.getPublic() : null;
    }

    public PrivateKey getPrivateKey() {
        return (this.keyPair != null) ? this.keyPair.getPrivate() : null;
    }

    public static UUID fingerprintPublicKey(PublicKey publicKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(publicKey.getEncoded());
            return convertBytesToUUID(digest);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5 algorithm not found", e);
            return null; // Return null on error
        }
    }

    public static UUID fingerprintPublicKey(byte[] publicKeyBytes) {
        try {
            PublicKey publicKey = bytesToPublicKey(publicKeyBytes);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(publicKey.getEncoded());
            return convertBytesToUUID(digest);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5 algorithm not found", e);
            return null;
        }
    }

    public static boolean validateFingerprint(PublicKey publicKey, UUID expectedFingerprint) {
        if (publicKey == null || expectedFingerprint == null) {
            return false;
        }
        UUID generatedFingerprint = fingerprintPublicKey(publicKey);
        return generatedFingerprint != null && generatedFingerprint.equals(expectedFingerprint);
    }

    public boolean validateFingerprint(byte[] publicKeyBytes, UUID expectedFingerprint) {
        if (publicKeyBytes == null || expectedFingerprint == null) {
            return false;
        }
        UUID generatedFingerprint = fingerprintPublicKey(publicKeyBytes);
        return generatedFingerprint != null && generatedFingerprint.equals(expectedFingerprint);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String encrypt(String plaintext, PublicKey publicKey) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            Log.e(TAG, "RSA encryption failed", e);
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String encrypt(String plaintext, byte[] publicKeyBytes) {
        try {
            PublicKey publicKey = bytesToPublicKey(publicKeyBytes);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            Log.e(TAG, "RSA encryption failed", e);
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String decrypt(String ciphertext, PrivateKey privateKey) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey);
            byte[] decodedBytes = Base64.getDecoder().decode(ciphertext);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "RSA decryption failed", e);
            return null; // Return null on error
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String decrypt(String ciphertext, byte[] privateKeyBytes) {
        try {
            PrivateKey privateKey = bytesToPrivateKey(privateKeyBytes);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey);
            byte[] decodedBytes = Base64.getDecoder().decode(ciphertext);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "RSA decryption failed", e);
            return null;
        }
    }

    public static UUID convertBytesToUUID(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("Byte array must be 16 bytes long for UUID conversion");
        }
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++)
            msb = (msb << 8) | (bytes[i] & 0xff);
        for (int i = 8; i < 16; i++)
            lsb = (lsb << 8) | (bytes[i] & 0xff);
        return new UUID(msb, lsb);
    }

    public static PublicKey bytesToPublicKey(byte[] keyBytes) {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            return keyFactory.generatePublic(spec);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException: " + e.getMessage());
            return null;
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "InvalidKeySpecException: " + e.getMessage());
            return null;
        }
    }

    public static PrivateKey bytesToPrivateKey(byte[] keyBytes) {
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            return keyFactory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException: " + e.getMessage());
            return null;
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "InvalidKeySpecException: " + e.getMessage());
            return null;
        }
    }

}