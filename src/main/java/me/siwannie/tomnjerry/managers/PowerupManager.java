package me.siwannie.tomnjerry.managers;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.GameState;
import me.siwannie.tomnjerry.utils.ItemBuilder;
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

import java.util.*;

public class PowerupManager {

    private final TomNJerry plugin;
    private final Map<ItemDisplay, String> activePowerups;
    private final Random random;
    private int spawnTaskId = -1;
    private int spinTaskId = -1;
    private int proximityTaskId = -1;
    private final MiniMessage mm;

    public PowerupManager(TomNJerry plugin) {
        this.plugin = plugin;
        this.activePowerups = new HashMap<>();
        this.random = new Random();
        this.mm = MiniMessage.miniMessage();
    }

    public void startTasks() {
        spawnTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnRandomPowerup, 400L, 400L).getTaskId();

        spinTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (ItemDisplay display : activePowerups.keySet()) {
                if (display.isValid()) {
                    Location loc = display.getLocation();
                    loc.setYaw((loc.getYaw() + 5f) % 360f);
                    display.teleport(loc);
                }
            }
        }, 1L, 1L).getTaskId();

        proximityTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::checkProximity, 5L, 5L).getTaskId();
    }

    public void stopTasksAndCleanup() {
        if (spawnTaskId != -1) Bukkit.getScheduler().cancelTask(spawnTaskId);
        if (spinTaskId != -1) Bukkit.getScheduler().cancelTask(spinTaskId);
        if (proximityTaskId != -1) Bukkit.getScheduler().cancelTask(proximityTaskId);
        spawnTaskId = -1;
        spinTaskId = -1;
        proximityTaskId = -1;

        for (ItemDisplay display : activePowerups.keySet()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        activePowerups.clear();

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (ItemDisplay entity : world.getEntitiesByClass(ItemDisplay.class)) {
                if (entity.getScoreboardTags().contains("tnj_powerup")) {
                    entity.remove();
                }
            }
        }
    }

    private void spawnRandomPowerup() {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        if (activePowerups.size() >= 30) return;

        List<Location> spawns = plugin.getConfigManager().getJerryLocations();
        if (spawns.isEmpty()) spawns = new ArrayList<>(plugin.getConfigManager().getCheeseLocations());
        if (spawns.isEmpty()) return;

        Location baseLoc = spawns.get(random.nextInt(spawns.size()));

        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = 1.0 + (random.nextDouble() * 2.0);

        Location spawnLoc = baseLoc.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);

        spawnLoc.setX(spawnLoc.getBlockX() + 0.5);
        spawnLoc.setY(baseLoc.getBlockY() + 0.25);
        spawnLoc.setZ(spawnLoc.getBlockZ() + 0.5);

        ItemDisplay display = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, ent -> {
            ent.addScoreboardTag("tnj_powerup");
            ent.setGlowing(true);
            ent.setGlowColorOverride(org.bukkit.Color.WHITE);
            ent.setBillboard(ItemDisplay.Billboard.FIXED);

            ent.setTransformation(new Transformation(
                    new Vector3f(), new AxisAngle4f(),
                    new Vector3f(1.0f, 1.0f, 1.0f), new AxisAngle4f()
            ));
        });

        int roll = random.nextInt(10);
        String type;
        if (roll < 4) {
            type = "SPEED";
            display.setItemStack(new ItemStack(Material.SUGAR));
        } else if (roll < 8) {
            type = "HEAL";
            display.setItemStack(new ItemStack(Material.GLISTERING_MELON_SLICE));
        } else {
            type = "INVIS";
            display.setItemStack(new ItemStack(Material.GLASS_BOTTLE));
        }

        activePowerups.put(display, type);
    }

    private void checkProximity() {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        activePowerups.keySet().removeIf(display -> !display.isValid());
        Iterator<Map.Entry<ItemDisplay, String>> iterator = activePowerups.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ItemDisplay, String> entry = iterator.next();
            ItemDisplay display = entry.getKey();
            String type = entry.getValue();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("tnj.tom") || player.hasPermission("tnj.exclude")) continue;
                if (plugin.getDataManager().getData(player.getUniqueId()) != null && plugin.getDataManager().getData(player.getUniqueId()).isCaged()) continue;
                if (!player.getWorld().equals(display.getWorld())) continue;

                if (player.getLocation().distanceSquared(display.getLocation()) <= 2.25) {
                    givePowerupItem(player, type);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
                    display.remove();
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private void givePowerupItem(Player player, String type) {
        ItemStack item = null;
        if (type.equals("SPEED")) {
            item = new ItemBuilder(Material.SUGAR)
                    .name(mm.deserialize("<aqua><bold>Speed Powerup</bold></aqua>"))
                    .lore(mm.deserialize("<gray>Grants Speed II for 15s.</gray>"))
                    .addTag(plugin, "item_type", "powerup_speed")
                    .build();
            player.sendMessage(mm.deserialize("<aqua>You picked up a Speed Powerup!</aqua>"));
        } else if (type.equals("INVIS")) {
            item = new ItemBuilder(Material.GLASS_BOTTLE)
                    .name(mm.deserialize("<gray><bold>Invisibility Powerup</bold></gray>"))
                    .lore(mm.deserialize("<gray>Grants Invisibility for 6s.</gray>"))
                    .addTag(plugin, "item_type", "powerup_invis")
                    .build();
            player.sendMessage(mm.deserialize("<gray>You picked up an Invisibility Powerup!</gray>"));
        } else if (type.equals("HEAL")) {
            item = new ItemBuilder(Material.GLISTERING_MELON_SLICE)
                    .name(mm.deserialize("<light_purple><bold>Heal Powerup</bold></light_purple>"))
                    .lore(mm.deserialize("<gray>Instantly heals 1 Heart.</gray>"))
                    .addTag(plugin, "item_type", "powerup_heal")
                    .build();
            player.sendMessage(mm.deserialize("<light_purple>You picked up a Healing Powerup!</light_purple>"));
        }

        if (item != null) {
            player.getInventory().addItem(item);
        }
    }
}