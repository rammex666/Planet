package fr.rammex.planet.placeholder;

import fr.rammex.planet.Planet;
import fr.rammex.planet.data.DataManager;
import fr.rammex.planet.events.ZoneManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class PlaceHolder extends PlaceholderExpansion {
    private final Planet plugin;

    public PlaceHolder(Planet planet) {
        this.plugin = planet;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return ".rammex";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "planet";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    @Nullable
    public String onPlaceholderRequest(@Nullable Player player, @NotNull String identifier) {
        if (player == null) {
            return null;
        }

        if (identifier.equals("x")) {
            UUID playerUUID = player.getUniqueId();
            DataManager db = plugin.getDatabase("planet");
            if (db != null) {
                return String.valueOf(db.getX(playerUUID.toString()));
            }
        }

        if (identifier.equals("y")) {
            UUID playerUUID = player.getUniqueId();
            DataManager db = plugin.getDatabase("planet");
            if (db != null) {
                return String.valueOf(db.getY(playerUUID.toString()));
            }
        }

        if (identifier.equals("z")) {
            UUID playerUUID = player.getUniqueId();
            DataManager db = plugin.getDatabase("planet");
            if (db != null) {
                return String.valueOf(db.getZ(playerUUID.toString()));
            }
        }

        if (identifier.equals("days")) {
            return getPlayerDays(player);
        }

        if (identifier.equals("schem")){
            UUID playerUUID = player.getUniqueId();
            DataManager db = plugin.getDatabase("planet");
            if (db != null) {
                String schematic = db.getSchematic(playerUUID.toString());
                return schematic;
            }
        }

        if (identifier.equals("player")) {
            Location loc = player.getLocation();
            String playerName = getPlayerAtLoc(loc);

            if (playerName != null) {
                return playerName;
            } else {
                return "Aucun joueur";
            }
        }

        return null;
    }



    public static String getPlayerDays(Player player) {
        UUID playerUUID = player.getUniqueId();
        DataManager db = Planet.instance.getDatabase("planet");

        if (db != null) {
            int totalSeconds = db.getSeconds(playerUUID.toString());
            int days = totalSeconds / (24 * 60 * 60);
            int hours = (totalSeconds % (24 * 60 * 60)) / (60 * 60);
            int minutes = (totalSeconds % (60 * 60)) / 60;

            return String.format("%d:%02d:%02d", days, hours, minutes);
        }
        return "Aucune";
    }

    private String getPlayerAtLoc(Location loc) {
        for (Map.Entry<String, DataManager> entry : plugin.getDatabases().entrySet()) {
            DataManager db = entry.getValue();
            for (String uuid : db.getAllUUIDs()) {
                double x = db.getX(uuid);
                double y = db.getY(uuid);
                double z = db.getZ(uuid);
                String worldName = db.getWorld(uuid);
                String playerName = db.getPlayerName(uuid);

                if (loc.getWorld().getName().equals(worldName) && ZoneManager.isInProtectedZone(loc, x, y, z)) {
                    return playerName;
                }
            }
        }
        return null;
    }
}