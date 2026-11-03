package io.github.tuyucheng777.quarkforge.crypto;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CryptoTest {

    @Inject
    CryptoService cryptoService;

    @Test
    void encryptsAndDecryptsRoundTrip() {
        String plaintext = "13800000000";
        String ciphertext = cryptoService.encrypt(plaintext);

        assertTrue(ciphertext.startsWith("v1:"));
        assertNotEquals(plaintext, ciphertext);
        assertEquals(plaintext, cryptoService.decrypt(ciphertext));
    }

    @Test
    void samePlaintextProducesDifferentCiphertext() {
        assertNotEquals(cryptoService.encrypt("hello"), cryptoService.encrypt("hello"));
    }

    @Test
    void rejectsTamperedOrUnknownCiphertext() {
        String ciphertext = cryptoService.encrypt("secret");
        String tampered = ciphertext.substring(0, ciphertext.length() - 2) + "AA";

        assertThrows(RuntimeException.class, () -> cryptoService.decrypt(tampered));
        assertThrows(IllegalArgumentException.class, () -> cryptoService.decrypt("plain-text"));
    }

    @Test
    void nullPassesThrough() {
        assertNull(cryptoService.encrypt(null));
        assertNull(cryptoService.decrypt(null));
    }

    @Test
    void masksCommonSensitiveTypes() {
        assertEquals("138****0000", Masking.mask("13800000000", SensitiveType.PHONE));
        assertEquals("110101********1234", Masking.mask("110101199001011234", SensitiveType.ID_CARD));
        assertEquals("a***@example.com", Masking.mask("alice@example.com", SensitiveType.EMAIL));
        assertEquals("**** 0123", Masking.mask("6222021234567890123", SensitiveType.BANK_CARD));
        assertEquals("张*", Masking.mask("张三", SensitiveType.NAME));
        assertEquals("******", Masking.mask("p@ssw0rd", SensitiveType.PASSWORD));
        assertEquals("A***E", Masking.mask("ABCDE", SensitiveType.DEFAULT));
        assertNull(Masking.mask(null, SensitiveType.PHONE));
    }
}
