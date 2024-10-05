package fr.rammex.planet.events;

import fr.rammex.planet.Planet;
import fr.rammex.planet.data.DataManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import fr.rammex.planet.utils.MessagesConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ZoneManager implements Listener {
    Planet plugin;

    public ZoneManager(Planet plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        handleBlockEvent(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockEvent(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            handleBlockEvent(event.getPlayer(), event.getClickedBlock().getLocation(), event);
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Location playerLocation = player.getLocation();
        handleCommandEvent(player, playerLocation, event);
    }

    private void handleBlockEvent(Player player, Location blockLocation, Cancellable event) {
        UUID playerUUID = player.getUniqueId();

        for (Map.Entry<String, DataManager> entry : Planet.instance.getDatabases().entrySet()) {
            DataManager db = entry.getValue();
            for (String uuid : db.getAllUUIDs()) {
                double x = db.getX(uuid);
                double y = db.getY(uuid);
                double z = db.getZ(uuid);
                String worldName = db.getWorld(uuid);

                if (blockLocation.getWorld().getName().equals(worldName) && isInProtectedZone(blockLocation, x, y, z)) {
                    if (!player.hasPermission("Planet.bypassbuild") && !playerUUID.toString().equals(uuid)) {
                        player.sendMessage((MessagesConfig.getMessage("interact.no-permission", player)));
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    private void handleCommandEvent(Player player, Location playerLocation, Cancellable event) {
        if (!(event instanceof PlayerCommandPreprocessEvent)) {
            return;
        }

        PlayerCommandPreprocessEvent commandEvent = (PlayerCommandPreprocessEvent) event;
        UUID playerUUID = player.getUniqueId();
        String message = commandEvent.getMessage().toLowerCase();
        List<String> blockedCommands = Planet.instance.getConfig().getStringList("Settings.BlockedCommands");

        for (Map.Entry<String, DataManager> entry : Planet.instance.getDatabases().entrySet()) {
            DataManager db = entry.getValue();
            for (String uuid : db.getAllUUIDs()) {
                double x = db.getX(uuid);
                double y = db.getY(uuid);
                double z = db.getZ(uuid);
                String worldName = db.getWorld(uuid);

                if (playerLocation.getWorld().getName().equals(worldName) && isInProtectedZone(playerLocation, x, y, z)) {
                    if (blockedCommands.contains(message.split(" ")[0]) && !player.hasPermission("Planet.bypasscommand") && !playerUUID.toString().equals(uuid)) {
                        player.sendMessage(MessagesConfig.getMessage("command.no-permission", player));
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    public static boolean isInProtectedZone(Location blockLocation, double x, double y, double z) {
        double radius = Planet.instance.getConfig().getDouble("Settings.ProtectedZoneRadius");
        Location center = new Location(blockLocation.getWorld(), x, y, z);
        return blockLocation.distance(center) <= radius;
    }
}