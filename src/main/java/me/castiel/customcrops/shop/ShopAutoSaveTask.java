package me.castiel.customcrops.shop;

import me.castiel.customcrops.CustomCropsPlugin;
import org.bukkit.Bukkit;

public class ShopAutoSaveTask {

    private final CustomCropsPlugin plugin;
    private final RotatingShopManager rotatingShopManager;

    public ShopAutoSaveTask(CustomCropsPlugin plugin, RotatingShopManager rotatingShopManager) {
        this.plugin = plugin;
        this.rotatingShopManager = rotatingShopManager;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::run, 0L, 60L * 20L);
    }

    public void run() {
        rotatingShopManager.saveRequirements(true);
    }

    public void stop() {
        Bukkit.getScheduler().cancelTasks(plugin);
        rotatingShopManager.saveRequirements(false);
        plugin.getLogger().info("Currency auto save task stopped.");
    }

}
