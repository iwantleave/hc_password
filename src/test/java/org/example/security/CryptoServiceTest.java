package org.example.security;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CryptoServiceTest {
    private final CryptoService crypto = new CryptoService();

    @Test
    void encryptsAndDecryptsWithDerivedKey() throws Exception {
        byte[] salt = crypto.newSalt();
        SecretKey key = crypto.deriveKey("master-password".toCharArray(), salt, 10_000);
        String plaintext = "s3cret-密码-value";

        String encrypted = crypto.encryptString(key, plaintext);

        assertNotEquals(plaintext, encrypted);
        assertArrayEquals(plaintext.getBytes(StandardCharsets.UTF_8),
                crypto.decryptString(key, encrypted).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void usesRandomIvForEveryEncryption() throws Exception {
        byte[] salt = crypto.newSalt();
        SecretKey key = crypto.deriveKey("master-password".toCharArray(), salt, 10_000);

        String first = crypto.encryptString(key, "same-value");
        String second = crypto.encryptString(key, "same-value");

        assertNotEquals(first, second);
        assertNotEquals("same-value", first);
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        byte[] salt = crypto.newSalt();
        SecretKey correctKey = crypto.deriveKey("master-password".toCharArray(), salt, 10_000);
        SecretKey wrongKey = crypto.deriveKey("wrong-password".toCharArray(), salt, 10_000);
        String encrypted = crypto.encryptString(correctKey, "secret");

        assertThrows(GeneralSecurityException.class,
                () -> crypto.decryptString(wrongKey, encrypted));
    }

    @Test
    void derivesDifferentKeysFromDifferentSalts() throws Exception {
        SecretKey first = crypto.deriveKey("master-password".toCharArray(),
                crypto.newSalt(), 10_000);
        SecretKey second = crypto.deriveKey("master-password".toCharArray(),
                crypto.newSalt(), 10_000);

        assertFalse(java.util.Arrays.equals(first.getEncoded(), second.getEncoded()));
    }
}
