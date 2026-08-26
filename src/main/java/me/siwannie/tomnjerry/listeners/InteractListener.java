package me.siwannie.tomnjerry.listeners;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class InteractListener implements Listener {

    private final TomNJerry plugin;

    public InteractListener(TomNJerry plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (plugin.getGameManager().getGameState() == GameState.IN_GAME || plugin.getGameManager().getGameState() == GameState.PAUSED) {
            Player player = event.getPlayer();
            if (player.hasPermission("tnj.exclude") && !player.hasPermission("tnj.tom")) return;

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (plugin.getGameManager().getGameState() == GameState.IN_GAME || plugin.getGameManager().getGameState() == GameState.PAUSED) {
            if (event.getWhoClicked() instanceof Player player) {
                if (player.hasPermission("tnj.exclude") && !player.hasPermission("tnj.tom")) return;

                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSwapHand(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        if (plugin.getGameManager().getGameState() == GameState.IN_GAME || plugin.getGameManager().getGameState() == GameState.PAUSED) {
            org.bukkit.entity.Player player = event.getPlayer();

            if (player.hasPermission("tnj.exclude") && !player.hasPermission("tnj.tom")) return;

            event.setCancelled(true);
        }
    }
}