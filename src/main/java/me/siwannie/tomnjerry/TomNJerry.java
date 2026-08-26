package me.siwannie.tomnjerry;

import me.siwannie.tomnjerry.commands.TomNJerryCommand;
import me.siwannie.tomnjerry.hooks.TomNJerryExpansion;
import me.siwannie.tomnjerry.listeners.*;
import me.siwannie.tomnjerry.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class TomNJerry extends JavaPlugin {

    // Managers
    private ConfigManager configManager;
    private DataManager dataManager;
    private SetupManager setupManager;
    private GameManager gameManager;
    private PowerupManager powerupManager;
    private PhaseManager phaseManager;
    private TrapManager trapManager;
    private GoldenCheeseManager goldenCheeseManager;

    private AbilityListener abilityListener;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.setupManager = new SetupManager();
        this.powerupManager = new PowerupManager(this);

        this.gameManager = new GameManager(this);

        this.phaseManager = new PhaseManager(this);
        this.trapManager = new TrapManager(this);
        this.goldenCheeseManager = new GoldenCheeseManager(this);

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(new ConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SetupListener(this), this);
        this.abilityListener = new AbilityListener(this);
        Bukkit.getPluginManager().registerEvents(abilityListener, this);

        // Register Commands
        TomNJerryCommand cmd = new TomNJerryCommand(this);
        if (getCommand("tnj") != null) {
            getCommand("tnj").setExecutor(cmd);
            getCommand("tnj").setTabCompleter(cmd);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TomNJerryExpansion(this).register();
        }

        getLogger().info("TomNJerry has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.forceStopGame();
        }
        if (powerupManager != null) {
            powerupManager.stopTasksAndCleanup();
        }
        getLogger().info("TomNJerry has been disabled.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public SetupManager getSetupManager() { return setupManager; }
    public GameManager getGameManager() { return gameManager; }
    public PowerupManager getPowerupManager() { return powerupManager; }

    public PhaseManager getPhaseManager() { return phaseManager; }
    public TrapManager getTrapManager() { return trapManager; }
    public GoldenCheeseManager getGoldenCheeseManager() { return goldenCheeseManager; }

    public AbilityListener getAbilityListener() { return abilityListener; }
}