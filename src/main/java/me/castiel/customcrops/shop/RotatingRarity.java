package me.castiel.customcrops.shop;

public class RotatingRarity {
    private final String id;
    private final String displayName;
    private final double chance;

    public RotatingRarity(String id, String displayName, double chance) {
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

    public double getChance() {
        return chance;
    }
}