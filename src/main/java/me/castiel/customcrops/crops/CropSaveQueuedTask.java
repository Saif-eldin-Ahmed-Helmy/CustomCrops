package me.castiel.customcrops.crops;

import me.castiel.customcrops.CustomCropsPlugin;
import org.bukkit.Bukkit;

public class CropSaveQueuedTask {

    private final CustomCropsPlugin plugin;
    private final CropManager cropManager;

    public CropSaveQueuedTask(CustomCropsPlugin plugin, CropManager cropManager) {
        this.plugin = plugin;
        this.cropManager = cropManager;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::run, 0L, 60L * 20L);
    }

    public void run() {
        cropManager.saveDirtyCrops(true);
    }

    public void stop() {
        Bukkit.getScheduler().cancelTasks(plugin);
        cropManager.saveDirtyCrops(false);
        plugin.getLogger().info("Crops auto save task stopped & crops have been saved.");
    }

}
