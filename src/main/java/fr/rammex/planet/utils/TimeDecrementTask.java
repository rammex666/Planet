package fr.rammex.planet.utils;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import fr.rammex.planet.Planet;
import fr.rammex.planet.commands.PlanetCommand;
import fr.rammex.planet.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;


public class TimeDecrementTask extends BukkitRunnable {

    private final Planet plugin;

    public TimeDecrementTask(Planet plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        DataManager db = plugin.getDatabase("planet");
        if (db != null) {
            List<String> uuids = db.getAllUUIDs();
            for (String uuidStr : uuids) {
                UUID playerUUID = UUID.fromString(uuidStr);
                int secondsLeft = db.getSeconds(playerUUID.toString());
                if (secondsLeft > 0) {
                    db.setSeconds(playerUUID.toString(), secondsLeft - 1);
                } else {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null) {
                        Hologram holo = DHAPI.getHologram("hologram_" + player.getUniqueId());
                        if (holo != null) {
                            holo.destroy();
                        }
                    }
                    double x = db.getX(playerUUID.toString());
                    double y = db.getY(playerUUID.toString());
                    double z = db.getZ(playerUUID.toString());
                    String worldName = db.getWorld(playerUUID.toString());
                    if (worldName != null) {
                        Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                        int radius = Planet.instance.getConfig().getInt("Settings.ProtectedZoneRadius");
                        PlanetCommand.deleteArea(location, radius);
                    }
                    db.deletePlayer(playerUUID.toString());
                }
            }
        }
    }
}