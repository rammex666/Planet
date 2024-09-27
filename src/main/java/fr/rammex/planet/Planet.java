package fr.rammex.planet;

import fr.rammex.planet.commands.PlanetCommand;
import fr.rammex.planet.data.DataManager;
import fr.rammex.planet.events.PlayerEventListener;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class Planet extends JavaPlugin {

    public static Planet instance;

    public static String PREFIX;

    public static int BLOCKS_PER_TICK;

    private final Map<String, DataManager> databases = new HashMap<>();




    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        instance = this;

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
                "PRIMARY KEY (`uuid`)" +
                ");");

        saveDefaultConfig();


        getConfig().addDefault("Settings.Prefix", "&8[&6Planet&8] &7");
        getConfig().addDefault("Settings.BlocksPerTick", 500);
        getConfig().options().copyDefaults(true);
        saveConfig();

        PREFIX = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Settings.Prefix"));
        BLOCKS_PER_TICK = getConfig().getInt("Settings.BlocksPerTick");

        getCommand("planet").setExecutor(new PlanetCommand());

        getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);

        loadSchematicsFolder();
        messages();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (DataManager db : getDatabases().values()) {
                    for (String uuid : db.getAllUUIDs()) {
                        db.decrementDays(uuid);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L * 60 * 5);
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



}
