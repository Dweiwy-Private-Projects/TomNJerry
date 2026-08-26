package me.siwannie.tomnjerry.managers;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.GameState;
import me.siwannie.tomnjerry.models.JerryData;
import me.siwannie.tomnjerry.utils.ItemBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrapManager implements Listener {

    private final TomNJerry plugin;
    private final MiniMessage mm;

    private final Map<Location, UUID> activeTraps;
    private int giveTrapTaskId = -1;

    public TrapManager(TomNJerry plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
        this.activeTraps = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void startPhase() {
        giveTrapsToToms();
        giveTrapTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::giveTrapsToToms, 6000L, 6000L).getTaskId();
    }

    public void endPhase() {
        if (giveTrapTaskId != -1) {
            Bukkit.getScheduler().cancelTask(giveTrapTaskId);
            giveTrapTaskId = -1;
        }
    }

    public void clearAllTraps() {
        endPhase();
        for (Location loc : activeTraps.keySet()) {
            if (loc.getBlock().getType() == Material.POLISHED_BLACKSTONE_BUTTON) {
                loc.getBlock().setType(Material.AIR);
            }
        }
        activeTraps.clear();
    }

    private void giveTrapsToToms() {
        ItemStack trapItem = new ItemBuilder(Material.POLISHED_BLACKSTONE_BUTTON)
                .name(mm.deserialize("<dark_gray><bold>Mouse Trap</bold></dark_gray>"))
                .lore(mm.deserialize("<gray>Place this on the ground.</gray>"), mm.deserialize("<gray>Cages the first Jerry to step on it.</gray>"))
                .addTag(plugin, "item_type", "mouse_trap")
                .build();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("tnj.tom")) {
                p.getInventory().addItem(trapItem);
                p.sendMessage(mm.deserialize("<dark_red>You received a Mouse Trap!</dark_red>"));
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void onTrapInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();

        if (ItemBuilder.hasTag(event.getItem(), plugin, "item_type", "mouse_trap")) {
            event.setCancelled(true);

            org.bukkit.block.Block clicked = event.getClickedBlock();
            if (clicked == null) return;

            org.bukkit.block.Block target = clicked.getRelative(event.getBlockFace());

            if (target.getType().isAir() || target.getType() == Material.WATER) {
                target.setType(Material.POLISHED_BLACKSTONE_BUTTON);
                org.bukkit.block.data.type.Switch buttonData = (org.bukkit.block.data.type.Switch) target.getBlockData();

                buttonData.setAttachedFace(org.bukkit.block.data.FaceAttachable.AttachedFace.FLOOR);

                target.setBlockData(buttonData);

                activeTraps.put(target.getLocation(), player.getUniqueId());

                player.sendMessage(mm.deserialize("<green>Mouse Trap placed successfully!</green>"));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.5f);

                event.getItem().subtract();
            } else {
                player.sendMessage(mm.deserialize("<red>You cannot place a trap here!</red>"));
            }
        }
    }

    @EventHandler
    public void onTrapStep(PlayerMoveEvent event) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        Player player = event.getPlayer();
        if (player.hasPermission("tnj.tom") || player.hasPermission("tnj.exclude")) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Block steppedBlock = event.getTo().getBlock();
        Location loc = steppedBlock.getLocation();

        if (activeTraps.containsKey(loc)) {
            JerryData data = plugin.getDataManager().getData(player.getUniqueId());
            if (data == null || data.isCaged()) return;

            UUID tomId = activeTraps.remove(loc);
            steppedBlock.setType(Material.AIR);

            plugin.getGameManager().cageJerry(player, data, false);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);

            Player tom = Bukkit.getPlayer(tomId);
            if (tom != null && tom.isOnline()) {
                tom.sendMessage(mm.deserialize("<green>Your Mouse Trap caught " + player.getName() + "!</green>"));
                tom.playSound(tom.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
        }
    }
}