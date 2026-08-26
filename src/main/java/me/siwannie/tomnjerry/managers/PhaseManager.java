package me.siwannie.tomnjerry.managers;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.GamePhase;
import me.siwannie.tomnjerry.models.JerryData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

public class PhaseManager {

    private final TomNJerry plugin;
    private final MiniMessage mm;
    private GamePhase currentPhase;
    private boolean doubleCheeseActive = false;
    private int spoliageTaskId = -1;

    public PhaseManager(TomNJerry plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
        this.currentPhase = GamePhase.NORMAL;
    }

    public void reset() {
        this.currentPhase = GamePhase.NORMAL;
        this.doubleCheeseActive = false;
        if (spoliageTaskId != -1) Bukkit.getScheduler().cancelTask(spoliageTaskId);
        spoliageTaskId = -1;
    }

    public void tick(int timeLeftSeconds, int durationMinutes) {
        int totalSeconds = durationMinutes * 60;
        int elapsedSeconds = totalSeconds - timeLeftSeconds;

        GamePhase newPhase = determinePhase(elapsedSeconds, totalSeconds);

        if (newPhase != currentPhase) {
            transitionPhase(newPhase);
        }

        if (currentPhase == GamePhase.SPOILAGE_DOUBLE) {
            handleSpoilageAndDoubleCheese(elapsedSeconds);
        }
    }

    private GamePhase determinePhase(int elapsedSeconds, int totalSeconds) {
        int phaseDuration = totalSeconds / 4; // 15 mins if 60 min game

        if (elapsedSeconds < phaseDuration) return GamePhase.NORMAL;
        if (elapsedSeconds < phaseDuration * 2) return GamePhase.GOLDEN_CAGE;
        if (elapsedSeconds < phaseDuration * 3) return GamePhase.MASTERY_TRAPS;
        return GamePhase.SPOILAGE_DOUBLE;
    }

    private void transitionPhase(GamePhase newPhase) {
        this.currentPhase = newPhase;

        switch (newPhase) {
            case NORMAL:
                break;
            case GOLDEN_CAGE:
                Bukkit.broadcast(mm.deserialize("\n<gold><st>                                                 </st>\n<bold> EVENT PHASE: GOLDEN CAGE</bold>\n \n A rare golden cheese has appeared! (+5 Cheese)\n Tom has reinforced the cage! (10 Mice to open!)\n<st>                                                 </st>\n"));
                plugin.getGoldenCheeseManager().startPhase();
                playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL);
                sendTitleToAll("<gold><bold>GOLDEN CAGE</bold></gold>", "<yellow>Golden Cheese & Reinforced Cage!</yellow>");
                break;
            case MASTERY_TRAPS:
                plugin.getGoldenCheeseManager().endPhase();
                Bukkit.broadcast(mm.deserialize("\n<aqua><st>                                                 </st>\n<bold> EVENT PHASE: MASTERY & TRAPS</bold>\n \n The mice have learned new tricks! (Abilities 50% off!)\n Tom has set the traps! Watch your step!\n<st>                                                 </st>\n"));
                plugin.getTrapManager().startPhase();
                playSoundToAll(Sound.ENTITY_WITHER_SPAWN);
                sendTitleToAll("<aqua><bold>MASTERY TRAPS</bold></aqua>", "<dark_red>Traps set!</dark_red> <gray>|</gray> <aqua>Abilities cheaper!</aqua>");
                break;
            case SPOILAGE_DOUBLE:
                plugin.getTrapManager().endPhase();
                Bukkit.broadcast(mm.deserialize("\n<dark_green><st>                                                 </st>\n<bold> EVENT PHASE: SPOILAGE</bold>\n \n Tom can sniff Jerrys holding 25+ cheese!\n Random Double Cheese cycles are active!\n<st>                                                 </st>\n"));
                startSpoilageTask();
                playSoundToAll(Sound.ENTITY_WOLF_HOWL);
                sendTitleToAll("<dark_green><bold>CHEESE SPOILAGE</bold></dark_green>", "<yellow>Hold less than 10 cheese!</yellow>");
                break;
        }
    }

    private void sendTitleToAll(String title, String subtitle) {
        net.kyori.adventure.title.Title t = net.kyori.adventure.title.Title.title(
                mm.deserialize(title),
                mm.deserialize(subtitle),
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofSeconds(4), java.time.Duration.ofMillis(500))
        );
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(t);
        }
    }

    private void handleSpoilageAndDoubleCheese(int elapsedSeconds) {
        // 5 minute cycle: 2 mins ON, 3 mins OFF
        int cycleTime = elapsedSeconds % 300;

        boolean shouldBeActive = cycleTime < 120;

        if (shouldBeActive && !doubleCheeseActive) {
            doubleCheeseActive = true;
            Bukkit.broadcast(mm.deserialize("<yellow><bold>DOUBLE CHEESE:</bold> The house owner dropped a giant wheel of cheese! Every piece counts double for 2 minutes!</yellow>"));
            playSoundToAll(Sound.BLOCK_BELL_USE);
        } else if (!shouldBeActive && doubleCheeseActive) {
            doubleCheeseActive = false;
            Bukkit.broadcast(mm.deserialize("<gray><bold>DOUBLE CHEESE:</bold> Cheese yields have returned to normal.</gray>"));
        }
    }

    private void startSpoilageTask() {
        if (spoliageTaskId != -1) return;
        spoliageTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("tnj.tom") || player.hasPermission("tnj.exclude")) continue;

                JerryData data = plugin.getDataManager().getData(player.getUniqueId());
                Team cheeseTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("TNJ_Cheese");
                Team jerryTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("TNJ_Jerry");

                if (data != null && data.getCheeseHeld() >= 25 && !data.isCaged()) {
                    if (cheeseTeam != null) cheeseTeam.addEntry(player.getName());

                    PotionEffect glowing = player.getPotionEffect(PotionEffectType.GLOWING);
                    if (glowing == null || glowing.getDuration() <= 60) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false, false));
                    }
                } else {
                    if (jerryTeam != null && !player.hasPermission("tnj.tom")) {
                        jerryTeam.addEntry(player.getName());
                    }
                }
            }
        }, 20L, 20L).getTaskId();
    }

    private void playSoundToAll(Sound sound) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    public GamePhase getCurrentPhase() { return currentPhase; }

    public int getCageThreshold() {
        return currentPhase == GamePhase.GOLDEN_CAGE ? 10 : 5;
    }

    public int getAbilityCost(int baseCost) {
        if (currentPhase == GamePhase.MASTERY_TRAPS) {
            return (int) Math.ceil(baseCost / 2.0);
        }
        return baseCost;
    }

    public boolean isDoubleCheeseActive() {
        return doubleCheeseActive;
    }
}