package me.castiel.customcrops.shop;

public class RotatingRarity {
    private final String id;
    private final String displayName;
    private final int chance;

    public RotatingRarity(String id, String displayName, int chance) {
        this.id = id;
        this.displayName = displayName;
        this.chance = chance;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getChance() {
        return chance;
    }
}