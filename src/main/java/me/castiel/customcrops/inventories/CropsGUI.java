package me.castiel.customcrops.inventories;

import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.crops.CropData;
import me.castiel.customcrops.util.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CropsGUI {

    public static void openCropsGUI(Player player, CropData cropData) {
        Map<String, Supplier<String>> placeholders = new HashMap<>();

        // Register dynamic placeholder suppliers
        placeholders.put("%crop_icon%", cropData::getDisplayMaterial);
        placeholders.put("%crop_type%", () -> StringUtils.getType(cropData.getMaterial()));
        placeholders.put("%time_remaining%", () -> {
            int seconds = cropData.getTimeRemaining();
            if (seconds <= 1 || cropData.getLastGrowth() == 0) return "Fully generated";
            int mins = seconds / 60;
            int secs = seconds % 60;
            return mins + "m " + secs + "s";
        });

        placeholders.put("%growth_percentage%", () -> {
            int percentage = cropData.getGrowthProgress();
            if (percentage >= 100) return "Fully grown";
            return percentage + "%";
        });

        placeholders.put("%planted_time%", () -> {
            long elapsed = System.currentTimeMillis() - cropData.getPlantedAt();
            return formatTimeAgo(elapsed);
        });

        placeholders.put("%last_growth%", () -> {
            long elapsed = System.currentTimeMillis() - cropData.getLastGrowth();
            return formatTimeAgo(elapsed);
        });

        // Load inventory config section
        CustomCropsPlugin plugin = CustomCropsPlugin.getInstance();
        ConfigurationSection guiSection = plugin.getConfig().getConfigurationSection("CropInfoGUI");

        ConfigurableInventoryGUI gui = new ConfigurableInventoryGUI(
                plugin,
                guiSection,
                player,
                1,
                placeholders
        );
    }

    // Formats elapsed milliseconds to "Xm Ys ago"
    private static String formatTimeAgo(long elapsedMillis) {
        long seconds = elapsedMillis / 1000;
        long mins = seconds / 60;
        long secs = seconds % 60;
        return mins + "m " + secs + "s ago";
    }
}