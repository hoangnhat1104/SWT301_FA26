package fu.de190259;

/**
 * Placeholder – sẽ được cài đặt đầy đủ ở TODO-3.
 */
public final class PasswordHasher {

    private PasswordHasher() {}

    public static String generateSalt() {
        throw new UnsupportedOperationException("TODO");
    }

    public static String hash(String salt, String rawPassword) {
        throw new UnsupportedOperationException("TODO");
    }

    public static boolean matches(String salt, String raw, String expectedHash) {
        throw new UnsupportedOperationException("TODO");
    }
}
