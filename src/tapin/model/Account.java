package tapin.model;

public record Account(int id, String fullName, String role) {
    public boolean admin() { return role.equals("ADMIN"); }
}
