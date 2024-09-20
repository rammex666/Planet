package fr.rammex.planet.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlanetCommand implements CommandExecutor {

    public static final Map<Player, Positions> POSITIONS = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Planet.PREFIX + "This command is for players only");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 2 && args[0].equalsIgnoreCase("paste")) {

            if (!player.hasPermission("Planet.paste")) {
                player.sendMessage(Planet.PREFIX + "No permission");
                return true;
            }

            File file = new File(Planet.instance.getDataFolder()+"/schematics", args[1].replace(".schematic", "") + ".schematic");
            if (!file.exists()) {
                player.sendMessage(Planet.PREFIX + "Schematic not found");
                return true;
            }

            player.sendMessage(Planet.PREFIX + "Starting to paste schematic...");
            try {
                Schematic schematic = new Schematic(file);
                schematic.paste(player.getLocation(), Planet.BLOCKS_PER_TICK, (Long time) -> {
                    player.sendMessage(Planet.PREFIX + "Schematic was pasted in " + (time / 1000F) + " seconds");
                });
            } catch (IOException ex) {
                player.sendMessage(Planet.PREFIX + "An error occured while pasting");
                ex.printStackTrace();
            }

        } else {
            player.sendMessage(Planet.PREFIX + "Usage:");
            player.sendMessage(Planet.PREFIX + "/" + label + " paste <schematic>");
        }
        return true;
    }

    private void spawnArmorStand(Player player, World world, double x, double y, double z, float yaw, float pitch, String name) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        EntityArmorStand armorStand = new EntityArmorStand(worldServer, x, y, z);
        armorStand.setSmall(true);
        armorStand.setCustomName(name);
        armorStand.setCustomNameVisible(true);

        connection.sendPacket(new PacketPlayOutSpawnEntityLiving(armorStand));
        connection.sendPacket(new PacketPlayOutEntityTeleport(armorStand.getId(), convertDouble(x), convertDouble(y), convertDouble(z), convertFloat(yaw), convertFloat(pitch), false));
        connection.sendPacket(new PacketPlayOutEntityEquipment(armorStand.getId(), 4, CraftItemStack.asNMSCopy(new ItemStack(Material.SKULL_ITEM, 1, (byte) 3))));
    }

    private int convertDouble(double d) {
        return (int) (d * 32.0D);
    }

    private byte convertFloat(float f) {
        return (byte) ((int) (f * 256.0F / 360.0F));
    }
}