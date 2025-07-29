package me.castiel.customcrops.shop;

import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.util.DateUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RotatingShopManager {

    private final CustomCropsPlugin plugin;
    private final List<ShopItem> items = new ArrayList<>();
    private final List<ShopItem> currentRotation = new ArrayList<>();
    private final List<Integer> slots = new ArrayList<>();
    private final Map<String, RotatingRarity> rarities = new HashMap<>();

    public RotatingShopManager(CustomCropsPlugin plugin) {
        this.plugin = plugin;
        loadRarities();
        loadItems();
        loadSlots();
        rotateItems();
        scheduleRotation();
    }

    private void loadItems() {
        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("RotatingShop.ShopItems");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                Material material = Material.valueOf(itemSection.getString("Material", "STONE"));
                String name = itemSection.getString("Name", "");
                List<String> lore = itemSection.getStringList("Lore");
                int price = itemSection.getInt("Price", 0);
                boolean glow = itemSection.getBoolean("Glow", false);

                String rarityKey = itemSection.getString("Rarity", "Common");
                RotatingRarity rarity = rarities.getOrDefault(rarityKey, new RotatingRarity("Common", "&aCommon", 100));
                List<String> actions = itemSection.getStringList("Actions");

                items.add(new ShopItem(material, name, lore, price, glow, rarity, actions));
            }
        }
    }

    private void loadRarities() {
        ConfigurationSection raritiesSection = plugin.getConfig().getConfigurationSection("RotatingShop.Rarities");
        if (raritiesSection != null) {
            for (String key : raritiesSection.getKeys(false)) {
                String display = raritiesSection.getString(key + ".DisplayName", key);
                int chance = raritiesSection.getInt(key + ".Chance", 0);
                rarities.put(key, new RotatingRarity(key, display, chance));
            }
        }
    }


    private void loadSlots() {
        slots.addAll(plugin.getConfig().getIntegerList("RotatingShop.Slots"));
    }

    private void scheduleRotation() {
        long nextRotationTime = getNextRotationTime();
        new BukkitRunnable() {
            @Override
            public void run() {
               rotateItems();
               scheduleRotation();
            }
        }.runTaskLaterAsynchronously(plugin, (nextRotationTime / 50L) + 1L);
    }

    private void rotateItems() {
        currentRotation.clear();
        List<ShopItem> pool = new ArrayList<>(items);
        Random random = new Random();

        while (currentRotation.size() < slots.size() && !pool.isEmpty()) {
            ShopItem chosen = getWeightedRandomItem(pool, random);
            if (chosen != null && !currentRotation.contains(chosen)) {
                currentRotation.add(chosen);
            }
        }
    }

    private ShopItem getWeightedRandomItem(List<ShopItem> pool, Random random) {
        int totalWeight = pool.stream().mapToInt(item -> item.getRarity().getChance()).sum();
        int r = random.nextInt(totalWeight);
        int count = 0;

        for (ShopItem item : pool) {
            count += item.getRarity().getChance();
            if (r < count) {
                return item;
            }
        }
        return null;
    }

    public List<ShopItem> getCurrentRotation() {
        return currentRotation;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public long getNextRotationTime() {
        List<String> resetTimes = plugin.getConfig().getStringList("RotatingShop.ResetTimes");
        return DateUtils.getMillisecondsUntilRotation(resetTimes);
    }
}