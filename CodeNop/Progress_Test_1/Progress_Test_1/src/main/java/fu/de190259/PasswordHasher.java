package fu.de190259;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Băm mật khẩu SHA-256 + salt. Kết quả là chuỗi hex 64 ký tự. */
public final class PasswordHasher {

    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    /** Sinh salt ngẫu nhiên 16 bytes, trả về chuỗi hex 32 ký tự. */
    public static String generateSalt() {
        byte[] bytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Băm SHA-256(salt + rawPassword), trả về chuỗi hex 64 ký tự. */
    public static String hash(String salt, String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** So sánh an toàn (constant-time). null ở bất kỳ tham số nào trả false. */
    public static boolean matches(String salt, String raw, String expectedHash) {
        if (salt == null || raw == null || expectedHash == null) {
            return false;
        }
        byte[] actual   = hash(salt, raw).getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }
}
