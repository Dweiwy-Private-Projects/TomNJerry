package me.siwannie.tomnjerry.models;

public enum GamePhase {
    NORMAL("Normal Rules"),
    GOLDEN_CAGE("Golden Cheese & Reinforced Cage"),
    MASTERY_TRAPS("Mice Mastery & Mouse Traps"),
    SPOILAGE_DOUBLE("Cheese Spoilage & Double Cheese");

    private final String displayName;

    GamePhase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}