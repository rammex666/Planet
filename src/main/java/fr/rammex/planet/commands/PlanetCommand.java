package fr.rammex.planet.commands;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import fr.rammex.planet.Planet;
import fr.rammex.planet.data.DataManager;
import fr.rammex.planet.object.Schematic;
import fr.rammex.planet.placeholder.PlaceHolder;
import fr.rammex.planet.utils.MessagesConfig;
import fr.rammex.planet.utils.Positions;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static java.util.logging.Logger.getLogger;

public class PlanetCommand implements CommandExecutor {

    static int cooldown = Planet.instance.getConfig().getInt("Command.Tp.cooldown");

    public static final Map<Player, Positions> POSITIONS = new HashMap<>();
    private static final int ITEMS_PER_PAGE = 52;
    private ArmorStand armorStand;
    public static final Map<UUID, UUID> armorStandToPlayerUUID = new HashMap<>();
    private final Map<UUID, Long> tpCooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = cooldown * 1000L;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Planet.PREFIX + "This command is for players only");
            return true;
        }
        Player player = (Player) sender;

        if (args[0].equalsIgnoreCase("admin")) {
            if (player.hasPermission("Planet.staff")) {
                openMenu(player, 0); // Ouvrir la première page
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("home") || args[0].equalsIgnoreCase("tp")) {
            if (!player.hasPermission("Planet.tp")) {
                player.sendMessage(MessagesConfig.getMessage("error.no-permission", player));
                return true;
            }

            UUID playerUUID = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            if (tpCooldowns.containsKey(playerUUID)) {
                    long lastUsed = tpCooldowns.get(playerUUID);
                    if (currentTime - lastUsed < COOLDOWN_TIME) {
                        long timeLeft = (COOLDOWN_TIME - (currentTime - lastUsed)) / 1000;
                        player.sendMessage(MessagesConfig.getMessage("error.tp-cooldown", player).replace("%time%", String.valueOf(timeLeft)));
                        return true;
                    }

            }

            if (!Planet.instance.getDatabase("planet").isUUIDInDatabase(playerUUID.toString())) {
                player.sendMessage(MessagesConfig.getMessage("error.player-not-found", player));
                return true;
            } else {
                int secondeleft = Planet.instance.getDatabase("planet").getSeconds(playerUUID.toString());
                if (secondeleft <= 0) {
                    player.sendMessage(MessagesConfig.getMessage("error.no-days-left", player));
                    return true;
                }
                double x = Planet.instance.getDatabase("planet").getX(playerUUID.toString());
                double y = Planet.instance.getDatabase("planet").getY(playerUUID.toString());
                double z = Planet.instance.getDatabase("planet").getZ(playerUUID.toString());
                String worldName = Planet.instance.getDatabase("planet").getWorld(playerUUID.toString());

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    player.sendMessage(MessagesConfig.getMessage("error.world-not-found", player));
                    return true;
                }
                player.teleport(new Location(world, x, y, z));
                player.sendMessage(MessagesConfig.getMessage("command.teleported", player));
                if (!player.hasPermission("Planet.tp.bypass")) {
                    tpCooldowns.put(playerUUID, currentTime);
                }
            }
        }

        if (args[0].equalsIgnoreCase("addays")) {
            String playerName = args[1];
            String days = args[2];

            if (!isNumeric(days)) {
                player.sendMessage(MessagesConfig.getMessage("error.days-not-number", player));
                return true;
            }
            if(!player.hasPermission("Planet.addays")) {
                player.sendMessage(MessagesConfig.getMessage("error.no-permission", player));
                return true;
            }

            UUID playerUUID = getPlayerUUID(playerName);
            if (playerUUID == null) {
                player.sendMessage(MessagesConfig.getMessage("error.player-not-found", player));
                return true;
            }

            if (!Planet.instance.getDatabase("planet").isUUIDInDatabase(playerUUID.toString())) {
                player.sendMessage(MessagesConfig.getMessage("error.player-not-found", player));
                return true;
            } else {
                int seconds = Planet.instance.getDatabase("planet").getSeconds(playerUUID.toString());
                Planet.instance.getDatabase("planet").setSeconds(playerUUID.toString(), seconds + Integer.parseInt(days) * 24 * 60 * 60);
                Planet.instance.getDatabase("planet").addDays(playerUUID.toString(), Integer.parseInt(days));
                player.sendMessage(Planet.PREFIX + "Added " + days + " days to " + playerName + "'s planet");
            }

        }

        if (args[0].equalsIgnoreCase("removedays")) {
            String playerName = args[1];
            String days = args[2];

            if (!isNumeric(days)) {
                player.sendMessage(MessagesConfig.getMessage("error.days-not-number", player));
                return true;
            }
            if(!player.hasPermission("Planet.removedays")) {
                player.sendMessage(MessagesConfig.getMessage("error.no-permission", player));
                return true;
            }

            UUID playerUUID = getPlayerUUID(playerName);
            if (playerUUID == null) {
                player.sendMessage(MessagesConfig.getMessage("error.player-not-found", player));
                return true;
            }

            if (!Planet.instance.getDatabase("planet").isUUIDInDatabase(playerUUID.toString())) {
                player.sendMessage(MessagesConfig.getMessage("error.player-not-found", player));
                return true;
            } else {
                int seconds = Planet.instance.getDatabase("planet").getSeconds(playerUUID.toString());
                Planet.instance.getDatabase("planet").addDays(playerUUID.toString(), -Integer.parseInt(days));
                Planet.instance.getDatabase("planet").setSeconds(playerUUID.toString(), seconds - Integer.parseInt(days) * 24 * 60 * 60);
                int secondeleft = Planet.instance.getDatabase("planet").getSeconds(playerUUID.toString());
                if (secondeleft < 0) {

                    player.sendMessage(Planet.PREFIX + "Removed " + days + " days to " + playerName + "'s planet is now expired");
                } else {
                player.sendMessage(Planet.PREFIX + "Removed " + days + " days to " + playerName + "'s planet");
            }}

        }

        if (args[0].equalsIgnoreCase("create")) {
            String schematicName = args[1];
            String playerName = args[2];
            int days = Integer.parseInt(args[3]);
            String worldName = player.getWorld().getName();
            int seconds = days * 24 * 60 * 60;
            DataManager db = Planet.instance.getDatabase("planet");
            Player playert = Bukkit.getPlayer(playerName);
            if (playert == null) {
                player.sendMessage(MessagesConfig.getMessage("error.player-not-found", player));
                return true;
            }

            if (!isNumeric(String.valueOf(days))) {
                player.sendMessage(MessagesConfig.getMessage("error.days-not-number", player));
                return true;
            }

            if (!player.hasPermission("Planet.paste")) {
                player.sendMessage(MessagesConfig.getMessage("error.no-permission", player));
                return true;
            }

            File file = new File(Planet.instance.getDataFolder() + "/schematics", schematicName + ".schematic");
            if (!file.exists()) {
                player.sendMessage(MessagesConfig.getMessage("error.schematic-not-found", player));
                return true;
            }

            UUID playerUUID = getPlayerUUID(playerName);
            if (playerUUID == null) {
                player.sendMessage(MessagesConfig.getMessage("error.player-not-found", player));
                return true;
            }

            if (Planet.instance.getDatabase("planet").isUUIDInDatabase(playerUUID.toString())) {
                player.sendMessage(MessagesConfig.getMessage("error.player-already-exists", player));
                return true;
            }

            if(db.isZoneOverlapping(player.getLocation().getX(),player.getLocation().getY(),player.getLocation().getZ(), Planet.instance.getConfig().getInt("Settings.ProtectedZoneRadius"))) {
                player.sendMessage(MessagesConfig.getMessage("error.zone-overlapping", player));
                return true;
            }

            player.sendMessage(MessagesConfig.getMessage("schematic.pasting", player));
            try {
                Schematic schematic = new Schematic(file);
                schematic.paste(player.getLocation(), Planet.BLOCKS_PER_TICK, (Long time) -> {
                    player.sendMessage(Planet.PREFIX + "Schematic was pasted in " + (time / 1000F) + " seconds");
                    String query = "INSERT INTO player_data (player_name, uuid, schematic, day, start_date, x, y, z, world, armorloc, seconds) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    Planet.instance.getDatabase("planet").insertData(query,
                            playerName,
                            playerUUID.toString(),
                            schematicName,
                            days,
                            Date.valueOf(LocalDate.now()),
                            player.getLocation().getX(),
                            player.getLocation().getY(),
                            player.getLocation().getZ(),
                            worldName, player.getLocation(),
                            seconds);
                });

                double x = player.getLocation().getX()+Planet.instance.getConfig().getDouble("holo.incrx");
                double y = player.getLocation().getY()+Planet.instance.getConfig().getDouble("holo.incry");
                double z = player.getLocation().getZ()+Planet.instance.getConfig().getDouble("holo.incrz");

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location loc = new Location(world, x, y, z);

                    int hours = 0;
                    int minutes = 0;

                    String daysFormatted = String.format("%d:%02d:%02d", days, hours, minutes);

                    List<String> lines = MessagesConfig.getHologram("hologram.day");

                    if (days > 20) {
                        for (int i = 0; i < lines.size(); i++) {
                            lines.set(i, lines.get(i).toString().replace("%days%", "§a" + days)
                                    .replace("%daysf%", daysFormatted));
                        }
                    } else if (days < 20 && days > 10) {
                        for (int i = 0; i < lines.size(); i++) {
                            lines.set(i, lines.get(i).toString().replace("%days%", "§2" + days)
                                    .replace("%daysf%", daysFormatted));
                        }
                    } else if (days < 10 && days > 5) {
                        for (int i = 0; i < lines.size(); i++) {
                            lines.set(i, lines.get(i).toString().replace("%days%", "§e" + days)
                                    .replace("%daysf%", daysFormatted));
                        }
                    } else if (days < 5 && days > 1) {
                        for (int i = 0; i < lines.size(); i++) {
                            lines.set(i, lines.get(i).toString().replace("%days%", "§6" + days)
                                    .replace("%daysf%", daysFormatted));
                        }
                    } else {
                        for (int i = 0; i < lines.size(); i++) {
                            lines.set(i, lines.get(i).toString().replace("%days%", "§4" + days)
                                    .replace("%daysf%", daysFormatted));
                        }
                    }

                    Hologram hologram = DHAPI.createHologram("hologram_" + playerUUID, loc, false, lines);
                }
            } catch (IOException ex) {
                player.sendMessage(MessagesConfig.getMessage("error.schematic-not-found", player));
                ex.printStackTrace();
            }

        } else {
            return true;
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




    public static void openMenu(Player player, int page) {
        DataManager db = Planet.instance.getDatabase("planet");
        if (db == null) {
            player.sendMessage(MessagesConfig.getMessage("error.database-not-found", player));
            return;
        }

        List<String> uuids = db.getAllUUIDs();
        int totalPages = (int) Math.ceil((double) uuids.size() / ITEMS_PER_PAGE);

        Inventory menu = Bukkit.createInventory(null, 54, "Planets - Page " + (page + 1));

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, uuids.size());

        for (int i = start; i < end; i++) {
            String uuid = uuids.get(i);
            ItemStack item = createPlanetItem(uuid);
            if (item != null) {
                menu.addItem(item);
            }
        }

        // Ajouter les boutons de navigation
        if (page > 0) {
            ItemStack previousPage = new ItemStack(Material.ARROW);
            ItemMeta previousMeta = previousPage.getItemMeta();
            previousMeta.setDisplayName(ChatColor.GREEN + "Previous Page");
            previousPage.setItemMeta(previousMeta);
            menu.setItem(45, previousPage);
        }

        if (page < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
            nextPage.setItemMeta(nextMeta);
            menu.setItem(53, nextPage);
        }

        player.openInventory(menu);
    }

    private static ItemStack createPlanetItem(String uuid) {
        DataManager db = Planet.instance.getDatabase("planet");
        if (db == null) {
            return null;
        }

        String playerName = db.getPlayerName(uuid);
        double x = db.getX(uuid);
        double y = db.getY(uuid);
        double z = db.getZ(uuid);
        String world = db.getWorld(uuid);

        ItemStack item = new ItemStack(getRandomBlock());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + playerName + "'s Planet");
            List<String> lore = new ArrayList<>();
            String message = "%planet_days%";
            message = PlaceholderAPI.setPlaceholders(Bukkit.getPlayer(UUID.fromString(uuid)), message);
            lore.add(ChatColor.YELLOW + "Days left: " + message);
            lore.add(ChatColor.YELLOW + "Coordinates: ");
            lore.add(ChatColor.YELLOW + " * X : " + x);
            lore.add(ChatColor.YELLOW + " * Y : " + y);
            lore.add(ChatColor.YELLOW + " * Z : " + z);
            lore.add(ChatColor.YELLOW + "World: " + world);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static Material getRandomBlock() {
        Material[] blocks = {Material.DIAMOND_BLOCK, Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.EMERALD_BLOCK, Material.DIRT, Material.GRASS};
        Random random = new Random();
        return blocks[random.nextInt(blocks.length)];
    }

    public void setDays(UUID playerUUID, int days) {
        int seconds = days * 24 * 60 * 60;
        DataManager db = Planet.instance.getDatabase("planet");
        db.setSeconds(playerUUID.toString(), seconds);
    }

    public static void deleteArea(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        double centerX = center.getBlockX();
        double centerY = center.getBlockY();
        double centerZ = center.getBlockZ();

        for (double x = centerX - radius; x <= centerX + radius; x++) {
            for (double y = centerY - radius; y <= centerY + radius; y++) {
                for (double z = centerZ - radius; z <= centerZ + radius; z++) {
                    Location loc = new Location(world, x, y, z);
                    if (loc.getBlockY() >= 0 && loc.getBlockY() < world.getMaxHeight()) {
                        Block block = world.getBlockAt(loc);
                        if (block.getState() instanceof Chest) {
                            Chest chest = (Chest) block.getState();
                            chest.getInventory().clear();
                        }
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private static String getPlayerDays(Player player) {
        UUID playerUUID = player.getUniqueId();
        DataManager db = Planet.instance.getDatabase("planet");

        if (db != null) {
            int totalSeconds = db.getSeconds(String.valueOf(player));
            int days = totalSeconds / (24 * 60 * 60);
            int hours = (totalSeconds % (24 * 60 * 60)) / (60 * 60);
            int minutes = (totalSeconds % (60 * 60)) / 60;

            return String.format("%d:%02d:%02d", days, hours, minutes);
        }
        return "Aucune Planet";
    }

    public static void updateHolograms() {
        for (Map.Entry<String, DataManager> entry : Planet.instance.getDatabases().entrySet()) {
            DataManager db = entry.getValue();
            for (String uuid : db.getAllUUIDs()) {
                double x = db.getX(uuid) + Planet.instance.getConfig().getDouble("holo.incrx");
                double y = db.getY(uuid) + Planet.instance.getConfig().getDouble("holo.incry");
                double z = db.getZ(uuid) + Planet.instance.getConfig().getDouble("holo.incrz");
                String worldName = db.getWorld(uuid);
                String playerName = db.getPlayerName(uuid);

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location loc = new Location(world, x, y, z);

                    int totalSeconds = db.getSeconds(uuid);
                    int days = totalSeconds / (24 * 60 * 60);
                    int hours = (totalSeconds % (24 * 60 * 60)) / (60 * 60);
                    int minutes = (totalSeconds % (60 * 60)) / 60;

                    String daysFormatted = String.format("%d:%02d:%02d", days, hours, minutes);

                    List<String> lines = MessagesConfig.getHologram("hologram.day");

                    if (days > 20) {
                        for (int i = 0; i < lines.size(); i++) {
                            lines.set(i, lines.get(i).toString().replace("%days%", "§a" + days)
                                    .replace("%daysf%", daysFormatted));
                        }
                    } else if (days < 20 && days > 10) {
                        for (int i = 0; i < lines.size(); i++) {
                            lines.set(i, lines.get(i).toString().replace("%days%", "§2" + days)
                                    .replace("%daysf%", daysFormatted));
                        }
                    } else if (days < 10 && days > 5) {
                        for (int i = 0; i < lines.size(); i++) {
                            lines.set(i, lines.get(i).toString().replace("%days%", "§e" + days)
                                    .replace("%daysf%", daysFormatted));
                        }
                    } else if (days < 5 && days > 1) {
                        for (int i = 0; i < lines.size(); i++) {
                            lines.set(i, lines.get(i).toString().replace("%days%", "§6" + days)
                                    .replace("%daysf%", daysFormatted));
                        }
                    } else {
                        for (int i = 0; i < lines.size(); i++) {
                            lines.set(i, lines.get(i).toString().replace("%days%", "§4" + days)
                                    .replace("%daysf%", daysFormatted));
                        }
                    }

                    Hologram hologram = DHAPI.getHologram("hologram_" + uuid);
                    if (hologram != null) {
                        DHAPI.setHologramLines(hologram, lines);
                    }
                }
            }
        }
    }
}