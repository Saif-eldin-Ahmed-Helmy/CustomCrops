package me.castiel.customcrops.config;

import me.castiel.customcrops.CustomCropsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class SettingsManager {

    private final CustomCropsPlugin plugin;
    private final Map<Material, Integer> cropGrowthTimes = new EnumMap<>(Material.class);
    private final Map<Material, Integer> cropSellPrices = new EnumMap<>(Material.class);
    private String customCropName;
    private List<String> customCropLore;
    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, Object> rotatingShopSettings = new HashMap<>();

    public SettingsManager(CustomCropsPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        // Load crop growth times
        ConfigurationSection cropGrowthSection = config.getConfigurationSection("CropGrowthTimes");
        if (cropGrowthSection != null) {
            for (String key : cropGrowthSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    int timeInSeconds = cropGrowthSection.getInt(key);
                    cropGrowthTimes.put(material, timeInSeconds);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in CropGrowthTimes: " + key);
                }
            }
        }

        ConfigurationSection cropSellSection = config.getConfigurationSection("CropSellPrices");
        if (cropSellSection != null) {
            for (String key : cropSellSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    int prices = cropSellSection.getInt(key);
                    cropSellPrices.put(material, prices);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in CropSellPrices: " + key);
                }
            }
        }

        // Load custom crop item settings
        ConfigurationSection customCropSection = config.getConfigurationSection("CustomCropItem");
        if (customCropSection != null) {
            customCropName = customCropSection.getString("Name", "&aCrop: %crop_name%");
            customCropLore = customCropSection.getStringList("Lore");
        }

        // Load messages
        ConfigurationSection messagesSection = config.getConfigurationSection("Messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                messages.put(key, messagesSection.getString(key, ""));
            }
        }

        // Load rotating shop settings
        ConfigurationSection shopSection = config.getConfigurationSection("RotatingShop");
        if (shopSection != null) {
            rotatingShopSettings.put("Size", shopSection.getInt("Size", 45));
            rotatingShopSettings.put("Title", shopSection.getString("Title", "&a&lRotating Shop"));
            rotatingShopSettings.put("ResetTimes", shopSection.getStringList("ResetTimes"));
            rotatingShopSettings.put("Rarities", shopSection.getConfigurationSection("Rarities"));
            rotatingShopSettings.put("Slots", shopSection.getIntegerList("Slots"));
            rotatingShopSettings.put("Items", shopSection.getConfigurationSection("Items"));
            rotatingShopSettings.put("ShopItems", shopSection.getConfigurationSection("ShopItems"));
        }
    }

    public void reload() {
        plugin.reloadConfig();
        loadConfig();
        plugin.getLogger().info("Configuration reloaded successfully.");
    }

    public int getGrowthTime(Material material) {
        if (material.name().contains("ATTACHED_")) {
            material = Material.valueOf(material.name().replace("ATTACHED_", ""));
        }
        return cropGrowthTimes.getOrDefault(material, 600); // Default to 600 seconds
    }

    public int getSellPrice(Material material) {
        return cropSellPrices.getOrDefault(material, -1);
    }

    public Map<Material, Integer> getAllGrowthTimes() {
        return cropGrowthTimes;
    }

    public String getCustomCropName() {
        return customCropName;
    }

    public List<String> getCustomCropLore() {
        return customCropLore;
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }

    public Map<String, Object> getRotatingShopSettings() {
        return rotatingShopSettings;
    }
}