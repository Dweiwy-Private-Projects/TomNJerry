package me.siwannie.tomnjerry.managers;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.GameState;
import me.siwannie.tomnjerry.models.JerryData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GoldenCheeseManager {

    private final TomNJerry plugin;
    private final MiniMessage mm;
    private List<Location> spawnLocations = new ArrayList<>();

    private final List<ItemDisplay> activeCheeses = new ArrayList<>();

    private int proximityTaskId = -1;
    private int respawnTaskId = -1;

    private final Map<UUID, Integer> collectionProgress = new HashMap<>();
    private final Map<UUID, ItemDisplay> targetCheese = new HashMap<>();

    public GoldenCheeseManager(TomNJerry plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
    }

    public void setSpawnLocations(List<Location> locations) { this.spawnLocations = locations; }

    public void startPhase() {
        if (spawnLocations == null || spawnLocations.isEmpty()) {
            plugin.getLogger().warning("Golden Cheese spawn location is not set! Skipping event.");
            return;
        }
        spawnCheeses();
        proximityTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::checkProximity, 5L, 5L).getTaskId();
    }

    public void endPhase() {
        if (proximityTaskId != -1) Bukkit.getScheduler().cancelTask(proximityTaskId);
        if (respawnTaskId != -1) Bukkit.getScheduler().cancelTask(respawnTaskId);
        proximityTaskId = -1;
        respawnTaskId = -1;

        for (ItemDisplay display : activeCheeses) {
            if (display.isValid()) display.remove();
        }
        activeCheeses.clear();
        collectionProgress.clear();
        targetCheese.clear();
    }

    private void spawnCheeses() {
        if (spawnLocations.isEmpty() || !activeCheeses.isEmpty()) return;

        for (Location loc : spawnLocations) {
            Location centerLoc = loc.clone().add(0.5, 0.5, 0.5);
            ItemDisplay displayEntity = centerLoc.getWorld().spawn(centerLoc, ItemDisplay.class, ent -> {
                ent.addScoreboardTag("tnj_golden_cheese");
                ent.setGlowing(true);
                ent.setBillboard(ItemDisplay.Billboard.FIXED);
                ent.setItemStack(new ItemStack(Material.GOLD_BLOCK));
                ent.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.5f, 1.5f, 1.5f), new AxisAngle4f()));
            });
            activeCheeses.add(displayEntity);
        }

        Bukkit.broadcast(mm.deserialize("<gold><bold>GOLDEN CHEESE:</bold> " + activeCheeses.size() + " Golden Cheeses have spawned!</gold>"));
    }

    private void checkProximity() {
        if (activeCheeses.isEmpty()) return;
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("tnj.tom") || player.hasPermission("tnj.exclude")) continue;

            JerryData data = plugin.getDataManager().getData(player.getUniqueId());
            if (data == null || data.isCaged()) continue;

            ItemDisplay nearestCheese = null;
            double closestDist = 4.0;

            for (ItemDisplay cheese : activeCheeses) {
                if (!cheese.isValid()) continue;
                if (!player.getWorld().equals(cheese.getWorld())) continue;

                double dist = player.getLocation().distanceSquared(cheese.getLocation());
                if (dist <= closestDist) {
                    closestDist = dist;
                    nearestCheese = cheese;
                }
            }

            if (nearestCheese != null) {
                if (!player.isSneaking()) {
                    if (collectionProgress.containsKey(player.getUniqueId())) {
                        collectionProgress.remove(player.getUniqueId());
                        targetCheese.remove(player.getUniqueId());
                        player.sendActionBar(mm.deserialize("<red>Sneak to collect the Golden Cheese!</red>"));
                    }
                    continue;
                }

                if (targetCheese.get(player.getUniqueId()) != nearestCheese) {
                    collectionProgress.put(player.getUniqueId(), 0);
                    targetCheese.put(player.getUniqueId(), nearestCheese);
                }

                int progress = collectionProgress.getOrDefault(player.getUniqueId(), 0) + 1;
                collectionProgress.put(player.getUniqueId(), progress);

                if (progress >= 40) {
                    collectCheese(player, data, nearestCheese);
                    collectionProgress.remove(player.getUniqueId());
                    targetCheese.remove(player.getUniqueId());
                } else {
                    int percent = (int) ((progress / 40.0) * 100);
                    player.sendActionBar(mm.deserialize("<yellow>Collecting Golden Cheese: " + percent + "%</yellow>"));
                }
            } else {
                if (collectionProgress.containsKey(player.getUniqueId())) {
                    collectionProgress.remove(player.getUniqueId());
                    targetCheese.remove(player.getUniqueId());
                    player.sendActionBar(mm.deserialize("<red>Golden Cheese collection cancelled.</red>"));
                }
            }
        }
    }

    private void collectCheese(Player player, JerryData data, ItemDisplay cheese) {
        cheese.remove();
        activeCheeses.remove(cheese);

        data.addPoints(5);
        data.addBonusCheese(5);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
        Bukkit.broadcast(mm.deserialize("<gold>" + player.getName() + " claimed a Golden Cheese! (+5 Points & +5 Cheese)</gold>"));

        if (activeCheeses.isEmpty() && respawnTaskId == -1) {
            respawnTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                respawnTaskId = -1;
                if (plugin.getPhaseManager().getCurrentPhase() == me.siwannie.tomnjerry.models.GamePhase.GOLDEN_CAGE) {
                    spawnCheeses();
                }
            }, 20L * 120).getTaskId();
        }
    }
}