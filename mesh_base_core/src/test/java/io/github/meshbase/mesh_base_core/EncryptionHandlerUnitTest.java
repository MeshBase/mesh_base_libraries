package io.github.meshbase.mesh_base_core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

import io.github.meshbase.mesh_base_core.encryption.EncryptionHandler;

@RunWith(MockitoJUnitRunner.class)
public class EncryptionHandlerUnitTest {

    @Test
    public void testGenerateKeyPair() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) { // Mock Log
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PublicKey publicKey = cryptoHandler.getPublicKey();
            PrivateKey privateKey = cryptoHandler.getPrivateKey();
            assertNotNull(publicKey);
            assertNotNull(privateKey);
        }
    }

    @Test
    public void testGetPublicKey() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) { // Mock Log
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PublicKey publicKey = cryptoHandler.getPublicKey();
            assertNotNull(publicKey);
        }
    }

    @Test
    public void testGetPrivateKey() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) { // Mock Log
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PrivateKey privateKey = cryptoHandler.getPrivateKey();
            assertNotNull(privateKey);
        }
    }

    @Test
    public void testFingerprintPublicKey() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) { // Mock Log
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PublicKey publicKey = cryptoHandler.getPublicKey();
            assertNotNull(publicKey);
            UUID fingerprint = cryptoHandler.fingerprintPublicKey(publicKey);
            assertNotNull(fingerprint);
        }
    }

    @Test
    public void testFingerprintPublicKeyFromBytes() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) { // Mock Log
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PublicKey publicKey = cryptoHandler.getPublicKey();
            assertNotNull(publicKey);
            byte[] publicKeyBytes = publicKey.getEncoded();
            UUID fingerprint = cryptoHandler.fingerprintPublicKey(publicKeyBytes);
            assertNotNull(fingerprint);
        }
    }

    @Test
    public void testValidateFingerprint() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) { // Mock Log
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PublicKey publicKey = cryptoHandler.getPublicKey();
            assertNotNull(publicKey);
            UUID fingerprint = cryptoHandler.fingerprintPublicKey(publicKey);
            assertNotNull(fingerprint);
            boolean isValid = cryptoHandler.validateFingerprint(publicKey, fingerprint);
            assertTrue(isValid);
        }
    }

    @Test
    public void testValidateFingerprintFromBytes() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) { // Mock Log
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PublicKey publicKey = cryptoHandler.getPublicKey();
            assertNotNull(publicKey);
            byte[] publicKeyBytes = publicKey.getEncoded();
            UUID fingerprint = cryptoHandler.fingerprintPublicKey(publicKeyBytes);
            assertNotNull(fingerprint);
            boolean isValid = cryptoHandler.validateFingerprint(publicKeyBytes, fingerprint);
            assertTrue(isValid);
        }
    }

    @Test
    public void testEncryptDecrypt() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) { // Mock Log
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PublicKey publicKey = cryptoHandler.getPublicKey();
            PrivateKey privateKey = cryptoHandler.getPrivateKey();
            assertNotNull(publicKey);
            assertNotNull(privateKey);
            String plaintext = "This is a test message.";
            String ciphertext = cryptoHandler.encrypt(plaintext, publicKey);
            assertNotNull(ciphertext);
            String decryptedText = cryptoHandler.decrypt(ciphertext, privateKey);
            assertEquals(plaintext, decryptedText);
        }
    }

    @Test
    public void testEncryptDecryptWithBytes() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) {
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PublicKey publicKey = cryptoHandler.getPublicKey();
            PrivateKey privateKey = cryptoHandler.getPrivateKey();
            assertNotNull("Public key should not be null", publicKey);
            assertNotNull("Private key should not be null", privateKey);
            byte[] publicKeyBytes = publicKey.getEncoded();
            byte[] privateKeyBytes = privateKey.getEncoded();
            String plaintext = "This is a test message.";
            String ciphertext = cryptoHandler.encrypt(plaintext, publicKeyBytes);
            assertNotNull("Ciphertext (from bytes) should not be null", ciphertext);
            String decryptedText = cryptoHandler.decrypt(ciphertext, privateKeyBytes);
            assertNotNull("Decrypted text (from bytes) should not be null", decryptedText);
            assertEquals("Decrypted text (from bytes) should match plaintext", plaintext, decryptedText);
        }
    }

    @Test
    public void testConvertBytesToPublicKey() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) { // Mock Log
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PublicKey publicKey = cryptoHandler.getPublicKey();
            assertNotNull(publicKey);
            byte[] publicKeyBytes = publicKey.getEncoded();
            PublicKey recoveredPublicKey = cryptoHandler.bytesToPublicKey(publicKeyBytes);
            assertEquals(publicKey, recoveredPublicKey);
        }
    }

    @Test
    public void testConvertBytesToPrivateKey() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) { // Mock Log
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PrivateKey privateKey = cryptoHandler.getPrivateKey();
            assertNotNull(privateKey);
            byte[] privateKeyBytes = privateKey.getEncoded();
            PrivateKey recoveredPrivateKey = cryptoHandler.bytesToPrivateKey(privateKeyBytes);
            assertEquals(privateKey, recoveredPrivateKey);
        }
    }

    @Test
    public void testConvertBytesToUUID() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) { // Mock Log
            EncryptionHandler cryptoHandler = new EncryptionHandler();
            cryptoHandler.generateKeyPair();
            PublicKey publicKey = cryptoHandler.getPublicKey();
            UUID fingerprint = cryptoHandler.fingerprintPublicKey(publicKey);
            assertNotNull(fingerprint);
            // Get the bytes from the fingerprint.  This is a little tricky because UUID doesn't
            // directly give you the raw 16 bytes.  We have to extract them.
            byte[] fingerprintBytes = new byte[16];
            long msb = fingerprint.getMostSignificantBits();
            long lsb = fingerprint.getLeastSignificantBits();

            for (int i = 0; i < 8; i++) {
                fingerprintBytes[i] = (byte) (msb >>> (8 * (7 - i)));
            }
            for (int i = 0; i < 8; i++) {
                fingerprintBytes[8 + i] = (byte) (lsb >>> (8 * (7 - i)));
            }

            UUID convertedUuid = cryptoHandler.convertBytesToUUID(fingerprintBytes);
            assertEquals(fingerprint, convertedUuid);
        }
    }
}
