package fr.rammex.planet;

import fr.rammex.planet.commands.PlanetCommand;
import fr.rammex.planet.events.PlayerEventListener;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Planet extends JavaPlugin {

    public static Planet instance;

    public static String PREFIX;

    public static int BLOCKS_PER_TICK;

    @Override
    public void onEnable() {

        instance = this;

        getConfig().addDefault("Settings.Prefix", "&8[&6SchematicAPI&8] &7");
        getConfig().addDefault("Settings.BlocksPerTick", 500);
        getConfig().options().copyDefaults(true);
        saveConfig();

        PREFIX = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Settings.Prefix"));
        BLOCKS_PER_TICK = getConfig().getInt("Settings.BlocksPerTick");

        getCommand("planet").setExecutor(new PlanetCommand());

        getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);

        loadSchematicsFolder();
        messages();
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
}
