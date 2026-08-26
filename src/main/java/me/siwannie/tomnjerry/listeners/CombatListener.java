package me.siwannie.tomnjerry.listeners;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.GameState;
import me.siwannie.tomnjerry.models.JerryData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class CombatListener implements Listener {

    private final TomNJerry plugin;
    private final MiniMessage mm;

    public CombatListener(TomNJerry plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) {
            return;
        }

        boolean attackerIsTom = attacker.hasPermission("tnj.tom");
        boolean victimIsTom = victim.hasPermission("tnj.tom");

        if (attackerIsTom && victimIsTom) {
            event.setCancelled(true);
            return;
        }

        if (!attackerIsTom && !victimIsTom) {
            event.setCancelled(true);
            return;
        }

        if (!attackerIsTom && victimIsTom) {
            event.setCancelled(true);
            return;
        }

        if (attackerIsTom && !victimIsTom && !victim.hasPermission("tnj.exclude")) {
            JerryData data = plugin.getDataManager().getData(victim.getUniqueId());
            if (data == null || data.isCaged()) {
                event.setCancelled(true);
                return;
            }

            event.setDamage(2.0);

            if (victim.getHealth() - event.getFinalDamage() <= 0) {
                event.setCancelled(true);

                plugin.getGameManager().cageJerry(victim, data);
                attacker.sendMessage(mm.deserialize("<green>You caught " + victim.getName() + "!</green>"));
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            } else {
                victim.sendMessage(mm.deserialize("<red>Ouch! You lost 1 heart!</red>"));
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        if (event.getEntity() instanceof Player player) {
            if (player.hasPermission("tnj.exclude") && !player.hasPermission("tnj.tom")) return;

            EntityDamageEvent.DamageCause cause = event.getCause();
            if (cause == EntityDamageEvent.DamageCause.FALL ||
                    cause == EntityDamageEvent.DamageCause.FIRE ||
                    cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                    cause == EntityDamageEvent.DamageCause.LAVA ||
                    cause == EntityDamageEvent.DamageCause.DROWNING ||
                    cause == EntityDamageEvent.DamageCause.CONTACT ||
                    cause == EntityDamageEvent.DamageCause.HOT_FLOOR ||
                    cause == EntityDamageEvent.DamageCause.SUFFOCATION ||
                    cause == EntityDamageEvent.DamageCause.FREEZE ||
                    cause == EntityDamageEvent.DamageCause.CRAMMING ||
                    cause.name().equals("CAMPFIRE")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        Player player = event.getEntity();
        if (player.hasPermission("tnj.exclude") && !player.hasPermission("tnj.tom")) return;

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.spigot().respawn();
            }
        });
    }

    @EventHandler
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        Player player = event.getPlayer();
        if (player.hasPermission("tnj.exclude") && !player.hasPermission("tnj.tom")) return;

        JerryData data = plugin.getDataManager().getData(player.getUniqueId());

        if (data != null && data.isCaged()) {
            org.bukkit.Location cageLoc = plugin.getConfigManager().getCageLocation();
            if (cageLoc != null) event.setRespawnLocation(cageLoc);
        } else if (player.hasPermission("tnj.tom")) {
            org.bukkit.Location tomSpawn = plugin.getConfigManager().getTomLocation();
            if (tomSpawn != null) event.setRespawnLocation(tomSpawn);
        } else {
            java.util.List<org.bukkit.Location> jerrySpawns = plugin.getConfigManager().getJerryLocations();
            if (!jerrySpawns.isEmpty()) {
                event.setRespawnLocation(jerrySpawns.get((int) (Math.random() * jerrySpawns.size())));
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                plugin.getGameManager().setupPlayerAttributes(player);
            }
        });
    }

    @EventHandler
    public void onHealthRegen(EntityRegainHealthEvent event) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        if (event.getEntity() instanceof Player player) {
            if (!player.hasPermission("tnj.tom") && !player.hasPermission("tnj.exclude")) {
                if (event.getRegainReason() != EntityRegainHealthEvent.RegainReason.MAGIC &&
                        event.getRegainReason() != EntityRegainHealthEvent.RegainReason.CUSTOM) {
                    event.setCancelled(true);
                }
            }
        }
    }
}