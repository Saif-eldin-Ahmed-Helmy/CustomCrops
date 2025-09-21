package me.castiel.customcrops.shop;

import org.bukkit.Material;

import java.util.List;

public class ShopItem {
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final double price;
    private final boolean glow;
    private final RotatingRarity rarity;
    private final List<String> actions;
    private final int personalStock;
    private final int globalStock;
    private final int requiredLevel;
    private final double chance; // additional per-item chance multiplier

    public ShopItem(Material material, String name, List<String> lore, double price, boolean glow, RotatingRarity rarity, List<String> actions, int personalStock, int globalStock, int requiredLevel, double chance) {
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.price = price;
        this.glow = glow;
        this.rarity = rarity;
        this.actions = actions;
        this.personalStock = personalStock;
        this.globalStock = globalStock;
        this.requiredLevel = requiredLevel;
        this.chance = chance;
    }

    public Material getMaterial() { return material; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public double getPrice() { return price; }
    public boolean isGlow() { return glow; }
    public RotatingRarity getRarity() { return rarity; }
    public List<String> getActions() { return actions; }

    public int getPersonalStock() {
        return personalStock;
    }

    public int getGlobalStock() {
        return globalStock;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public double getChance() { return chance; }
}