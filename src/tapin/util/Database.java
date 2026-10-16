package tapin.util;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** One connection per operation; credentials stay outside the application JAR. */
public final class Database {
    private Database() {}
    public static Connection open() throws SQLException {
        Properties p = new Properties();
        Path file = Path.of(System.getProperty("tapin.config", "config/database.properties"));
        try (InputStream in = Files.newInputStream(file)) { p.load(in); }
        catch (IOException e) { throw new SQLException("Cannot read " + file + ". Run from the TapIn folder.", e); }
        return DriverManager.getConnection(p.getProperty("db.url"), p.getProperty("db.user"), p.getProperty("db.password", ""));
    }
    @FunctionalInterface public interface Work<T> { T run(Connection c) throws SQLException; }
    public static <T> T transaction(Work<T> work) throws SQLException {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try { T result = work.run(c); c.commit(); return result; }
            catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }
}
