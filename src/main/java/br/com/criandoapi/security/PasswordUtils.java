package br.com.criandoapi.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtils {

    private PasswordUtils() {
    }

    public static String hash(String rawPassword) {
        return sha256(rawPassword);
    }

    public static boolean matches(String rawPassword, String hashedPassword) {
        return hash(rawPassword).equals(hashedPassword);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String hexPart = Integer.toHexString(0xff & b);
                if (hexPart.length() == 1) {
                    hex.append('0');
                }
                hex.append(hexPart);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Nao foi possivel gerar hash da senha", e);
        }
    }
}

