package fr.rammex.planet.events;

import fr.rammex.planet.commands.PlanetCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEventListener implements Listener {

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        PlanetCommand.POSITIONS.remove(event.getPlayer());
    }

}
