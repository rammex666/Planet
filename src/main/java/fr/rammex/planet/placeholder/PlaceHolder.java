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

        if (identifier.equals("coordinates")) {
            return getPlayerPlanetCoordinates(player);
        }

        if (identifier.equals("days")) {
            return getPlayerDays(player);
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

    private String getPlayerPlanetCoordinates(Player player) {
        UUID playerUUID = player.getUniqueId();
        DataManager db = plugin.getDatabase("planet");

        if (db != null) {
            double x = db.getX(playerUUID.toString());
            double y = db.getY(playerUUID.toString());
            double z = db.getZ(playerUUID.toString());
            String world = db.getWorld(playerUUID.toString());

            return String.format("World: %s, X: %.2f, Y: %.2f, Z: %.2f", world, x, y, z);
        }

        return "Aucune Planet";
    }

    private String getPlayerDays(Player player) {
        UUID playerUUID = player.getUniqueId();
        DataManager db = plugin.getDatabase("planet");

        if (db != null) {
            int days = db.getDays(UUID.fromString(playerUUID.toString()));

            return String.format("Jours restant: %d", days);
        }
        return "Aucune Planet";
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