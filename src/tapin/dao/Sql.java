package tapin.dao;

import java.sql.*;
import java.util.*;

/** SQL text is owned by DAO/service code. All values use bound parameters. */
public final class Sql {
    private Sql() {}
    public static PreparedStatement prepare(Connection c, String sql, Object... args) throws SQLException {
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
        return s;
    }
    public static List<Map<String,Object>> rows(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = prepare(c, sql, args); ResultSet r = s.executeQuery()) {
            List<Map<String,Object>> rows = new ArrayList<>();
            while (r.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) {
                    // Connector/J versions differ in DATETIME getObject() defaults.
                    Object value = switch (r.getMetaData().getColumnType(i)) {
                        case Types.TIMESTAMP -> r.getTimestamp(i);
                        case Types.DATE -> r.getDate(i);
                        case Types.TIME -> r.getTime(i);
                        default -> r.getObject(i);
                    };
                    row.put(r.getMetaData().getColumnLabel(i), value);
                }
                rows.add(row);
            }
            return rows;
        }
    }
    public static Map<String,Object> one(Connection c, String sql, Object... args) throws SQLException {
        List<Map<String,Object>> rows = rows(c, sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public static int update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = prepare(c, sql, args)) { return s.executeUpdate(); }
    }
    public static int insert(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = prepare(c, sql, args)) {
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
            throw new SQLException("No generated ID returned.");
        }
    }
    public static int id(Map<String,Object> r, String key) { return ((Number)r.get(key)).intValue(); }
    public static String str(Map<String,Object> r, String key) { return Objects.toString(r.get(key), ""); }
}
