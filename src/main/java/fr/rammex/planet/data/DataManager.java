package fr.rammex.planet.data;

import fr.rammex.planet.Planet;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;


public class DataManager {

    private final String dbname;



    private Connection connection;

    private final String customCreateString;

    private final File dataFolder;

    public DataManager(String databaseName, String createStatement, File folder) {
        dbname = databaseName;
        customCreateString = createStatement;
        dataFolder = folder;
    }

    public void initialize() {
        connection = getSQLConnection();
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM player_data");
            ResultSet rs = ps.executeQuery();
            close(ps, rs);

        } catch (SQLException ex) {
            Planet.instance.getLogger().log(Level.SEVERE, "Unable to retreive connection", ex);
        }
    }

    public Connection getSQLConnection() {
        File folder = new File(dataFolder, dbname + ".db");
        if (!folder.exists()) {
            try {
                folder.createNewFile();
            } catch (IOException e) {
                Planet.instance.getLogger().log(Level.SEVERE, "File write error: " + dbname + ".db");
            }
        }
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + folder);
            return connection;
        } catch (SQLException ex) {
            Planet.instance.getLogger().log(Level.SEVERE, "SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            Planet.instance.getLogger().log(Level.SEVERE,
                    "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }
        return null;
    }

    public void load() {
        connection = getSQLConnection();
        try {
            Statement s = connection.createStatement();
            String createTestTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                    "`player_name` VARCHAR(32) NOT NULL," +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`schematic` TEXT," +
                    "`day` INT," +
                    "`x` DOUBLE," +
                    "`y` DOUBLE," +
                    "`z` DOUBLE," +
                    "PRIMARY KEY (`uuid`)" +
                    ");";
            s.executeUpdate(createTestTable);
            s.executeUpdate(customCreateString);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initialize();
    }

    public void close(PreparedStatement ps, ResultSet rs) {
        try {
            if (ps != null)
                ps.close();
            if (rs != null)
                rs.close();
        } catch (SQLException ex) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to close database connection", ex);
        }
    }

    public void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to close database connection", e);
        }
    }

    public void insertData(String query, Object... params) {
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to insert data", e);
        }
    }

    public boolean isUUIDInDatabase(String uuid) {
        String query = "SELECT COUNT(*) FROM player_data WHERE uuid = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to check UUID in database", e);
        }
        return false;
    }
}