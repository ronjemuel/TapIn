package tapin.service;

import java.sql.*;
import java.util.*;
import tapin.dao.Sql;
import tapin.model.Account;
import tapin.util.*;

public final class AuthService {
    public Account login(String username, char[] password, String role) throws SQLException {
        try {
            if (username.trim().isEmpty()) throw new IllegalArgumentException("Enter your username.");
            if (password.length == 0) throw new IllegalArgumentException("Enter your password.");
            try (Connection c = Database.open()) {
                Map<String,Object> r = Sql.one(c, "SELECT * FROM accounts WHERE username=?", username.trim());
                if (r == null || !Passwords.verify(password, Sql.str(r,"password"))) throw new IllegalArgumentException("Incorrect username or password.");
                if (!role.equals(Sql.str(r,"role"))) throw new IllegalArgumentException("Wrong role. Choose the role assigned to this account.");
                return new Account(Sql.id(r,"account_id"), Sql.str(r,"full_name"), role);
            }
        } finally { Arrays.fill(password, '\0'); }
    }
    public static void requireAdmin(Account a) {
        if (a == null || !a.admin()) throw new IllegalArgumentException("Administrator access is required.");
    }
    public static void requireTeacher(Account a, int teacherId) {
        if (a == null || a.admin() || a.id() != teacherId) throw new IllegalArgumentException("Only the assigned teacher can change this class attendance.");
    }
}
