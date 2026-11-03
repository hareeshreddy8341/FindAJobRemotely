package com.findajobremotely.core;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM encryption for private fields (phone number, salary expectation, resume files).
 *
 * <p>Every value gets a fresh random 96-bit IV. A "context" string (the owner's user id) is bound
 * to the ciphertext as additional authenticated data, so an encrypted value copied onto another
 * user's record fails to decrypt.
 */
public final class FieldCrypto {

    private static final String PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    /** @param base64Key a Base64 encoded 32-byte key, e.g. generated with {@code openssl rand -base64 32} */
    public FieldCrypto(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException(
                    "DATA_ENCRYPTION_KEY is not set. Generate one with: openssl rand -base64 32");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("DATA_ENCRYPTION_KEY must be valid Base64");
        }
        if (raw.length != 32) {
            throw new IllegalArgumentException(
                    "DATA_ENCRYPTION_KEY must decode to exactly 32 bytes (got " + raw.length + ")");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plain, String context) {
        if (plain == null) {
            return null;
        }
        return PREFIX + Base64.getEncoder().encodeToString(encryptBytes(plain.getBytes(StandardCharsets.UTF_8), context));
    }

    public String decrypt(String encrypted, String context) {
        if (encrypted == null) {
            return null;
        }
        if (!encrypted.startsWith(PREFIX)) {
            throw new IllegalStateException("Unrecognised encrypted value format");
        }
        byte[] data = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
        return new String(decryptBytes(data, context), StandardCharsets.UTF_8);
    }

    /** Output layout: 12-byte IV followed by ciphertext and GCM tag. */
    public byte[] encryptBytes(byte[] plain, String context) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] ct = cipher.doFinal(plain);
            return ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public byte[] decryptBytes(byte[] data, String context) {
        if (data == null || data.length <= IV_BYTES) {
            throw new IllegalStateException("Encrypted payload is too short");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Decryption failed (wrong key or tampered data)", e);
        }
    }
}
