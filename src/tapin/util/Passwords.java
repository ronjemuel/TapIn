package tapin.util;

/** Plain-text account passwords, as requested for phpMyAdmin editing. */
public final class Passwords {
    private Passwords() {}
    public static String store(char[] password) {
        if (password.length < 8 || password.length > 255)
            throw new IllegalArgumentException("Password must have 8 to 255 characters.");
        return new String(password);
    }
    public static boolean verify(char[] password, String stored) {
        return stored != null && !stored.isEmpty() && stored.equals(new String(password));
    }
}
