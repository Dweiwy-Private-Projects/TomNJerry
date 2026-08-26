package me.siwannie.tomnjerry.models;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JerryData {
    private final UUID uuid;
    private final String name;
    private int points;
    private int cheeseHeld;
    private final Set<Location> foundCheese;
    private long stunnerCooldown;
    private boolean isCaged;
    private long timeCaged = 0;
    public long getTimeCaged() { return timeCaged; }
    public void setTimeCaged(long time) { this.timeCaged = time; }
    private int survivalBonuses = 0;
    private int totalCheeseCollected = 0;
    private boolean receivedKit = false;
    private Location logoutLocation = null;

    public JerryData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.points = 0;
        this.cheeseHeld = 0;
        this.foundCheese = new HashSet<>();
        this.stunnerCooldown = 0;
        this.isCaged = false;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }

    public int getPoints() { return points; }
    public void addPoints(int amount) {
        this.points += amount;
        if (this.points < 0) {
            this.points = 0;
        }
    }
    public int getCheeseHeld() { return cheeseHeld; }
    public void consumeCheese(int amount) { this.cheeseHeld -= amount; }

    public boolean isCaged() { return isCaged; }
    public void setCaged(boolean caged) { isCaged = caged; }

    public boolean addFoundCheese(Location loc) {
        foundCheese.add(loc);
        this.cheeseHeld++;
        this.totalCheeseCollected++;
        return true;
    }

    public void addBonusCheese(int amount) {
        this.cheeseHeld += amount;
        this.totalCheeseCollected += amount;
    }

    public boolean hasFoundCheese(Location location) {
        return foundCheese.contains(location);
    }

    public boolean canUseStunner() {
        return System.currentTimeMillis() >= stunnerCooldown;
    }

    public void setStunnerCooldown(long cooldownMillis) {
        this.stunnerCooldown = System.currentTimeMillis() + cooldownMillis;
    }

    public long getStunnerCooldownRemaining() {
        long remaining = stunnerCooldown - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    public int getSurvivalBonuses() {
        return survivalBonuses;
    }

    public void addSurvivalBonus() {
        this.survivalBonuses++;
    }

    public int getTotalCheeseCollected() {
        return totalCheeseCollected;
    }

    public boolean hasReceivedKit() { return receivedKit; }
    public void setReceivedKit(boolean receivedKit) { this.receivedKit = receivedKit; }

    public Location getLogoutLocation() { return logoutLocation; }
    public void setLogoutLocation(Location loc) { this.logoutLocation = loc; }
}