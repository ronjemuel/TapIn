package tapin.model;

/** IDs, rather than display names, are used for combo-box selections. */
public record Choice(int id, String label) {
    @Override public String toString() { return label; }
}
