package me.siwannie.tomnjerry.commands;

import me.siwannie.tomnjerry.TomNJerry;
import me.siwannie.tomnjerry.models.SetupMode;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TomNJerryCommand implements CommandExecutor, TabCompleter {

    private final TomNJerry plugin;
    private final MiniMessage mm;

    public TomNJerryCommand(TomNJerry plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("tnj.admin")) {
            sender.sendMessage(mm.deserialize("<red>You do not have permission.</red>"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "start":
                int minutes = plugin.getConfig().getInt("settings.game-duration-minutes", 60);
                if (args.length > 1) {
                    try { minutes = Integer.parseInt(args[1]); }
                    catch (NumberFormatException e) { sender.sendMessage(mm.deserialize("<red>Invalid time.</red>")); return true; }
                }
                plugin.getGameManager().startGame(minutes);
                sender.sendMessage(mm.deserialize("<green>Started the game for " + minutes + " minutes.</green>"));
                break;

            case "stop":
                plugin.getGameManager().forceStopGame();
                sender.sendMessage(mm.deserialize("<red>Game has been force-stopped.</red>"));
                break;

            case "pause":
                plugin.getGameManager().pauseGame();
                sender.sendMessage(mm.deserialize("<yellow>Game Paused.</yellow>"));
                break;

            case "resume":
                plugin.getGameManager().resumeGame();
                sender.sendMessage(mm.deserialize("<green>Game Resumed.</green>"));
                break;

            case "setlobby":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mm.deserialize("<red>Only players can set the lobby.</red>"));
                    return true;
                }
                plugin.getConfigManager().setLobbyLocation(player.getLocation());
                player.sendMessage(mm.deserialize("<green>Lobby location successfully set to your current position!</green>"));
                break;

            case "forcephase":
                if (args.length < 2) {
                    sender.sendMessage(mm.deserialize("<red>Usage: /tnj forcephase <normal|golden_cage|mastery_traps|spoilage_double></red>"));
                    return true;
                }
                int totalDuration = plugin.getConfig().getInt("settings.game-duration-minutes", 60) * 60;
                int phaseDur = totalDuration / 4;
                int newElapsed = 0;

                switch(args[1].toLowerCase()) {
                    case "normal": newElapsed = 0; break;
                    case "golden_cage": newElapsed = phaseDur; break;
                    case "mastery_traps": newElapsed = phaseDur * 2; break;
                    case "spoilage_double": newElapsed = phaseDur * 3; break;
                    default: sender.sendMessage(mm.deserialize("<red>Invalid phase.</red>")); return true;
                }
                // Sets the timer right before the new phase mathematically triggers
                plugin.getGameManager().setTimeLeftSeconds(totalDuration - newElapsed - 1);
                sender.sendMessage(mm.deserialize("<green>Forced phase change to " + args[1].toUpperCase() + ".</green>"));
                break;

            case "forcerelease":
                if (args.length < 2) {
                    sender.sendMessage(mm.deserialize("<red>Usage: /tnj forcerelease <player></red>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(mm.deserialize("<red>Player not found.</red>"));
                    return true;
                }
                plugin.getGameManager().releaseJerry(target);
                sender.sendMessage(mm.deserialize("<green>Force released " + target.getName() + ".</green>"));
                break;

            case "setup":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mm.deserialize("<red>Only players can use setup mode.</red>"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize("<red>Usage: /tnj setup <add|remove|cage|tom_spawn|jerry_spawn|golden_cheese|none></red>"));
                    return true;
                }
                try {
                    SetupMode mode = SetupMode.valueOf(args[1].toUpperCase());
                    plugin.getSetupManager().setSetupMode(player, mode);
                    player.sendMessage(mm.deserialize("<green>Setup mode set to: " + mode.name() + "</green>"));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(mm.deserialize("<red>Invalid mode.</red>"));
                }
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(mm.deserialize("<yellow>TomNJerry Commands:</yellow>"));
        sender.sendMessage(mm.deserialize("<gray>- /tnj start [minutes]</gray>"));
        sender.sendMessage(mm.deserialize("<gray>- /tnj stop</gray>"));
        sender.sendMessage(mm.deserialize("<gray>- /tnj pause</gray>"));
        sender.sendMessage(mm.deserialize("<gray>- /tnj resume</gray>"));
        sender.sendMessage(mm.deserialize("<gray>- /tnj setlobby</gray>"));
        sender.sendMessage(mm.deserialize("<gray>- /tnj setup <add|remove|cage|tom_spawn|jerry_spawn|golden_cheese|none></gray>"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("tnj.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("start", "stop", "pause", "resume", "setlobby", "setup", "forcephase", "forcerelease");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setup")) {
                return Arrays.asList("add", "remove", "cage", "tom_spawn", "jerry_spawn", "golden_cheese", "none");
            } else if (args[0].equalsIgnoreCase("forcephase")) {
                return Arrays.asList("normal", "golden_cage", "mastery_traps", "spoilage_double");
            } else if (args[0].equalsIgnoreCase("forcerelease")) {
                return null;
            }
        }
        return new ArrayList<>();
    }
}