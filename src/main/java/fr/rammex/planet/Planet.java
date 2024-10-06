package fr.rammex.planet;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import fr.rammex.planet.commands.PlanetCommand;
import fr.rammex.planet.data.DataManager;
import fr.rammex.planet.events.PlayerEventListener;
import fr.rammex.planet.events.UiClick;
import fr.rammex.planet.events.ZoneManager;
import fr.rammex.planet.placeholder.PlaceHolder;
import fr.rammex.planet.utils.MessagesConfig;
import fr.rammex.planet.utils.TimeDecrementTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public final class Planet extends JavaPlugin {

    public static Planet instance;

    public static String PREFIX;

    public static int BLOCKS_PER_TICK;

    private final Map<String, DataManager> databases = new HashMap<>();


    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        instance = this;



        new TimeDecrementTask(this).runTaskTimer(this, 0L, 20L);

        //initialize database
        initializeDatabase("planet", "CREATE TABLE IF NOT EXISTS player_data (" +
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
                ");");
        //save config.yml
        saveDefaultConfig();
        //config.yml
        getConfig().addDefault("Settings.Prefix", "&8[&6Planet&8] &7");
        getConfig().addDefault("Settings.BlocksPerTick", 500);
        getConfig().options().copyDefaults(true);
        saveConfig();
        //config.yml
        PREFIX = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Settings.Prefix"));
        BLOCKS_PER_TICK = getConfig().getInt("Settings.BlocksPerTick");
        //messages.yml
        MessagesConfig messagesConfig = new MessagesConfig();
        messagesConfig.loadMessages();
        //commands
        getCommand("planete").setExecutor(new PlanetCommand());
        //events
        getServer().getPluginManager().registerEvents(new ZoneManager(this), this);
        getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);
        getServer().getPluginManager().registerEvents(new UiClick(this), this);
        // schem et messages
        loadSchematicsFolder();
        messages();

        createAllHolograms();

        startUpdatingHolograms();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
            new PlaceHolder(this).register(); //
        }


    }

    @Override
    public void onDisable() {
    }

    private void loadSchematicsFolder() {
        File schematicsFolder = new File(getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            if (schematicsFolder.mkdirs()) {
                getLogger().info("Schematics folder created successfully.");
            } else {
                getLogger().warning("Failed to create schematics folder.");
            }
        } else {
            getLogger().info("Schematics folder already exists.");
        }
    }

    public void startUpdatingHolograms() {
        new BukkitRunnable() {
            @Override
            public void run() {
                PlanetCommand.updateHolograms();
            }
        }.runTaskTimer(Planet.instance, 0L, 20L); // 20 ticks = 1 second
    }


    private void messages(){
        getLogger().info("-------------------------");
        getLogger().info("Plugin planet enabled v1.0");
        getLogger().info("by .rammex");
        getLogger().info("-------------------------");
    }

    public void initializeDatabase(String databaseName, String createStatement) {
        DataManager db = new DataManager(databaseName, createStatement, this.getDataFolder());
        db.load();
        databases.put(databaseName, db);
    }

    public Map<String, DataManager> getDatabases() {
        return databases;
    }


    public DataManager getDatabase(String databaseName) {
        return getDatabases().get(databaseName);
    }

    public void createAllHolograms() {
        for (Map.Entry<String, DataManager> entry : databases.entrySet()) {
            DataManager db = entry.getValue();
            for (String uuid : db.getAllUUIDs()) {
                double x = db.getX(uuid)+Planet.instance.getConfig().getDouble("holo.incrx");
                double y = db.getY(uuid)+Planet.instance.getConfig().getDouble("holo.incry");
                double z = db.getZ(uuid)+Planet.instance.getConfig().getDouble("holo.incrz");
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

                    Hologram hologram = DHAPI.createHologram("hologram_" + uuid, loc, false, lines);
                }
            }
        }
    }
}
