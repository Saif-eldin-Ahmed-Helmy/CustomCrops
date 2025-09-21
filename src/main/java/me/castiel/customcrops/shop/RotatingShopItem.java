package me.castiel.customcrops.shop;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RotatingShopItem extends ShopItem {

    private final Map<UUID, Integer> playerPurchases;

    public RotatingShopItem(Material material, String name, List<String> lore, double price, boolean glow, RotatingRarity rarity, List<String> actions, int personalStock, int globalStock, int requiredLevel, double chance) {
        super(material, name, lore, price, glow, rarity, actions, personalStock, globalStock, requiredLevel, chance);
        playerPurchases = new HashMap<>();
    }

    public RotatingShopItem(Material material, String name, List<String> lore, double price, boolean glow, RotatingRarity rarity, List<String> actions, int personalStock, int globalStock, int requiredLevel, HashMap<UUID, Integer> playerPurchases, double chance) {
        super(material, name, lore, price, glow, rarity, actions, personalStock, globalStock, requiredLevel, chance);
        this.playerPurchases = playerPurchases;
    }

    public Map<UUID, Integer> getPlayerPurchases() {
        return playerPurchases;
    }

    public void addPurchase(UUID playerId) {
        playerPurchases.put(playerId, playerPurchases.getOrDefault(playerId, 0) + 1);
    }

    public int getPlayerPurchaseCount(UUID playerId) {
        return playerPurchases.getOrDefault(playerId, 0);
    }
}