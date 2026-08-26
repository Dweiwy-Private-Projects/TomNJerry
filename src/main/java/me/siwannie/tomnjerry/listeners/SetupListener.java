package me.siwannie.tomnjerry.listeners;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.SetupMode;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SetupListener implements Listener {

    private final TomNJerry plugin;
    private final MiniMessage mm;

    public SetupListener(TomNJerry plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
    }

    @EventHandler
    public void onSetupInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getSetupManager().isInSetupMode(player) || event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        event.setCancelled(true);

        SetupMode mode = plugin.getSetupManager().getSetupMode(player);
        Location spawnLoc = block.getLocation().clone().add(0.5, 1.0, 0.5);

        if (mode == SetupMode.ADD || mode == SetupMode.REMOVE) {
            if (block.getType() != Material.PLAYER_HEAD && block.getType() != Material.PLAYER_WALL_HEAD) {
                player.sendMessage(mm.deserialize("<red>You must click a Player Head!</red>"));
                return;
            }
            if (mode == SetupMode.ADD) {
                if (plugin.getConfigManager().addCheeseLocation(block.getLocation())) player.sendMessage(mm.deserialize("<green>Cheese location added!</green>"));
                else player.sendMessage(mm.deserialize("<red>This location is already registered.</red>"));
            } else {
                if (plugin.getConfigManager().removeCheeseLocation(block.getLocation())) player.sendMessage(mm.deserialize("<green>Cheese location removed!</green>"));
                else player.sendMessage(mm.deserialize("<red>This location was not registered.</red>"));
            }
        } else if (mode == SetupMode.CAGE) {
            plugin.getConfigManager().setCageLocation(spawnLoc);
            player.sendMessage(mm.deserialize("<green>Cage location set!</green>"));
        } else if (mode == SetupMode.TOM_SPAWN) {
            plugin.getConfigManager().setTomLocation(spawnLoc);
            player.sendMessage(mm.deserialize("<green>Tom's spawn location set!</green>"));
        } else if (mode == SetupMode.JERRY_SPAWN) {
            plugin.getConfigManager().addJerryLocation(spawnLoc);
            player.sendMessage(mm.deserialize("<green>Random Jerry spawn added!</green>"));
        } else if (mode == SetupMode.GOLDEN_CHEESE) {
            plugin.getConfigManager().addGoldenCheeseLocation(spawnLoc);
            player.sendMessage(mm.deserialize("<gold>Golden Cheese spawn location set!</gold>"));
        }
    }
}