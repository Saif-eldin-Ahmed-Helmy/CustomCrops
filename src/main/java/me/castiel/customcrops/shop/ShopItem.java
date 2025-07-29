package me.castiel.customcrops.shop;

import org.bukkit.Material;

import java.util.List;

public class ShopItem {
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final int price;
    private final boolean glow;
    private final RotatingRarity rarity;
    private final List<String> actions;

    public ShopItem(Material material, String name, List<String> lore, int price, boolean glow, RotatingRarity rarity, List<String> actions) {
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.price = price;
        this.glow = glow;
        this.rarity = rarity;
        this.actions = actions;
    }

    public Material getMaterial() { return material; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public int getPrice() { return price; }
    public boolean isGlow() { return glow; }
    public RotatingRarity getRarity() { return rarity; }
    public List<String> getActions() { return actions; }
}