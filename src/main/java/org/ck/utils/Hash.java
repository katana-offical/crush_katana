package org.ck.utils;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Angus
 */
public class Hash {

    /**
     * Generates SHA-256 digest for the given {@code input}.
     *
     * @param input The input to digest
     * @return The hash value for the given input
     * @throws RuntimeException If we couldn't find any SHA-256 provider
     */
    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Couldn't find a SHA-256 provider", e);
        }
    }

    public static byte[] hash160(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] sha256Hash = sha256.digest(input);

        RIPEMD160Digest ripemd160 = new RIPEMD160Digest();
        ripemd160.update(sha256Hash, 0, sha256Hash.length);
        byte[] ripemd160Hash = new byte[ripemd160.getDigestSize()];
        ripemd160.doFinal(ripemd160Hash, 0);

        return ripemd160Hash;
    }
}
