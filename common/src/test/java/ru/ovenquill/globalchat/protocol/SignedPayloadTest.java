package ru.ovenquill.globalchat.protocol;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SignedPayloadTest {
    @Test void roundTripAndVerifyUnicode() throws Exception {
        long now = 123456789L;
        SignedPayload original = SignedPayload.create(UUID.randomUUID(), "Привет 🌍", "secret-long-enough", now, UUID.randomUUID());
        SignedPayload decoded = SignedPayload.decode(original.encode());
        assertEquals("Привет 🌍", decoded.message());
        assertTrue(decoded.verify("secret-long-enough", now, 30_000));
        assertFalse(decoded.verify("wrong", now, 30_000));
    }
    @Test void rejectsExpiredPayload() {
        SignedPayload payload = SignedPayload.create(UUID.randomUUID(), "x", "secret", 1000, UUID.randomUUID());
        assertFalse(payload.verify("secret", 31_001, 30_000));
    }
}
