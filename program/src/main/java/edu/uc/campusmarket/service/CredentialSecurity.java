package edu.uc.campusmarket.service;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Salted password/recovery hashing; plaintext secrets are never persisted. */
public final class CredentialSecurity {
    public static final int SALT_BYTES = 16;
    public static final int RECOVERY_BYTES = 18;
    public static final int HASH_ITERATIONS = 210_000;
    public static final int HASH_BITS = 256;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;

    private CredentialSecurity() {
    }

    public static byte[] randomBytes(int count) {
        byte[] bytes = new byte[count];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    public static String randomSecret() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(RECOVERY_BYTES));
    }

    public static byte[] hash(char[] secret, byte[] salt) {
        PBEKeySpec specification = new PBEKeySpec(secret, salt, HASH_ITERATIONS, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new AppException("Password verification is unavailable.", error);
        } finally {
            specification.clearPassword();
        }
    }
}
