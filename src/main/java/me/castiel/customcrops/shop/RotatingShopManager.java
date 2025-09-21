package me.castiel.customcrops.shop;

import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.storage.CropDAO;
import me.castiel.customcrops.util.DateUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RotatingShopManager {

    private final CustomCropsPlugin plugin;
    private final CropDAO cropDAO;
    private final List<ShopItem> items = new ArrayList<>();
    private final List<RotatingShopItem> currentRotation = new ArrayList<>();
    private final List<Integer> slots = new ArrayList<>();
    private final Map<String, RotatingRarity> rarities = new HashMap<>();
    private final ConcurrentHashMap<String, Integer> playersHighestRequirements;
    private final ShopAutoSaveTask shopAutoSaveTask;

    public RotatingShopManager(CustomCropsPlugin plugin, CropDAO cropDAO) {
        this.plugin = plugin;
        this.cropDAO = cropDAO;
        loadRarities();
        loadItems();
        loadSlots();
        rotateItems();
        scheduleRotation();
        this.shopAutoSaveTask = new ShopAutoSaveTask(plugin, this);
        this.playersHighestRequirements = new ConcurrentHashMap<>();
    }

    public void loadData() {
        cropDAO.loadAllHighestRequirements().whenCompleteAsync((playersRequirements, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().severe("Failed to load shop player data: " + throwable.getMessage());
                return;
            }
            playersHighestRequirements.putAll(playersRequirements);
            plugin.getLogger().info("Loaded player data for rotating shop.");
        });
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
                int personalStock = itemSection.getInt("PersonalStock", -1);
                int globalStock = itemSection.getInt("GlobalStock", -1);
                int requiredLevel = itemSection.getInt("RequiredLevel", 0);
                double itemChance = itemSection.getDouble("Chance", 1.0);

                items.add(new ShopItem(material, name, lore, price, glow, rarity, actions, personalStock, globalStock, requiredLevel, itemChance));
            }
        }
    }

    private void loadRarities() {
        ConfigurationSection raritiesSection = plugin.getConfig().getConfigurationSection("RotatingShop.Rarities");
        if (raritiesSection != null) {
            for (String key : raritiesSection.getKeys(false)) {
                String display = raritiesSection.getString(key + ".DisplayName", key);
                double chance = raritiesSection.getDouble(key + ".Chance", 0);
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

        while (currentRotation.size() < slots.size() && items.size() - currentRotation.size() > 0) {
            ShopItem chosen = getWeightedRandomItem(pool, random);
            if (chosen != null && currentRotation.stream().noneMatch(item -> item.getMaterial() == chosen.getMaterial())) {
                RotatingShopItem rotatingItem = new RotatingShopItem(
                        chosen.getMaterial(),
                        chosen.getName(),
                        chosen.getLore(),
                        chosen.getPrice(),
                        chosen.isGlow(),
                        chosen.getRarity(),
                        chosen.getActions(),
                        chosen.getPersonalStock(),
                        chosen.getGlobalStock(),
                        chosen.getRequiredLevel(),
                        chosen.getChance()
                );
                currentRotation.add(rotatingItem);
            }
        }
    }

    private ShopItem getWeightedRandomItem(List<ShopItem> pool, Random random) {
        double totalWeight = pool.stream()
                .mapToDouble(item -> item.getRarity().getChance() * Math.max(0.0, item.getChance()))
                .sum();

        if (totalWeight <= 0) {
            return null; // no valid items
        }

        double r = random.nextDouble() * totalWeight; // random in [0, totalWeight)
        double count = 0.0;

        for (ShopItem item : pool) {
            count += item.getRarity().getChance() * Math.max(0.0, item.getChance());
            if (r < count) {
                return item;
            }
        }

        return null; // shouldn't happen if totalWeight > 0
    }

    public List<RotatingShopItem> getCurrentRotation() {
        return currentRotation;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public long getNextRotationTime() {
        List<String> resetTimes = plugin.getConfig().getStringList("RotatingShop.ResetTimes");
        return DateUtils.getMillisecondsUntilRotation(resetTimes);
    }

    public int getPlayerHighestRequirement(String playerId) {
        return playersHighestRequirements.getOrDefault(playerId, 0);
    }

    public void saveRequirements(boolean async) {
        if (async) {
            cropDAO.saveHighestRequirements(playersHighestRequirements).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().severe("Failed to save shop player data: " + throwable.getMessage());
                }
                //else {
                    // plugin.getLogger().info("Saved player data for rotating shop.");
                //}
            });
        } else {
            cropDAO.saveHighestRequirementsSync(playersHighestRequirements);
        }
    }

    public void stopAutoSave() {
        shopAutoSaveTask.stop();
    }

    public void setHighestRequirement(String string, int requiredLevel) {
        playersHighestRequirements.put(string, requiredLevel);
    }

    // Expose a public manual rotate method
    public void rotateNow() {
        rotateItems();
        plugin.getLogger().info("Rotating shop items have been rotated manually.");
    }

    // Reload rarities, items, and slots from the config, then optionally rotate immediately
    public void reloadFromConfig(boolean rotate) {
        rarities.clear();
        items.clear();
        slots.clear();
        loadRarities();
        loadItems();
        loadSlots();
        if (rotate) {
            rotateItems();
        }
        plugin.getLogger().info("Rotating shop configuration reloaded.");
    }
}