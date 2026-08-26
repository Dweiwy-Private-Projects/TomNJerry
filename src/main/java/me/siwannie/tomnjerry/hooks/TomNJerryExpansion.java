package me.siwannie.tomnjerry.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.JerryData;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TomNJerryExpansion extends PlaceholderExpansion {

    private final TomNJerry plugin;

    public TomNJerryExpansion(TomNJerry plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "tnj"; }

    @Override
    public @NotNull String getAuthor() { return "Siwannie"; }

    @Override
    public @NotNull String getVersion() { return "2.0.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (params.equalsIgnoreCase("status")) {
            return plugin.getGameManager().getGameState().name();
        }

        if (params.equalsIgnoreCase("time_left")) {
            int totalSeconds = plugin.getGameManager().getTimeLeftSeconds();
            int mins = totalSeconds / 60;
            int secs = totalSeconds % 60;
            return String.format("%02d:%02d", mins, secs);
        }

        if (params.equalsIgnoreCase("phase_name")) {
            if (plugin.getPhaseManager() != null && plugin.getPhaseManager().getCurrentPhase() != null) {
                return plugin.getPhaseManager().getCurrentPhase().getDisplayName();
            }
            return "Normal";
        }

        if (params.equalsIgnoreCase("phase_effect_1")) {
            if (plugin.getPhaseManager() == null) return "";
            return switch (plugin.getPhaseManager().getCurrentPhase()) {
                case GOLDEN_CAGE -> "Golden Cheese spawns";
                case MASTERY_TRAPS -> "Abilities are 50% cheaper";
                case SPOILAGE_DOUBLE -> plugin.getPhaseManager().isDoubleCheeseActive() ? "&aDouble Cheese ACTIVE!" : "&cDouble Cheese INACTIVE";
                default -> "";
            };
        }

        if (params.equalsIgnoreCase("phase_effect_2")) {
            if (plugin.getPhaseManager() == null) return "";
            return switch (plugin.getPhaseManager().getCurrentPhase()) {
                case GOLDEN_CAGE -> "Cage requires 10 mice to open";
                case MASTERY_TRAPS -> "Toms can place Mouse Traps";
                case SPOILAGE_DOUBLE -> "10+ Cheese = Highlighted";
                default -> "";
            };
        }

        if (params.startsWith("top_name_") || params.startsWith("top_score_")) {
            List<JerryData> lb = plugin.getDataManager().getLeaderboard();
            try {
                String[] parts = params.split("_");
                int rank = Integer.parseInt(parts[2]);

                if (rank > 0 && rank <= 15) {
                    if (rank <= lb.size()) {
                        JerryData data = lb.get(rank - 1);
                        return params.startsWith("top_name_") ? data.getName() : String.valueOf(data.getPoints());
                    }
                }
            } catch (Exception ignored) {}
            return params.startsWith("top_name_") ? "N/A" : "0";
        }

        if (offlinePlayer != null && offlinePlayer.isOnline()) {
            Player player = offlinePlayer.getPlayer();
            JerryData data = plugin.getDataManager().getData(player.getUniqueId());

            if (params.equalsIgnoreCase("role")) {
                if (player.hasPermission("tnj.tom")) return "Tom";
                if (player.hasPermission("tnj.exclude")) return "Spectator";
                return "Jerry";
            }

            if (data != null) {
                if (params.equalsIgnoreCase("points")) return String.valueOf(data.getPoints());
                if (params.equalsIgnoreCase("cheese_held")) return String.valueOf(data.getCheeseHeld());
                if (params.equalsIgnoreCase("health")) return String.valueOf((int) player.getHealth());
                if (params.equalsIgnoreCase("is_caged")) return data.isCaged() ? "Yes" : "No";

                if (params.equalsIgnoreCase("survival_bonuses")) return String.valueOf(data.getSurvivalBonuses());
                if (params.equalsIgnoreCase("total_cheese")) return String.valueOf(data.getTotalCheeseCollected());

                if (params.equalsIgnoreCase("stunner_cooldown")) {
                    return String.valueOf(data.getStunnerCooldownRemaining() / 1000);
                }
            }
        }
        return "";
    }
}