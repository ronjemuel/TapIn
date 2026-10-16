package tapin;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;
import tapin.util.Database;

/** Creates a new isolated schema. Never drops or resets an existing database. */
public final class TestRunner {
    public static void main(String[] args)throws Exception {
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(Path.of(System.getProperty("tapin.config","config/database.properties")))){p.load(in);}
        String schema="tapin_test_"+System.currentTimeMillis();
        try(Connection c=Database.open();Statement statement=c.createStatement()){
            statement.executeUpdate("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        URI base=URI.create(p.getProperty("db.url").substring("jdbc:".length()));
        URI testUrl=new URI(base.getScheme(),base.getUserInfo(),base.getHost(),base.getPort(),"/"+schema,base.getQuery(),null);
        p.setProperty("db.url","jdbc:"+testUrl);
        Path config=Path.of("build/integration.properties");Files.createDirectories(config.getParent());
        try(OutputStream out=Files.newOutputStream(config)){p.store(out,"Generated isolated test configuration");}
        System.setProperty("tapin.config",config.toString());
        try(Connection c=Database.open();Statement statement=c.createStatement()){
            if(!schema.equals(c.getCatalog()))throw new IllegalStateException("Unexpected test database.");
            String sql=Files.readString(Path.of("sql/setup.sql")).replaceAll("(?m)^--.*$","");
            for(String part:sql.split(";")){
                String command=part.trim();
                if(command.isEmpty()||command.startsWith("CREATE DATABASE")||command.startsWith("USE "))continue;
                statement.executeUpdate(command);
            }
        }
        System.out.println("Isolated schema: "+schema+" (retained for inspection)");
        IntegrationTest.main(new String[0]);
    }
}
