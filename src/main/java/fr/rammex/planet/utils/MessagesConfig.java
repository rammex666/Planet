package fr.rammex.planet.utils;

import fr.rammex.planet.Planet;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MessagesConfig {

    private static FileConfiguration messagesConf;
    private File file;

    private void loadFile(String fileName, String folder) {
        if(folder == null){
            file = new File(Planet.instance.getDataFolder(), fileName + ".yml");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                Planet.instance.saveResource(fileName + ".yml", false);
            }
        } else {
            file = new File(Planet.instance.getDataFolder() + "/" + folder, fileName + ".yml");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                Planet.instance.saveResource(folder+"/"+fileName + ".yml", false);
            }
        }

        FileConfiguration fileConf = new YamlConfiguration();
        try {
            fileConf.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        switch (fileName) {
            case "messages":
                messagesConf = fileConf;
                break;
        }
    }

    public FileConfiguration getMessagesConf() {
        return messagesConf;
    }

    public void loadMessages() {
        loadFile("messages", null);
    }

    public static String getMessage(String key, Player player) {
        String message = messagesConf.getString(key);
        if (message != null) {
            message = message.replace("&", "§")
                    .replace("%prefix%", Planet.PREFIX)
                    .replace("%player%", player.getName());
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }

    public static List<String> getHologram(String key){
        List<String>  hologram = messagesConf.getStringList(key);
        for (int i = 0; i < hologram.size(); i++) {
            hologram.set(i, hologram.get(i).toString().replace("&", "§"));
        }
        return hologram;
    }


}
