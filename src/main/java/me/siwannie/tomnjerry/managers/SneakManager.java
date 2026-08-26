package me.siwannie.tomnjerry.managers;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.GameState;
import me.siwannie.tomnjerry.models.JerryData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SneakManager {

    private final TomNJerry plugin;
    private final MiniMessage mm;
    private int taskId = -1;

    private final Map<UUID, Integer> sneakProgress = new HashMap<>();
    private final Map<UUID, Location> targetLocations = new HashMap<>();

    public SneakManager(TomNJerry plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
    }

    public void startTask() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::processSneaking, 10L, 10L).getTaskId();
    }

    public void stopTask() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
        sneakProgress.clear();
        targetLocations.clear();
    }

    private void processSneaking() {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) {
            if (plugin.getGameManager().getGameState() == GameState.PAUSED) return;
            stopTask();
            return;
        }

        int requiredTicks = plugin.getConfig().getInt("settings.sneak.seconds", 8) * 2;
        double radiusSq = Math.pow(plugin.getConfig().getDouble("settings.sneak.radius", 2.0), 2);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("tnj.tom") || player.hasPermission("tnj.exclude")) continue;

            JerryData data = plugin.getDataManager().getData(player.getUniqueId());
            if (data == null || data.isCaged()) continue;

            if (player.isSneaking()) {
                Location nearestCheese = getNearestUncollectedCheese(player, radiusSq);

                if (nearestCheese != null) {
                    UUID id = player.getUniqueId();
                    Location currentTarget = targetLocations.get(id);

                    if (currentTarget == null || !currentTarget.equals(nearestCheese)) {
                        sneakProgress.put(id, 0);
                        targetLocations.put(id, nearestCheese);
                    }

                    int progress = sneakProgress.get(id) + 1;
                    sneakProgress.put(id, progress);

                    int finishTicks = requiredTicks + 1;

                    if (progress >= finishTicks) {
                        finishCheeseCollection(player, data, nearestCheese);
                        sneakProgress.remove(id);
                        targetLocations.remove(id);
                    } else {
                        int percentage;
                        if (progress >= requiredTicks) {
                            percentage = 100;
                        } else {
                            percentage = (int) (((double) progress / requiredTicks) * 100);
                        }

                        String bar = generateProgressBar(Math.min(progress, requiredTicks), requiredTicks);

                        String format = plugin.getConfig().getString("messages.sneak_progress", "<yellow>Collecting <target>: </yellow><bar> <gray><percent>%");
                        player.sendActionBar(mm.deserialize(format,
                                Placeholder.unparsed("target", "Cheese"),
                                Placeholder.parsed("bar", bar),
                                Placeholder.unparsed("percent", String.valueOf(percentage))
                        ));
                        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 1.5f);
                    }
                } else {
                    cancelSneak(player);
                }
            } else {
                cancelSneak(player);
            }
        }
    }

    private void cancelSneak(Player player) {
        sneakProgress.remove(player.getUniqueId());
        targetLocations.remove(player.getUniqueId());
        player.sendActionBar(mm.deserialize("<gray>Sneak near a cheese to start collecting</gray>"));
    }

    private void finishCheeseCollection(Player player, JerryData data, Location cheeseLoc) {
        Location blockLoc = cheeseLoc.getBlock().getLocation();

        if (plugin.getGameManager().getActiveCheeseLocations().contains(blockLoc)) {
            plugin.getGameManager().getActiveCheeseLocations().remove(blockLoc);
            blockLoc.getBlock().setType(Material.AIR);

            int pts = plugin.getConfig().getInt("settings.points.per-cheese", 1);

            data.addFoundCheese(blockLoc);

            if (plugin.getPhaseManager() != null && plugin.getPhaseManager().isDoubleCheeseActive()) {
                pts *= 2;
                data.addBonusCheese(1);
            }

            data.addPoints(pts);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);

            String format = plugin.getConfig().getString("messages.sneak_complete_cheese", "<green>+<points> Point! Total Points: <total> | Held Cheese: <held></green>");
            player.sendMessage(mm.deserialize(format,
                    Placeholder.unparsed("points", String.valueOf(pts)),
                    Placeholder.unparsed("total", String.valueOf(data.getPoints())),
                    Placeholder.unparsed("held", String.valueOf(data.getCheeseHeld()))
            ));

            int cheeseLeft = plugin.getGameManager().getActiveCheeseLocations().size();

            if (cheeseLeft == 40) {
                Bukkit.broadcast(mm.deserialize("<yellow>Notice: The cheese will respawn when there is less than 40 cheese available!</yellow>"));
            } else if (cheeseLeft < 40) {
                plugin.getGameManager().spawnCheeseSubset();
            }
        }
    }

    private Location getNearestUncollectedCheese(Player player, double radiusSq) {
        for (Location loc : plugin.getGameManager().getActiveCheeseLocations()) {
            if (!player.getWorld().equals(loc.getWorld())) continue;

            if (player.getLocation().distanceSquared(loc) <= radiusSq) {
                return loc;
            }
        }
        return null;
    }

    private String generateProgressBar(int current, int max) {
        int bars = 10;
        int progressBars = (int) ((double) current / max * bars);
        StringBuilder sb = new StringBuilder("<green>");
        for (int i = 0; i < bars; i++) {
            if (i == progressBars) sb.append("</green><gray>");
            sb.append("█");
        }
        sb.append("");
        return sb.toString();
    }
}