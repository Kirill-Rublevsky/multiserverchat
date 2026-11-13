package ru.ovenquill.globalchat.protocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

public record SignedPayload(UUID playerId, long timestamp, UUID nonce, String message, byte[] signature) {
    public static final int VERSION = 1;
    public static final int MAX_PACKET_BYTES = 8192;

    public static SignedPayload create(UUID playerId, String message, String secret, long timestamp, UUID nonce) {
        return new SignedPayload(playerId, timestamp, nonce, message,
                sign(playerId, timestamp, nonce, message, secret));
    }

    public boolean verify(String secret, long now, long maxSkewMillis) {
        if (Math.abs(now - timestamp) > maxSkewMillis) return false;
        byte[] expected = sign(playerId, timestamp, nonce, message, secret);
        return MessageDigest.isEqual(expected, signature);
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(VERSION);
            out.writeLong(playerId.getMostSignificantBits()); out.writeLong(playerId.getLeastSignificantBits());
            out.writeLong(timestamp);
            out.writeLong(nonce.getMostSignificantBits()); out.writeLong(nonce.getLeastSignificantBits());
            byte[] text = message.getBytes(StandardCharsets.UTF_8);
            out.writeInt(text.length); out.write(text);
            out.writeInt(signature.length); out.write(signature);
        }
        if (bytes.size() > MAX_PACKET_BYTES) throw new IOException("Packet too large");
        return bytes.toByteArray();
    }

    public static SignedPayload decode(byte[] data) throws IOException {
        if (data.length > MAX_PACKET_BYTES) throw new IOException("Packet too large");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (in.readInt() != VERSION) throw new IOException("Unsupported protocol version");
            UUID playerId = new UUID(in.readLong(), in.readLong());
            long timestamp = in.readLong();
            UUID nonce = new UUID(in.readLong(), in.readLong());
            int textLength = in.readInt();
            if (textLength < 0 || textLength > 4096 || textLength > in.available()) throw new IOException("Invalid text length");
            String message = new String(in.readNBytes(textLength), StandardCharsets.UTF_8);
            int signatureLength = in.readInt();
            if (signatureLength != 32 || signatureLength > in.available()) throw new IOException("Invalid signature length");
            byte[] signature = in.readNBytes(signatureLength);
            if (in.available() != 0) throw new IOException("Trailing packet data");
            return new SignedPayload(playerId, timestamp, nonce, message, signature);
        }
    }

    private static byte[] sign(UUID playerId, long timestamp, UUID nonce, String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(canonical(playerId, timestamp, nonce, message));
            return mac.doFinal();
        } catch (Exception e) { throw new IllegalStateException("Unable to sign payload", e); }
    }

    private static byte[] canonical(UUID playerId, long timestamp, UUID nonce, String message) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeLong(playerId.getMostSignificantBits()); out.writeLong(playerId.getLeastSignificantBits());
            out.writeLong(timestamp);
            out.writeLong(nonce.getMostSignificantBits()); out.writeLong(nonce.getLeastSignificantBits());
            byte[] text = message.getBytes(StandardCharsets.UTF_8); out.writeInt(text.length); out.write(text);
        }
        return bytes.toByteArray();
    }

    @Override public String toString() { return "SignedPayload[playerId=" + playerId + ", timestamp=" + timestamp + ", nonce=" + nonce + ", signature=" + HexFormat.of().formatHex(signature, 0, 4) + "…]"; }
}
