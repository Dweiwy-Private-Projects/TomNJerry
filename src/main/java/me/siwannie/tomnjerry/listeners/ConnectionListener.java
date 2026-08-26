package me.siwannie.tomnjerry.listeners;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.GameState;
import me.siwannie.tomnjerry.models.JerryData;
import me.siwannie.tomnjerry.utils.ItemBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class ConnectionListener implements Listener {

    private final TomNJerry plugin;

    public ConnectionListener(TomNJerry plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getGameManager().getGameState() == GameState.IN_GAME || plugin.getGameManager().getGameState() == GameState.PAUSED) {
            if (player.hasPermission("tnj.exclude") && !player.hasPermission("tnj.tom")) {
                return;
            }

            JerryData data = plugin.getDataManager().getOrCreateData(player);
            plugin.getGameManager().setupPlayerAttributes(player);
            player.setGameMode(GameMode.ADVENTURE);

            MiniMessage mm = MiniMessage.miniMessage();

            if (!data.hasReceivedKit()) {
                player.getInventory().clear();
                data.setReceivedKit(true);

                if (player.hasPermission("tnj.tom")) {
                    player.getInventory().addItem(
                            new ItemBuilder(Material.IRON_SWORD).name(mm.deserialize("<red><bold>Claws</bold></red>")).unbreakable()
                                    .lore(mm.deserialize("<gray>A basic melee attack.</gray>"), mm.deserialize("<gray>Deals exactly 1 Heart to Jerrys.</gray>")).build(),
                            new ItemBuilder(Material.TORCH).name(mm.deserialize("<yellow><bold>Highlight</bold></yellow>")).addTag(plugin, "item_type", "highlight")
                                    .lore(mm.deserialize("<gray>Highlights nearby Jerrys for 10s.</gray>"), mm.deserialize("<gray>Cooldown: 90s</gray>")).build(),
                            new ItemBuilder(Material.TNT).name(mm.deserialize("<dark_red><bold>Kaboom</bold></dark_red>")).addTag(plugin, "item_type", "kaboom")
                                    .lore(mm.deserialize("<gray>Enlarges all Jerrys for 8s.</gray>"), mm.deserialize("<gray>Cooldown: 180s</gray>")).build(),
                            new ItemBuilder(Material.INK_SAC).name(mm.deserialize("<dark_gray><bold>Blinding</bold></dark_gray>")).addTag(plugin, "item_type", "blinding")
                                    .lore(mm.deserialize("<gray>Blinds all Jerrys for 5s.</gray>"), mm.deserialize("<gray>Cooldown: 120s</gray>")).build()
                    );
                } else {
                    player.getInventory().addItem(
                            new ItemBuilder(Material.BLAZE_ROD).name(mm.deserialize("<gold><bold>Stunner</bold></gold>")).addTag(plugin, "item_type", "stunner")
                                    .lore(mm.deserialize("<gray>Slows Tom for 3 seconds.</gray>"), mm.deserialize("<yellow>Cost: 2 Cheese</yellow>"), mm.deserialize("<gray>Cooldown: 45s</gray>")).build(),
                            new ItemBuilder(Material.ENDER_PEARL).name(mm.deserialize("<dark_purple><bold>Swapper</bold></dark_purple>")).addTag(plugin, "item_type", "swapper")
                                    .lore(mm.deserialize("<gray>Teleport to a random room.</gray>"), mm.deserialize("<yellow>Cost: 10 Cheese</yellow>"), mm.deserialize("<gray>Cooldown: 30s</gray>")).build(),
                            new ItemBuilder(Material.COBWEB).name(mm.deserialize("<white><bold>WebShooter</bold></white>")).addTag(plugin, "item_type", "webshooter")
                                    .lore(mm.deserialize("<gray>Shoots a 3-second cobweb trap.</gray>"), mm.deserialize("<yellow>Cost: 7 Cheese</yellow>"), mm.deserialize("<gray>Cooldown: 60s</gray>")).build()
                    );
                }
            }

            if (player.hasPermission("tnj.tom")) {
                Location tomSpawn = plugin.getConfigManager().getTomLocation();
                if (tomSpawn != null) player.teleport(tomSpawn);
            } else {
                if (data.isCaged()) {
                    Location cageLoc = plugin.getConfigManager().getCageLocation();
                    if (cageLoc != null) player.teleport(cageLoc);
                } else if (data.getLogoutLocation() != null) {
                    player.teleport(data.getLogoutLocation());
                    data.setLogoutLocation(null);
                } else {
                    List<Location> jerrySpawns = plugin.getConfigManager().getJerryLocations();
                    if (!jerrySpawns.isEmpty()) {
                        player.teleport(jerrySpawns.get((int) (Math.random() * jerrySpawns.size())));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getGameManager().getGameState() == GameState.IN_GAME) {
            JerryData data = plugin.getDataManager().getData(player.getUniqueId());
            if (data != null && !data.isCaged() && !player.hasPermission("tnj.tom")) {
                data.setLogoutLocation(player.getLocation());
            }
        }

        AttributeInstance scale = player.getAttribute(Attribute.GENERIC_SCALE);
        if (scale != null) {
            scale.setBaseValue(1.0);
        }

        AttributeInstance health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(20.0);
        }

        if (plugin.getGameManager().getGameState() == GameState.IN_GAME || plugin.getGameManager().getGameState() == GameState.PAUSED) {
            if (player.getGameMode() == GameMode.ADVENTURE) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        if (plugin.getSetupManager().isInSetupMode(player)) {
            plugin.getSetupManager().setSetupMode(player, me.siwannie.tomnjerry.models.SetupMode.NONE);
        }
    }
}