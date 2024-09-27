package fr.rammex.planet.commands;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import fr.rammex.planet.Planet;
import fr.rammex.planet.object.Schematic;
import fr.rammex.planet.object.SchematicLocation;
import fr.rammex.planet.utils.Positions;
import fr.rammex.planet.utils.Vector;
import fr.rammex.planet.utils.Region;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class PlanetCommand implements CommandExecutor {

    static int cooldown = Planet.instance.getConfig().getInt("Command.Tp.cooldown");

    public static final Map<Player, Positions> POSITIONS = new HashMap<>();
    private ArmorStand armorStand;
    private final Map<UUID, Long> tpCooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = cooldown * 1000L;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Planet.PREFIX + "This command is for players only");
            return true;
        }
        Player player = (Player) sender;

        if (args[0].equalsIgnoreCase("tp")) {
            if (!player.hasPermission("Planet.tp")) {
                player.sendMessage(Planet.PREFIX + "No permission");
                return true;
            }

            UUID playerUUID = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            if (tpCooldowns.containsKey(playerUUID)) {
                long lastUsed = tpCooldowns.get(playerUUID);
                if (currentTime - lastUsed < COOLDOWN_TIME) {
                    long timeLeft = (COOLDOWN_TIME - (currentTime - lastUsed)) / 1000;
                    player.sendMessage(Planet.PREFIX + "You must wait " + timeLeft + " seconds before using this command again.");
                    return true;
                }
            }

            if (!Planet.instance.getDatabase("planet").isUUIDInDatabase(playerUUID.toString())) {
                player.sendMessage(Planet.PREFIX + "You don't have a planet !");
                return true;
            } else {
                double x = Planet.instance.getDatabase("planet").getX(playerUUID.toString());
                double y = Planet.instance.getDatabase("planet").getY(playerUUID.toString());
                double z = Planet.instance.getDatabase("planet").getZ(playerUUID.toString());
                String worldName = Planet.instance.getDatabase("planet").getWorld(playerUUID.toString());

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    player.sendMessage(Planet.PREFIX + "World not found");
                    return true;
                }
                player.teleport(new Location(world, x, y, z));
                player.sendMessage(Planet.PREFIX + "Teleported to your planet !");
                tpCooldowns.put(playerUUID, currentTime);
            }
        }

        if (args[0].equalsIgnoreCase("addays")) {
            String playerName = args[1];
            String days = args[2];

            if (!isNumeric(days)) {
                player.sendMessage(Planet.PREFIX + "Days must be a number");
                return true;
            }
            if(!player.hasPermission("Planet.addays")) {
                player.sendMessage(Planet.PREFIX + "No permission");
                return true;
            }

            UUID playerUUID = getPlayerUUID(playerName);
            if (playerUUID == null) {
                player.sendMessage(Planet.PREFIX + "Player not found");
                return true;
            }

            if (!Planet.instance.getDatabase("planet").isUUIDInDatabase(playerUUID.toString())) {
                player.sendMessage(Planet.PREFIX + "Player data not found in the database");
                return true;
            } else {
                Planet.instance.getDatabase("planet").addDays(playerUUID.toString(), Integer.parseInt(days));
            }

        }

        if (args[0].equalsIgnoreCase("createplanet")) {
            String schematicName = args[1];
            String playerName = args[2];
            String days = args[3];
            String worldName = player.getWorld().getName();

            if (!isNumeric(days)) {
                player.sendMessage(Planet.PREFIX + "Days must be a number");
                return true;
            }

            if (!player.hasPermission("Planet.paste")) {
                player.sendMessage(Planet.PREFIX + "No permission");
                return true;
            }

            File file = new File(Planet.instance.getDataFolder() + "/schematics", schematicName + ".schematic");
            if (!file.exists()) {
                player.sendMessage(Planet.PREFIX + "Schematic not found");
                return true;
            }

            UUID playerUUID = getPlayerUUID(playerName);
            if (playerUUID == null) {
                player.sendMessage(Planet.PREFIX + "Player not found");
                return true;
            }

            if (Planet.instance.getDatabase("planet").isUUIDInDatabase(playerUUID.toString())) {
                player.sendMessage(Planet.PREFIX + "Player data already exists in the database");
                return true;
            }

            player.sendMessage(Planet.PREFIX + "Starting to paste schematic...");
            try {
                Schematic schematic = new Schematic(file);
                schematic.paste(player.getLocation(), Planet.BLOCKS_PER_TICK, (Long time) -> {
                    player.sendMessage(Planet.PREFIX + "Schematic was pasted in " + (time / 1000F) + " seconds");


                    String query = "INSERT INTO player_data (player_name, uuid, schematic, day, start_date, x, y, z, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    Planet.instance.getDatabase("planet").insertData(query, playerName, playerUUID.toString(), schematicName, Integer.parseInt(days), Date.valueOf(LocalDate.now()), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), worldName);
                });
            } catch (IOException ex) {
                player.sendMessage(Planet.PREFIX + "An error occurred while pasting");
                ex.printStackTrace();
            }

        } else {
            player.sendMessage(Planet.PREFIX + "Commande inconnue");
        }
        return true;
    }

    public boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public UUID getPlayerUUID(String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        return offlinePlayer != null ? offlinePlayer.getUniqueId() : null;
    }

    public void spawnArmorStand(Location location) {
        armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setCustomName("Loading...");
        armorStand.setCustomNameVisible(true);
        armorStand.setGravity(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (armorStand != null && !armorStand.isDead()) {
                    int dayleft = Planet.instance.getDatabase("planet").getDaysLeft(String.valueOf(armorStand.getLocation()));
                    if (dayleft < 30 && dayleft > 20) {
                        armorStand.setCustomName("§a" + dayleft + " §7days left");
                    } else if (dayleft < 20 && dayleft > 10) {
                        armorStand.setCustomName("§2" + dayleft + " §7days left");
                    } else if (dayleft < 10 && dayleft > 5) {
                        armorStand.setCustomName("§e" + dayleft + " §7days left");
                    } else if (dayleft < 5 && dayleft > 1) {
                        armorStand.setCustomName("§6" + dayleft + " §7days left");
                    } else {
                        armorStand.setCustomName("§4" + dayleft + " §7days left");
                    }
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(Planet.instance, 0L, 20L * 60 * 5); // 5 minutes
    }
}