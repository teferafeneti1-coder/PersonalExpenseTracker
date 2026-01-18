package personalexpensetracker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/javadb";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1431";   // ← CHANGE TO YOUR ACTUAL PASSWORD

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}