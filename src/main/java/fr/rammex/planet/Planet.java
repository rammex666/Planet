package fr.rammex.planet;

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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public final class Planet extends JavaPlugin {

    public static Planet instance;

    public static String PREFIX;

    public static int BLOCKS_PER_TICK;

    private final Map<String, DataManager> databases = new HashMap<>();

    private List<ArmorStand> armorStands = new ArrayList<>();

    @Override
    public void onEnable() {
        removeArmorStands();
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
        getCommand("planet").setExecutor(new PlanetCommand());
        //events
        getServer().getPluginManager().registerEvents(new ZoneManager(this), this);
        getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);
        getServer().getPluginManager().registerEvents(new UiClick(this), this);
        // schem et messages
        loadSchematicsFolder();
        messages();


        // Créer de nouveaux ArmorStand
        spawnArmorStands();

        PlanetCommand planetCommand = new PlanetCommand();
        startArmorStandLoop();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
            new PlaceHolder(this).register(); //
        }


    }

    @Override
    public void onDisable() {
        removeArmorStands();
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


    private void spawnArmorStands() {
        DataManager db = getDatabase("planet");
        if (db != null) {
            for (String uuid : db.getAllUUIDs()) {
                if (uuid == null) continue;

                double x = db.getX(uuid);
                double y = db.getY(uuid);
                double z = db.getZ(uuid);
                String worldName = db.getWorld(uuid);
                if (worldName == null) continue;

                Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                if (loc.getWorld() == null){
                    System.out.println("World not found");
                    continue;
                };

                int days = db.getDays(UUID.fromString(uuid));
                String hologram = MessagesConfig.getHologram("hologram.day");
                if (hologram == null){
                    System.out.println("hologrammess not found");
                    continue;
                };

                ArmorStand armorStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
                if (days > 20) {
                    armorStand.setCustomName(hologram.replace("%days%", "§a" + days));
                } else if (days < 20 && days > 10) {
                    armorStand.setCustomName(hologram.replace("%days%", "§2" + days));
                } else if (days < 10 && days > 5) {
                    armorStand.setCustomName(hologram.replace("%days%", "§e" + days));
                } else if (days < 5 && days > 1) {
                    armorStand.setCustomName(hologram.replace("%days%", "§6" + days));
                } else {
                    armorStand.setCustomName(hologram.replace("%days%", "§4" + days));
                }
                armorStand.setCustomNameVisible(true);
                armorStand.setGravity(false);
                armorStand.setVisible(false);

                armorStands.add(armorStand);
            }
        } else {
            getLogger().warning("Database 'planet' not found.");
        }
    }

    private void removeArmorStands() {
        for (ArmorStand armorStand : armorStands) {
            armorStand.remove();
        }
        armorStands.clear();
    }

    public List<ArmorStand> getArmorStands() {
        return armorStands;
    }

    public void startArmorStandLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                String hologram = MessagesConfig.getHologram("hologram.day");
                DataManager db = Planet.instance.getDatabase("planet");
                if (db != null) {
                    for (ArmorStand armorStand : Planet.instance.getArmorStands()) {
                        UUID playerUUID = PlanetCommand.armorStandToPlayerUUID.get(armorStand.getUniqueId());
                        if (playerUUID != null) {
                            int dayleft = db.getDays(UUID.fromString(playerUUID.toString()));
                            if (dayleft > 20) {
                                armorStand.setCustomName(hologram.replace("%days%", "§a" + dayleft));
                            } else if (dayleft < 20 && dayleft > 10) {
                                armorStand.setCustomName(hologram.replace("%days%", "§2" + dayleft));
                            } else if (dayleft < 10 && dayleft > 5) {
                                armorStand.setCustomName(hologram.replace("%days%", "§e" + dayleft));
                            } else if (dayleft < 5 && dayleft > 1) {
                                armorStand.setCustomName(hologram.replace("%days%", "§6" + dayleft));
                            } else {
                                armorStand.setCustomName(hologram.replace("%days%", "§4" + dayleft));
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Planet.instance, 0L, 20L); // 5 minutes
    }


}
