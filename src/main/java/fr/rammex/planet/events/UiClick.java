package fr.rammex.planet.events;

import fr.rammex.planet.Planet;
import fr.rammex.planet.commands.PlanetCommand;
import fr.rammex.planet.data.DataManager;
import fr.rammex.planet.utils.MessagesConfig;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class UiClick implements Listener {
    Planet plugin;
    public UiClick(Planet plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title.startsWith("Planets - Page")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || meta.getLore() == null) {
                return;
            }

            // Gestion des boutons de navigation
            if (clickedItem.getType() == Material.ARROW) {
                int currentPage = Integer.parseInt(title.split(" ")[2]) - 1;
                if (meta.getDisplayName().contains("Next Page")) {
                    PlanetCommand.openMenu(player, currentPage + 1);
                } else if (meta.getDisplayName().contains("Previous Page")) {
                    PlanetCommand.openMenu(player, currentPage - 1);
                }
                return;
            }

            // Gestion de la téléportation
            String playerName = ChatColor.stripColor(meta.getDisplayName().split("'s Planet")[0]);
            DataManager db = plugin.getDatabase("planet");
            if (db == null) {
                player.sendMessage(MessagesConfig.getMessage("error.database-not-found", player));
                return;
            }

            UUID playerUUID = UUID.fromString(db.getUUID(playerName));
            if (playerUUID == null) {
                player.sendMessage(MessagesConfig.getMessage("error.player-not-found", player));
                return;
            }

            double x = db.getX(playerUUID.toString());
            double y = db.getY(playerUUID.toString());
            double z = db.getZ(playerUUID.toString());
            String worldName = db.getWorld(playerUUID.toString());

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage(MessagesConfig.getMessage("error.world-not-found", player));
                return;
            }

            player.teleport(new Location(world, x, y, z));
            player.sendMessage(MessagesConfig.getMessage("command.teleported", player));
        }
    }
}
