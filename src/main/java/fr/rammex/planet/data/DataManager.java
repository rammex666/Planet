package fr.rammex.planet.data;

import fr.rammex.planet.Planet;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;


public class DataManager {

    private final String dbname;



    private Connection connection;

    private String createTestTable = "CREATE TABLE IF NOT EXISTS test (" + "`test` varchar(32) NOT NULL,"
            + "PRIMARY KEY (`test`)" + ");";

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
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM test");
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
}