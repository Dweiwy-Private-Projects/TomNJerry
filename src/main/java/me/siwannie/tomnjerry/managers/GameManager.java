package me.siwannie.tomnjerry.managers;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.GameState;
import me.siwannie.tomnjerry.models.JerryData;
import me.siwannie.tomnjerry.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.BlockState;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.block.Skull;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import java.net.URL;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class GameManager {

    private final TomNJerry plugin;
    private final MiniMessage mm;
    private GameState gameState;
    private int timeLeftSeconds;
    private int timerTaskId;

    private BossBar timerBar;
    private Team tomTeam, jerryTeam, cheeseTeam;

    private boolean cheeseRespawnEnabled = false;
    private final Map<Location, BlockState> cheeseStates;
    private final Set<Location> activeCheeseLocations;
    private final Map<UUID, Integer> survivalSeconds = new HashMap<>();
    private PlayerProfile cheeseProfile;

    public GameManager(TomNJerry plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
        this.gameState = GameState.WAITING;
        this.timeLeftSeconds = 0;
        this.timerTaskId = -1;
        this.cheeseStates = new HashMap<>();
        this.activeCheeseLocations = new HashSet<>();

        setupScoreboardTeam();
        this.timerBar = Bukkit.createBossBar("Waiting...", BarColor.PINK, BarStyle.SOLID);

        try {
            this.cheeseProfile = Bukkit.createPlayerProfile(UUID.fromString("c0ffee00-0000-0000-0000-000000000000"), "Cheese");
            PlayerTextures textures = cheeseProfile.getTextures();
            textures.setSkin(new URL("http://textures.minecraft.net/texture/d089bda72e1985324f3014932cde0241f9f38ddb98f07358b93413d80e5ac0ed"));
            cheeseProfile.setTextures(textures);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load custom cheese texture URL!");
        }
    }

    private void setupScoreboardTeam() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        tomTeam = getOrRegisterTeam(board, "TNJ_Tom", org.bukkit.ChatColor.AQUA);
        jerryTeam = getOrRegisterTeam(board, "TNJ_Jerry", org.bukkit.ChatColor.GOLD);
        cheeseTeam = getOrRegisterTeam(board, "TNJ_Cheese", org.bukkit.ChatColor.RED);
    }

    private Team getOrRegisterTeam(Scoreboard board, String name, org.bukkit.ChatColor color) {
        Team team = board.getTeam(name);
        if (team == null) team = board.registerNewTeam(name);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        team.setColor(color);
        return team;
    }

    public void startGame(int durationMinutes) {
        if (gameState != GameState.WAITING) return;

        this.gameState = GameState.IN_GAME;
        this.timeLeftSeconds = durationMinutes * 60;
        this.plugin.getDataManager().clearAllData();
        this.plugin.getAbilityListener().resetCooldowns();
        if (plugin.getPhaseManager() != null) plugin.getPhaseManager().reset();

        cheeseStates.clear();
        activeCheeseLocations.clear();
        for (Location loc : plugin.getConfigManager().getCheeseLocations()) {
            cheeseStates.put(loc, loc.getBlock().getState());
        }

        spawnCheeseSubset();

        Location tomSpawn = plugin.getConfigManager().getTomLocation();
        List<Location> jerrySpawns = plugin.getConfigManager().getJerryLocations();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("tnj.exclude") && !player.hasPermission("tnj.tom")) continue;

            plugin.getDataManager().getOrCreateData(player);
            setupPlayerAttributes(player);
            timerBar.addPlayer(player);

            player.getInventory().clear();
            player.setGameMode(GameMode.ADVENTURE);

            if (player.hasPermission("tnj.tom")) {
                tomTeam.addEntry(player.getName());
                if (tomSpawn != null) player.teleport(tomSpawn);

                player.getInventory().addItem(
                        new ItemBuilder(Material.IRON_SWORD).name(mm.deserialize("<red><bold>Claws</bold></red>")).unbreakable()
                                .lore(mm.deserialize("<gray>A basic melee attack.</gray>"), mm.deserialize("<gray>Deals exactly 1 Heart to Jerrys.</gray>")).build(),
                        new ItemBuilder(Material.TORCH).name(mm.deserialize("<yellow><bold>Highlight</bold></yellow>")).addTag(plugin, "item_type", "highlight")
                                .lore(mm.deserialize("<gray>Highlights nearby Jerrys for 10s.</gray>"), mm.deserialize("<gray>Cooldown: 90s</gray>")).build(),
                        new ItemBuilder(Material.TNT).name(mm.deserialize("<dark_red><bold>Kaboom</bold></dark_red>")).addTag(plugin, "item_type", "kaboom")
                                .lore(mm.deserialize("<gray>Enlarges all Jerrys for 8s.</gray>"), mm.deserialize("<gray>Cooldown: 180s</gray>")).build(),
                        new ItemBuilder(Material.INK_SAC).name(mm.deserialize("<dark_gray><bold>Blinding</bold></dark_gray>")).addTag(plugin, "item_type", "blinding")
                                .lore(mm.deserialize("<gray>Blinds all Jerrys for 5s.</gray>"), mm.deserialize("<gray>Cooldown: 120s</gray>")).build()
                );
            } else {
                jerryTeam.addEntry(player.getName());
                if (!jerrySpawns.isEmpty()) {
                    Location randomJerrySpawn = jerrySpawns.get((int) (Math.random() * jerrySpawns.size()));
                    player.teleport(randomJerrySpawn);
                }

                player.getInventory().addItem(
                        new ItemBuilder(Material.BLAZE_ROD).name(mm.deserialize("<gold><bold>Stunner</bold></gold>")).addTag(plugin, "item_type", "stunner")
                                .lore(mm.deserialize("<gray>Slows Tom for 3 seconds.</gray>"), mm.deserialize("<yellow>Cost: 2 Cheese</yellow>"), mm.deserialize("<gray>Cooldown: 45s</gray>")).build(),
                        new ItemBuilder(Material.ENDER_PEARL).name(mm.deserialize("<dark_purple><bold>Swapper</bold></dark_purple>")).addTag(plugin, "item_type", "swapper")
                                .lore(mm.deserialize("<gray>Teleport to a random room.</gray>"), mm.deserialize("<yellow>Cost: 10 Cheese</yellow>"), mm.deserialize("<gray>Cooldown: 30s</gray>")).build(),
                        new ItemBuilder(Material.COBWEB).name(mm.deserialize("<white><bold>WebShooter</bold></white>")).addTag(plugin, "item_type", "webshooter")
                                .lore(mm.deserialize("<gray>Shoots a 3-second cobweb trap.</gray>"), mm.deserialize("<yellow>Cost: 7 Cheese</yellow>"), mm.deserialize("<gray>Cooldown: 60s</gray>")).build()
                );
            }
        }

        plugin.getPowerupManager().startTasks();
        new SneakManager(plugin).startTask();

        Bukkit.broadcast(mm.deserialize("<bold><gradient:#FFB6C1:#FF69B4>Tom & Jerry Event has started!</gradient></bold>"));

        this.timerTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (gameState == GameState.PAUSED) return;

            if (timeLeftSeconds <= 0) {
                endGame();
                return;
            }

            if (plugin.getPhaseManager() != null) {
                plugin.getPhaseManager().tick(timeLeftSeconds, durationMinutes);
            }

            timeLeftSeconds--;

            if (timeLeftSeconds > 0 && timeLeftSeconds != durationMinutes * 60) {
                if (timeLeftSeconds % 600 == 0) {
                    Bukkit.broadcast(mm.deserialize("<yellow>Time Remaining: " + (timeLeftSeconds / 60) + " minutes!</yellow>"));
                } else if (timeLeftSeconds == 300) {
                    Bukkit.broadcast(mm.deserialize("<yellow>Time Remaining: 5 minutes!</yellow>"));
                } else if (timeLeftSeconds == 120) {
                    Bukkit.broadcast(mm.deserialize("<yellow>Time Remaining: 2 minutes!</yellow>"));
                } else if (timeLeftSeconds == 60) {
                    Bukkit.broadcast(mm.deserialize("<red><bold>Time Remaining: 1 minute!</bold></red>"));
                } else if (timeLeftSeconds == 30) {
                    Bukkit.broadcast(mm.deserialize("<red><bold>Time Remaining: 30 seconds!</bold></red>"));
                }
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("tnj.tom") || p.hasPermission("tnj.exclude")) continue;
                JerryData data = plugin.getDataManager().getData(p.getUniqueId());
                if (data != null && !data.isCaged()) {
                    int secondsSurviving = survivalSeconds.getOrDefault(p.getUniqueId(), 0) + 1;
                    if (secondsSurviving >= 900) {
                        data.addPoints(5);
                        data.addSurvivalBonus();
                        p.sendMessage(mm.deserialize("<gold>+5 Points for surviving 15 minutes consecutively!</gold>"));
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        survivalSeconds.put(p.getUniqueId(), 0);
                    } else {
                        survivalSeconds.put(p.getUniqueId(), secondsSurviving);
                    }
                }
            }

            int mins = timeLeftSeconds / 60;
            int secs = timeLeftSeconds % 60;
            timerBar.setTitle("Time Left: " + String.format("%02d:%02d", mins, secs));
            timerBar.setProgress((double) timeLeftSeconds / (durationMinutes * 60));

            int countdownStart = plugin.getConfig().getInt("settings.countdown.seconds", 10);
            if (timeLeftSeconds <= countdownStart && timeLeftSeconds > 0) {
                String titleFormat = plugin.getConfig().getString("messages.countdown_title", "<red><bold><time></bold></red>");
                Component titleComp = mm.deserialize(titleFormat, Placeholder.unparsed("time", String.valueOf(timeLeftSeconds)));

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.showTitle(Title.title(titleComp, Component.empty(), Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                }
            }

        }, 20L, 20L).getTaskId();
    }

    public void pauseGame() {
        if (gameState == GameState.IN_GAME) {
            gameState = GameState.PAUSED;
            Bukkit.broadcast(mm.deserialize(plugin.getConfig().getString("messages.game_paused", "<yellow>Game Paused!</yellow>")));
            timerBar.setColor(BarColor.RED);
            timerBar.setTitle("GAME PAUSED");
        }
    }

    public void spawnCheeseSubset() {
        for (Location loc : cheeseStates.keySet()) {
            loc.getBlock().setType(Material.AIR);
        }
        activeCheeseLocations.clear();

        List<Location> allLocations = new ArrayList<>(cheeseStates.keySet());
        Collections.shuffle(allLocations);

        int limit = Math.min(120, allLocations.size());

        for (int i = 0; i < limit; i++) {
            Location loc = allLocations.get(i);
            cheeseStates.get(loc).update(true, false);

            if (loc.getBlock().getState() instanceof Skull skull && cheeseProfile != null) {
                skull.setOwnerProfile(cheeseProfile);
                skull.update(true, false);
            }
            activeCheeseLocations.add(loc);
        }

        Bukkit.broadcast(mm.deserialize("<gold><bold>CHEESE RESPAWN!</bold> 120 new cheese blocks have appeared in the house!</gold>"));
    }

    public void resumeGame() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.IN_GAME;
            Bukkit.broadcast(mm.deserialize(plugin.getConfig().getString("messages.game_resumed", "<green>Game Resumed!</green>")));
            timerBar.setColor(BarColor.PINK);
        }
    }

    public void cageJerry(Player player, JerryData data) {
        cageJerry(player, data, true);
    }

    public void cageJerry(Player player, JerryData data, boolean deductPoints) {
        data.setCaged(true);
        data.setTimeCaged(System.currentTimeMillis());

        if (deductPoints) {
            data.addPoints(-5);
        }

        survivalSeconds.put(player.getUniqueId(), 0);

        Location cageLoc = plugin.getConfigManager().getCageLocation();
        if (cageLoc == null) cageLoc = player.getWorld().getSpawnLocation();

        player.teleport(cageLoc);

        if (deductPoints) {
            player.sendMessage(mm.deserialize("<red><bold>YOU HAVE BEEN CAUGHT!</bold> You lost 5 points and will be freed when enough Jerrys are in the cage.</red>"));
        } else {
            player.sendMessage(mm.deserialize("<red><bold>CAUGHT BY TRAP!</bold> You will be freed when enough Jerrys are in the cage. (No points lost!)</red>"));
        }

        checkCageRelease();
    }

    private void checkCageRelease() {
        List<JerryData> cagedMice = new ArrayList<>();
        for (JerryData data : plugin.getDataManager().getAllData()) {
            if (data.isCaged()) {
                cagedMice.add(data);
            }
        }

        int threshold = plugin.getPhaseManager() != null ? plugin.getPhaseManager().getCageThreshold() : 5;

        if (cagedMice.size() >= threshold) {
            int releaseCount = threshold - 1;
            Bukkit.broadcast(mm.deserialize("<green><bold>JAILBREAK!</bold> First " + releaseCount + " Jerrys have broken out!</green>"));

            cagedMice.sort(java.util.Comparator.comparingLong(JerryData::getTimeCaged));

            List<Location> safeSpots = plugin.getConfigManager().getJerryLocations();
            if (safeSpots.isEmpty()) safeSpots = new ArrayList<>(plugin.getConfigManager().getCheeseLocations());
            Location fallbackLoc = plugin.getConfigManager().getLobbyLocation();

            for (int i = 0; i < releaseCount; i++) {
                JerryData data = cagedMice.get(i);
                data.setCaged(false);

                Player p = Bukkit.getPlayer(data.getUuid());
                if (p != null && p.isOnline()) {
                    p.setHealth(6.0);

                    if (!safeSpots.isEmpty()) {
                        p.teleport(safeSpots.get((int) (Math.random() * safeSpots.size())));
                    } else if (fallbackLoc != null) {
                        p.teleport(fallbackLoc);
                    } else {
                        p.teleport(p.getWorld().getSpawnLocation());
                    }

                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);
                }
            }
        }
    }

    private void endGame() {
        this.gameState = GameState.ENDING;
        if (timerTaskId != -1) Bukkit.getScheduler().cancelTask(timerTaskId);
        plugin.getPowerupManager().stopTasksAndCleanup();

        int survivalPoints = plugin.getConfig().getInt("settings.points.survival-bonus", 5);

        for (JerryData data : plugin.getDataManager().getAllData()) {
            Player p = Bukkit.getPlayer(data.getUuid());
            if (p != null && p.hasPermission("tnj.tom")) continue;

            if (!data.isCaged()) {
                data.addPoints(survivalPoints);
                if (p != null) p.sendMessage(mm.deserialize("<gold>+" + survivalPoints + " Points for surviving!</gold>"));
            }
        }

        List<JerryData> leaderboard = plugin.getDataManager().getAllData().stream()
                .sorted((a, b) -> Integer.compare(b.getPoints(), a.getPoints()))
                .collect(Collectors.toList());

        plugin.getDataManager().saveMatchResults();

        Bukkit.broadcast(mm.deserialize(plugin.getConfig().getString("messages.top_header", "\n<yellow>Top Mice</yellow>")));

        int displayCount = Math.min(10, leaderboard.size());
        for (int i = 0; i < displayCount; i++) {
            JerryData data = leaderboard.get(i);
            Player player = Bukkit.getPlayer(data.getUuid());
            if (player != null) player.setGameMode(GameMode.SPECTATOR);

            String format = plugin.getConfig().getString("messages.top_format", "<gray><rank>.</gray> <yellow><player></yellow> <dark_gray>-</dark_gray> <white><score> Points</white> <gray>(<survival> Survival | <cheese> Cheese)</gray>");
            Bukkit.broadcast(mm.deserialize(format,
                    Placeholder.unparsed("rank", String.valueOf(i + 1)),
                    Placeholder.unparsed("player", data.getName()),
                    Placeholder.unparsed("score", String.valueOf(data.getPoints())),
                    Placeholder.unparsed("survival", String.valueOf(data.getSurvivalBonuses() * 5)),
                    Placeholder.unparsed("cheese", String.valueOf(data.getTotalCheeseCollected()))
            ));
        }
        Bukkit.broadcast(mm.deserialize(plugin.getConfig().getString("messages.top_footer", "\n")));
        Bukkit.getScheduler().runTaskLater(plugin, this::forceStopGame, 200L);
    }

    public void forceStopGame() {
        if (timerTaskId != -1) Bukkit.getScheduler().cancelTask(timerTaskId);
        timerTaskId = -1;
        timerBar.removeAll();

        boolean wasActive = (gameState == GameState.IN_GAME || gameState == GameState.PAUSED);
        this.gameState = GameState.WAITING;

        if (wasActive) {
            plugin.getDataManager().saveMatchResults();
        }

        plugin.getPowerupManager().stopTasksAndCleanup();
        if (plugin.getPhaseManager() != null) plugin.getPhaseManager().reset();
        if (plugin.getTrapManager() != null) plugin.getTrapManager().clearAllTraps();
        if (plugin.getGoldenCheeseManager() != null) plugin.getGoldenCheeseManager().endPhase();

        for (Map.Entry<Location, BlockState> entry : cheeseStates.entrySet()) {
            Location loc = entry.getKey();
            entry.getValue().update(true, false);

            if (loc.getBlock().getState() instanceof Skull skull && cheeseProfile != null) {
                skull.setOwnerProfile(cheeseProfile);
                skull.update(true, false);
            }
        }
        cheeseStates.clear();
        activeCheeseLocations.clear();

        resetAllPlayers();

        Location lobby = plugin.getConfigManager().getLobbyLocation();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (lobby != null) {
                p.teleport(lobby);
            } else {
                p.performCommand("spawn");
            }
        }
    }

    public void setupPlayerAttributes(Player player) {
        AttributeInstance scale = player.getAttribute(Attribute.GENERIC_SCALE);
        AttributeInstance health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        player.setCollidable(false);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        if (scale != null && health != null) {
            if (player.hasPermission("tnj.tom")) {
                scale.setBaseValue(0.75);
                health.setBaseValue(20.0);
                if (player.getHealth() > 20.0) player.setHealth(20.0);

                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 15, 255, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 15, 200, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 15, 0, false, false, false));
            } else {
                scale.setBaseValue(0.25);
                health.setBaseValue(6.0);

                if (player.getHealth() > 6.0) player.setHealth(6.0);

                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 99999, 0, false, false, false));
            }
        }
    }

    private void resetAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            AttributeInstance scale = player.getAttribute(Attribute.GENERIC_SCALE);
            AttributeInstance health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

            if (scale != null) scale.setBaseValue(1.0);
            if (health != null) health.setBaseValue(20.0);

            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            tomTeam.removeEntry(player.getName());
            jerryTeam.removeEntry(player.getName());
            cheeseTeam.removeEntry(player.getName());

            player.getInventory().clear();
            if (player.getGameMode() == GameMode.ADVENTURE) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }

    public GameState getGameState() { return gameState; }
    public int getTimeLeftSeconds() { return timeLeftSeconds; }

    public Set<Location> getActiveCheeseLocations() { return activeCheeseLocations; }
    public boolean isCheeseRespawnEnabled() { return cheeseRespawnEnabled; }
    public void toggleCheeseRespawn() { this.cheeseRespawnEnabled = !this.cheeseRespawnEnabled; }
    public void setTimeLeftSeconds(int seconds) {
        this.timeLeftSeconds = seconds;
    }

    public void releaseJerry(Player p) {
        JerryData data = plugin.getDataManager().getData(p.getUniqueId());
        if (data == null || !data.isCaged()) return;

        data.setCaged(false);
        p.setHealth(6.0);

        List<Location> safeSpots = plugin.getConfigManager().getJerryLocations();
        if (safeSpots.isEmpty()) safeSpots = new ArrayList<>(plugin.getConfigManager().getCheeseLocations());
        Location fallbackLoc = plugin.getConfigManager().getLobbyLocation();

        if (!safeSpots.isEmpty()) {
            p.teleport(safeSpots.get((int) (Math.random() * safeSpots.size())));
        } else if (fallbackLoc != null) {
            p.teleport(fallbackLoc);
        } else {
            p.teleport(p.getWorld().getSpawnLocation());
        }

        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);
        p.sendMessage(mm.deserialize("<green><bold>JAILBREAK!</bold> You have been forcefully released!</green>"));
        Bukkit.broadcast(mm.deserialize("<green>" + p.getName() + " was forcefully released from the cage!</green>"));
    }
}