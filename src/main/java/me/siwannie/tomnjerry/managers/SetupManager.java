package me.siwannie.tomnjerry.managers;

import me.siwannie.tomnjerry.models.SetupMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetupManager {

    private final Map<UUID, SetupMode> staffModes = new HashMap<>();

    public void setSetupMode(Player player, SetupMode mode) {
        if (mode == SetupMode.NONE) {
            staffModes.remove(player.getUniqueId());
        } else {
            staffModes.put(player.getUniqueId(), mode);
        }
    }

    public SetupMode getSetupMode(Player player) {
        return staffModes.getOrDefault(player.getUniqueId(), SetupMode.NONE);
    }

    public boolean isInSetupMode(Player player) {
        return getSetupMode(player) != SetupMode.NONE;
    }
}