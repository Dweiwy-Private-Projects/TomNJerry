package me.siwannie.tomnjerry.listeners;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.GameState;
import me.siwannie.tomnjerry.models.JerryData;
import me.siwannie.tomnjerry.utils.ItemBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AbilityListener implements Listener {

    private final TomNJerry plugin;
    private final MiniMessage mm;

    private long globalHighlightCooldown = 0L;
    private long globalKaboomCooldown = 0L;
    private long globalBlindingCooldown = 0L;

    private final Map<UUID, Long> swapperCooldowns = new HashMap<>();
    private final Map<UUID, Long> webShooterCooldowns = new HashMap<>();

    public AbilityListener(TomNJerry plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
    }

    @EventHandler
    public void onAbilityUse(PlayerInteractEvent event) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null) return;

        Player player = event.getPlayer();
        JerryData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data != null && data.isCaged()) {
            player.sendMessage(mm.deserialize("<red>You cannot use abilities while in the cage!</red>"));
            return;
        }
        ItemStack item = event.getItem();

        if (player.hasPermission("tnj.tom")) {
            PotionEffect slowness = player.getPotionEffect(PotionEffectType.SLOWNESS);
            if (slowness != null && slowness.getAmplifier() >= 100) {
                player.sendMessage(mm.deserialize("<red>You cannot use abilities during the hiding phase!</red>"));
                return;
            }
        }

        if (ItemBuilder.hasTag(item, plugin, "item_type", "powerup_speed")) {
            event.setCancelled(true);
            int baseDuration = 20 * 15;
            PotionEffect currentSpeed = player.getPotionEffect(PotionEffectType.SPEED);

            int totalDuration = (currentSpeed != null) ? currentSpeed.getDuration() + baseDuration : baseDuration;

            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, totalDuration, 1, false, false, false));            player.sendMessage(mm.deserialize("<aqua>You used a Speed Powerup!</aqua>"));
            player.playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 1f, 1f);
            item.subtract();
            return;
        }

        if (ItemBuilder.hasTag(item, plugin, "item_type", "powerup_invis")) {
            event.setCancelled(true);
            int baseDuration = 20 * 6;
            PotionEffect currentInvis = player.getPotionEffect(PotionEffectType.INVISIBILITY);

            int totalDuration = (currentInvis != null) ? currentInvis.getDuration() + baseDuration : baseDuration;

            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, totalDuration, 0, false, false, false));
            player.sendMessage(mm.deserialize("<gray>You used an Invisibility Powerup!</gray>"));
            player.playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 1f, 1f);
            item.subtract();
            return;
        }

        if (ItemBuilder.hasTag(item, plugin, "item_type", "powerup_heal")) {
            event.setCancelled(true);

            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();

            if (player.getHealth() >= maxHealth) {
                player.sendMessage(mm.deserialize("<red>Your health is already full!</red>"));
                return;
            }

            double newHealth = Math.min(player.getHealth() + 2.0, maxHealth);
            player.setHealth(newHealth);
            player.sendMessage(mm.deserialize("<light_purple>You used a Healing Powerup! (+1 Heart)</light_purple>"));
            player.playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 1f, 1f);
            item.subtract();
            return;
        }

        if (ItemBuilder.hasTag(item, plugin, "item_type", "blinding")) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();

            if (now >= globalBlindingCooldown) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!target.hasPermission("tnj.tom") && !target.hasPermission("tnj.exclude")) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 5, 0));
                        target.sendMessage(mm.deserialize("<dark_gray><bold>BLINDED!</bold> Tom has cast blindness upon you!</dark_gray>"));
                    }
                }

                int blindCd = 120;
                globalBlindingCooldown = now + (blindCd * 1000L);
                Bukkit.broadcast(mm.deserialize("<dark_gray>[Tom] " + player.getName() + " casted Blinding!</dark_gray>"));
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f);
            } else {
                long left = (globalBlindingCooldown - now) / 1000;
                player.sendMessage(mm.deserialize("<red>Blinding is on global cooldown for " + left + "s!</red>"));
            }
            return;
        }

        if (ItemBuilder.hasTag(item, plugin, "item_type", "stunner") && data != null) {
            event.setCancelled(true);
            int cost = plugin.getPhaseManager().getAbilityCost(2);

            if (data.getCheeseHeld() >= cost) {
                if (data.canUseStunner()) {
                    data.consumeCheese(cost);
                    Snowball snowball = player.launchProjectile(Snowball.class);
                    snowball.getPersistentDataContainer().set(
                            new org.bukkit.NamespacedKey(plugin, "is_stunner"),
                            org.bukkit.persistence.PersistentDataType.BYTE,
                            (byte) 1
                    );

                    int cdSeconds = 45;
                    data.setStunnerCooldown(cdSeconds * 1000L);
                    player.sendMessage(mm.deserialize("<green>Stunner fired! (-" + cost + " Cheese) Cooldown: " + cdSeconds + "s</green>"));
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
                } else {
                    long left = data.getStunnerCooldownRemaining() / 1000;
                    player.sendMessage(mm.deserialize("<red>Stunner is on cooldown for " + left + "s!</red>"));
                }
            } else {
                player.sendMessage(mm.deserialize("<red>You need " + cost + " Cheese to use the Stunner!</red>"));
            }
            return;
        }

        if (ItemBuilder.hasTag(item, plugin, "item_type", "swapper") && data != null) {
            event.setCancelled(true);
            int cost = plugin.getPhaseManager().getAbilityCost(10);
            long now = System.currentTimeMillis();
            long cd = swapperCooldowns.getOrDefault(player.getUniqueId(), 0L);

            if (now >= cd) {
                if (data.getCheeseHeld() >= cost) {
                    List<Location> safeSpots = new ArrayList<>(plugin.getConfigManager().getCheeseLocations());
                    safeSpots.removeIf(loc -> loc.distanceSquared(player.getLocation()) < 400.0);

                    if (safeSpots.isEmpty()) safeSpots = new ArrayList<>(plugin.getConfigManager().getCheeseLocations());

                    if (!safeSpots.isEmpty()) {
                        data.consumeCheese(cost);
                        Location randomSpot = safeSpots.get((int) (Math.random() * safeSpots.size())).clone();

                        randomSpot.setX(randomSpot.getBlockX() + 0.5);
                        randomSpot.setY(randomSpot.getBlockY() + 1.0);
                        randomSpot.setZ(randomSpot.getBlockZ() + 0.5);

                        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                        player.teleport(randomSpot);

                        int swapperCd = 30;
                        swapperCooldowns.put(player.getUniqueId(), now + (swapperCd * 1000L));

                        player.sendMessage(mm.deserialize("<aqua>Swapped to a new room! (-" + cost + " Cheese)</aqua>"));
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    } else {
                        player.sendMessage(mm.deserialize("<red>No valid rooms to swap to!</red>"));
                    }
                } else {
                    player.sendMessage(mm.deserialize("<red>You need " + cost + " Cheese to use the Swapper!</red>"));
                }
            } else {
                long left = (cd - now) / 1000;
                player.sendMessage(mm.deserialize("<red>Swapper is on cooldown for " + left + "s!</red>"));
            }
            return;
        }

        if (ItemBuilder.hasTag(item, plugin, "item_type", "highlight")) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();

            if (now >= globalHighlightCooldown) {
                int caughtCount = 0;

                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!target.hasPermission("tnj.tom") && !target.hasPermission("tnj.exclude")) {

                        if (target.getWorld().equals(player.getWorld()) && target.getLocation().distanceSquared(player.getLocation()) <= 400.0) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 10, 0));
                            target.sendMessage(mm.deserialize("<red><bold>WARNING!</bold> A nearby Tom has revealed your location!</red>"));
                            caughtCount++;
                        }
                    }
                }

                int highlightCd = 90;
                globalHighlightCooldown = now + (highlightCd * 1000L);

                Bukkit.broadcast(mm.deserialize("<yellow>[Tom] " + player.getName() + " highlighted " + caughtCount + " nearby mice!</yellow>"));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
            } else {
                long left = (globalHighlightCooldown - now) / 1000;
                player.sendMessage(mm.deserialize("<red>Highlight is on global cooldown for " + left + "s!</red>"));
            }
            return;
        }

        if (ItemBuilder.hasTag(item, plugin, "item_type", "webshooter") && data != null) {
            event.setCancelled(true);
            int cost = plugin.getPhaseManager().getAbilityCost(7);
            long now = System.currentTimeMillis();
            long cd = webShooterCooldowns.getOrDefault(player.getUniqueId(), 0L);

            if (now >= cd) {
                if (data.getCheeseHeld() >= cost) {
                    data.consumeCheese(cost);
                    Snowball snowball = player.launchProjectile(Snowball.class);
                    snowball.getPersistentDataContainer().set(
                            new org.bukkit.NamespacedKey(plugin, "is_webshooter"),
                            org.bukkit.persistence.PersistentDataType.BYTE,
                            (byte) 1
                    );

                    int webCd = 60;
                    webShooterCooldowns.put(player.getUniqueId(), now + (webCd * 1000L));
                    player.sendMessage(mm.deserialize("<green>WebShooter fired! (-" + cost + " Cheese)</green>"));
                    player.playSound(player.getLocation(), Sound.ENTITY_SPIDER_DEATH, 1f, 1f);
                } else {
                    player.sendMessage(mm.deserialize("<red>You need " + cost + " Cheese to use the WebShooter!</red>"));
                }
            } else {
                long left = (cd - now) / 1000;
                player.sendMessage(mm.deserialize("<red>WebShooter is on cooldown for " + left + "s!</red>"));
            }
            return;
        }

        if (ItemBuilder.hasTag(item, plugin, "item_type", "kaboom")) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();

            if (now >= globalKaboomCooldown) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!target.hasPermission("tnj.tom") && !target.hasPermission("tnj.exclude")) {
                        JerryData targetData = plugin.getDataManager().getData(target.getUniqueId());
                        if (targetData != null && targetData.isCaged()) continue;

                        Location targetLoc = target.getLocation();
                        Location cageLoc = plugin.getConfigManager().getCageLocation();
                        if (cageLoc != null && targetLoc.getWorld().equals(cageLoc.getWorld())) {
                            if (targetLoc.distanceSquared(cageLoc) <= 64.0) continue;
                        }

                        target.teleport(new Location(targetLoc.getWorld(), targetLoc.getBlockX() + 0.5, targetLoc.getY(), targetLoc.getBlockZ() + 0.5, targetLoc.getYaw(), targetLoc.getPitch()));

                        org.bukkit.attribute.AttributeInstance scale = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
                        if (scale != null) {
                            scale.setBaseValue(0.5);

                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (target.isOnline() && plugin.getGameManager().getGameState() == GameState.IN_GAME) {
                                    scale.setBaseValue(0.25);
                                }
                            }, 20L * 8);
                        }
                        target.sendMessage(mm.deserialize("<red><bold>KABOOM!</bold> Tom enlarged you for 8 seconds!</red>"));
                    }
                }

                int kaboomCd = 180;
                globalKaboomCooldown = now + (kaboomCd * 1000L);
                Bukkit.broadcast(mm.deserialize("<dark_red>[Tom] " + player.getName() + " activated Kaboom!</dark_red>"));
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            } else {
                long left = (globalKaboomCooldown - now) / 1000;
                player.sendMessage(mm.deserialize("<red>Kaboom is on global cooldown for " + left + "s!</red>"));
            }
            return;
        }
    }

    public void resetCooldowns() {
        globalHighlightCooldown = 0L;
        globalKaboomCooldown = 0L;
        globalBlindingCooldown = 0L;

        swapperCooldowns.clear();
        webShooterCooldowns.clear();
    }

    @EventHandler
    public void onStunnerHit(ProjectileHitEvent event) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        if (event.getEntity() instanceof Snowball snowball) {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "is_stunner");
            org.bukkit.NamespacedKey webKey = new org.bukkit.NamespacedKey(plugin, "is_webshooter");
            if (snowball.getPersistentDataContainer().has(webKey, org.bukkit.persistence.PersistentDataType.BYTE)) {

                Location hitLoc = null;
                if (event.getHitEntity() != null) {
                    hitLoc = event.getHitEntity().getLocation();
                } else if (event.getHitBlock() != null && event.getHitBlockFace() != null) {
                    hitLoc = event.getHitBlock().getRelative(event.getHitBlockFace()).getLocation();
                }

                if (hitLoc != null) {
                    org.bukkit.block.Block block = hitLoc.getBlock();
                    if (block.isPassable()) {
                        block.setType(org.bukkit.Material.COBWEB);

                        Location finalHitLoc = hitLoc;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (finalHitLoc.getBlock().getType() == org.bukkit.Material.COBWEB) {
                                finalHitLoc.getBlock().setType(org.bukkit.Material.AIR);
                            }
                        }, 20L * 3);
                    }
                }
            }

            if (snowball.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE)) {
                if (event.getHitEntity() instanceof Player hitPlayer) {
                    if (hitPlayer.hasPermission("tnj.tom")) {
                        hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
                        hitPlayer.sendMessage(mm.deserialize("<red>You were hit by a Stunner! Slowed for 3s!</red>"));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPearlThrow(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        if (event.getEntity() instanceof org.bukkit.entity.EnderPearl pearl) {
            if (pearl.getShooter() instanceof Player player) {
                if (!player.hasPermission("tnj.tom") && !player.hasPermission("tnj.exclude")) {
                    event.setCancelled(true);
                }
            }
        }
    }
}