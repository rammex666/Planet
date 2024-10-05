package fr.rammex.planet.data;

import fr.rammex.planet.Planet;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DataManager {

    private final String dbname;
    private final String customCreateString;
    private final File dataFolder;

    public DataManager(String databaseName, String createStatement, File folder) {
        dbname = databaseName;
        customCreateString = createStatement;
        dataFolder = folder;
    }

    public void initialize() {
        try (Connection connection = getSQLConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM player_data");
            ResultSet rs = ps.executeQuery();
            close(ps, rs);
        } catch (SQLException ex) {
            Planet.instance.getLogger().log(Level.SEVERE, "Unable to retrieve connection", ex);
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
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + folder);
        } catch (SQLException | ClassNotFoundException ex) {
            Planet.instance.getLogger().log(Level.SEVERE, "SQLite exception on initialize", ex);
        }
        return null;
    }

    public void load() {
        try (Connection connection = getSQLConnection()) {
            Statement s = connection.createStatement();
            String createTestTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                    "`player_name` VARCHAR(32) NOT NULL," +
                    "`uuid` VARCHAR(36) NOT NULL," +
                    "`schematic` TEXT," +
                    "`day` INT," +
                    "`start_date` DATE," +
                    "`x` DOUBLE," +
                    "`y` DOUBLE," +
                    "`z` DOUBLE," +
                    "`world` TEXT," +
                    "`armorloc` TEXT," +
                    "`seconds` INT," +
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
            if (ps != null) ps.close();
            if (rs != null) rs.close();
        } catch (SQLException ex) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to close database connection", ex);
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

    public Date getStartDate(String uuid) {
        String query = "SELECT start_date FROM player_data WHERE uuid = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDate("start_date");
                }
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to get start date", e);
        }
        return null;
    }

    public List<String> getAllUUIDs() {
        List<String> uuids = new ArrayList<>();
        String query = "SELECT uuid FROM player_data";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                uuids.add(rs.getString("uuid"));
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to get all UUIDs", e);
        }
        return uuids;
    }

    public void addDays(String uuid, int daysToAdd) {
        String query = "UPDATE player_data SET day = day + ? WHERE uuid = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, daysToAdd);
            ps.setString(2, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to add days", e);
        }
    }

    public double getX(String uuid) {
        String query = "SELECT x FROM player_data WHERE uuid = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("x");
                }
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to get x coordinate", e);
        }
        return 0.0;
    }

    public double getY(String uuid) {
        String query = "SELECT y FROM player_data WHERE uuid = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("y");
                }
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to get y coordinate", e);
        }
        return 0.0;
    }

    public double getZ(String uuid) {
        String query = "SELECT z FROM player_data WHERE uuid = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("z");
                }
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to get z coordinate", e);
        }
        return 0.0;
    }

    public String getWorld(String uuid) {
        String query = "SELECT world FROM player_data WHERE uuid = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("world");
                }
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to get world", e);
        }
        return null;
    }

    public int getDaysLeft(String loc) {
        String query = "SELECT day FROM player_data WHERE armorloc = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, loc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("day");
                }
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to get days left", e);
        }
        return 0;
    }

    public int getDays(UUID playerUUID) {
        int seconds = getSeconds(playerUUID.toString());
        return seconds / (24 * 60 * 60); // Convertir les secondes en jours
    }

    public String getPlayerName(String uuid) {
        String query = "SELECT player_name FROM player_data WHERE uuid = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("player_name");
                }
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to get player name", e);
        }
        return null;
    }

    public String getUUID(String playerName) {
        String query = "SELECT uuid FROM player_data WHERE player_name = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("uuid");
                }
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to get UUID", e);
        }
        return null;
    }

    public int getSeconds(String uuid) {
        String query = "SELECT seconds FROM player_data WHERE uuid = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("seconds");
                }
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to get seconds", e);
        }
        return 0;
    }

    public void setSeconds(String uuid, int seconds) {
        String query = "UPDATE player_data SET seconds = ? WHERE uuid = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, seconds);
            ps.setString(2, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to set seconds", e);
        }
    }

    public void deletePlayer(String uuid) {
        String query = "DELETE FROM player_data WHERE uuid = ?";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to delete player", e);
        }
    }

    public boolean isZoneOverlapping(double x, double y, double z, int radius) {
        String query = "SELECT x, y, z FROM player_data";
        try (Connection conn = getSQLConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double playerX = rs.getDouble("x");
                double playerY = rs.getDouble("y");
                double playerZ = rs.getDouble("z");

                double distance = Math.sqrt(Math.pow(playerX - x, 2) + Math.pow(playerY - y, 2) + Math.pow(playerZ - z, 2));
                if (distance < (radius)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            Planet.instance.getLogger().log(Level.SEVERE, "Failed to check zone overlap", e);
        }
        return false;
    }
}