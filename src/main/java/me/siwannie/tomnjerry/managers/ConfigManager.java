package me.siwannie.tomnjerry.managers;

import me.siwannie.tomnjerry.TomNJerry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigManager {

    private final TomNJerry plugin;
    private File cheeseFile;
    private FileConfiguration cheeseConfig;

    private final Set<Location> cheeseLocations;
    private Location cageLocation;
    private Location tomLocation;
    private Location lobbyLocation;

    private final List<Location> goldenCheeseLocations = new ArrayList<>();
    private final List<Location> jerryLocations;

    public ConfigManager(TomNJerry plugin) {
        this.plugin = plugin;
        this.cheeseLocations = new HashSet<>();
        this.jerryLocations = new ArrayList<>();

        plugin.saveDefaultConfig();
        setupCheeseFile();
        loadAllData();
    }

    private void setupCheeseFile() {
        cheeseFile = new File(plugin.getDataFolder(), "cheese.yml");
        if (!cheeseFile.exists()) {
            try { cheeseFile.createNewFile(); }
            catch (IOException e) { plugin.getLogger().severe("Could not create cheese.yml!"); }
        }
        cheeseConfig = YamlConfiguration.loadConfiguration(cheeseFile);
    }

    private void loadAllData() {
        cheeseLocations.clear();
        jerryLocations.clear();

        for (String locStr : cheeseConfig.getStringList("locations")) {
            Location loc = deserializeLocation(locStr);
            if (loc != null) cheeseLocations.add(loc);
        }

        for (String locStr : cheeseConfig.getStringList("jerry-spawns")) {
            Location loc = deserializeLocation(locStr);
            if (loc != null) jerryLocations.add(loc);
        }

        String cageStr = cheeseConfig.getString("cage-location");
        if (cageStr != null) cageLocation = deserializeLocation(cageStr);

        String tomStr = cheeseConfig.getString("tom-spawn");
        if (tomStr != null) tomLocation = deserializeLocation(tomStr);

        String lobbyStr = cheeseConfig.getString("lobby-location");
        if (lobbyStr != null) lobbyLocation = deserializeLocation(lobbyStr);

        for (String locStr : cheeseConfig.getStringList("golden-cheese-locations")) {
            Location loc = deserializeLocation(locStr);
            if (loc != null) goldenCheeseLocations.add(loc);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.getGoldenCheeseManager() != null) {
                plugin.getGoldenCheeseManager().setSpawnLocations(goldenCheeseLocations);
            }
        });
    }

    private void saveFile() {
        try { cheeseConfig.save(cheeseFile); }
        catch (IOException e) { plugin.getLogger().severe("Could not save cheese.yml!"); }
    }

    public Location getCageLocation() { return cageLocation; }
    public void setCageLocation(Location location) {
        this.cageLocation = location;
        cheeseConfig.set("cage-location", serializeLocation(location));
        saveFile();
    }

    public Location getTomLocation() { return tomLocation; }
    public void setTomLocation(Location location) {
        this.tomLocation = location;
        cheeseConfig.set("tom-spawn", serializeLocation(location));
        saveFile();
    }

    public Location getLobbyLocation() { return lobbyLocation; }
    public void setLobbyLocation(Location location) {
        this.lobbyLocation = location;
        cheeseConfig.set("lobby-location", serializeLocation(location));
        saveFile();
    }

    public List<Location> getGoldenCheeseLocations() { return goldenCheeseLocations; }

    public boolean addGoldenCheeseLocation(Location location) {
        goldenCheeseLocations.add(location);
        cheeseConfig.set("golden-cheese-locations", goldenCheeseLocations.stream().map(this::serializeLocation).collect(Collectors.toList()));
        saveFile();

        if (plugin.getGoldenCheeseManager() != null) {
            plugin.getGoldenCheeseManager().setSpawnLocations(goldenCheeseLocations);
        }
        return true;
    }

    public List<Location> getJerryLocations() { return jerryLocations; }
    public boolean addJerryLocation(Location location) {
        jerryLocations.add(location);
        cheeseConfig.set("jerry-spawns", jerryLocations.stream().map(this::serializeLocation).collect(Collectors.toList()));
        saveFile();
        return true;
    }

    public Set<Location> getCheeseLocations() { return cheeseLocations; }
    public int getTotalCheese() { return cheeseLocations.size(); }

    public boolean addCheeseLocation(Location location) {
        if (cheeseLocations.add(location)) {
            cheeseConfig.set("locations", cheeseLocations.stream().map(this::serializeLocation).collect(Collectors.toList()));
            saveFile();
            return true;
        }
        return false;
    }

    public boolean removeCheeseLocation(Location location) {
        if (cheeseLocations.remove(location)) {
            cheeseConfig.set("locations", cheeseLocations.stream().map(this::serializeLocation).collect(Collectors.toList()));
            saveFile();
            return true;
        }
        return false;
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location deserializeLocation(String locStr) {
        String[] parts = locStr.split(",");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) { return null; }
    }
}