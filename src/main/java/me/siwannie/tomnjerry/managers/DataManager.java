package me.siwannie.tomnjerry.managers;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.JerryData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DataManager {

    private final TomNJerry plugin;

    private final Map<UUID, JerryData> activePlayers;
    private List<JerryData> cachedLeaderboard;

    public DataManager(TomNJerry plugin) {
        this.plugin = plugin;
        this.activePlayers = new ConcurrentHashMap<>();
        this.cachedLeaderboard = new ArrayList<>();
        startLeaderboardTask();
    }

    public JerryData getOrCreateData(Player player) {
        return activePlayers.computeIfAbsent(player.getUniqueId(), k -> new JerryData(player.getUniqueId(), player.getName()));
    }

    public JerryData getData(UUID uuid) {
        return activePlayers.get(uuid);
    }

    public void removeData(UUID uuid) {
        activePlayers.remove(uuid);
    }

    public void clearAllData() {
        activePlayers.clear();
        cachedLeaderboard.clear();
    }

    public Collection<JerryData> getAllData() {
        return activePlayers.values();
    }

    public List<JerryData> getLeaderboard() {
        return cachedLeaderboard;
    }

    public void saveMatchResults() {
        if (activePlayers.isEmpty()) return;

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File resultFile = new File(dataFolder, "match_" + timestamp + ".txt");

        List<JerryData> sorted = activePlayers.values().stream()
                .sorted(Comparator.comparingInt(JerryData::getPoints).reversed())
                .collect(Collectors.toList());

        try (PrintWriter writer = new PrintWriter(new FileWriter(resultFile))) {
            writer.println("Tom & Jerry Match Results");
            writer.println("Date: " + timestamp);
            writer.println("Total Players: " + sorted.size());
            writer.println("--------------------------------------------------");

            int rank = 1;
            for (JerryData data : sorted) {
                writer.println(rank + ". " + data.getName() + " | UUID: " + data.getUuid().toString() + " | Points: " + data.getPoints());
                rank++;
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save match results: " + e.getMessage());
        }
    }

    private void startLeaderboardTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            cachedLeaderboard = activePlayers.values().stream()
                    .sorted(Comparator.comparingInt(JerryData::getPoints).reversed())
                    .collect(Collectors.toList());
        }, 40L, 40L);
    }
}