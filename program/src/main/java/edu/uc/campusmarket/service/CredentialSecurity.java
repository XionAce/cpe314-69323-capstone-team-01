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

    /**
     * Returns cryptographically random bytes of the requested length.
     *
     * @param count number of random bytes
     * @return a new random byte array
     */
    public static byte[] randomBytes(int count) {
        byte[] bytes = new byte[count];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    /**
     * Generates a URL-safe initial password or recovery token.
     *
     * @return an unpadded random token
     */
    public static String randomSecret() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(RECOVERY_BYTES));
    }

    /**
     * Derives a salted PBKDF2 hash without persisting the plaintext.
     *
     * @param secret submitted password or recovery code
     * @param salt   per-credential random salt
     * @return the derived hash bytes
     * @throws AppException when the hashing provider is unavailable
     */
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
