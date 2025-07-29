package me.castiel.customcrops.currency;

import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.crops.CropManager;
import org.bukkit.Bukkit;

public class AutoSaveTask {

    private final CustomCropsPlugin plugin;
    private final CurrencyManager currencyManager;

    public AutoSaveTask(CustomCropsPlugin plugin, CurrencyManager currencyManager) {
        this.plugin = plugin;
        this.currencyManager = currencyManager;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::run, 0L, 60L * 20L);
    }

    public void run() {
        currencyManager.saveBalances(true);
    }

    public void stop() {
        Bukkit.getScheduler().cancelTasks(plugin);
        currencyManager.saveBalances(false);
        plugin.getLogger().info("Currency auto save task stopped.");
    }

}
