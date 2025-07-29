package me.castiel.customcrops.crops;

import me.castiel.customcrops.CustomCropsPlugin;
import org.bukkit.Bukkit;

public class CropGrowthTask {

    private final CustomCropsPlugin plugin;
    private final CropManager cropManager;

    public CropGrowthTask(CustomCropsPlugin plugin, CropManager cropManager) {
        this.plugin = plugin;
        this.cropManager = cropManager;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::run, 0L, 20L);
    }

    public void run() {
        cropManager.updateCrops();
    }

    public void stop() {
        Bukkit.getScheduler().cancelTasks(plugin);
        plugin.getLogger().info("Crop growth task stopped.");
    }

}
