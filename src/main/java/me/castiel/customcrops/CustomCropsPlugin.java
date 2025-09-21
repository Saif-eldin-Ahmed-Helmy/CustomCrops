package me.castiel.customcrops;

import me.castiel.customcrops.commands.CubesCommand;
import me.castiel.customcrops.config.SettingsManager;
import me.castiel.customcrops.crops.CropManager;
import me.castiel.customcrops.currency.CubesPlaceholder;
import me.castiel.customcrops.currency.CurrencyManager;
import me.castiel.customcrops.shop.RotatingShopManager;
import me.castiel.customcrops.storage.CropDAO;
import me.castiel.customcrops.storage.SQLiteDatabase;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomCropsPlugin extends JavaPlugin {

    private static CustomCropsPlugin instance;
    public static CustomCropsPlugin getInstance() {
        return instance;
    }


    private SQLiteDatabase database;

    private SettingsManager settingsManager;
    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    private RotatingShopManager rotatingShopManager;
    public RotatingShopManager getRotatingShopManager() {
        return rotatingShopManager;
    }

    private CropManager cropManager;
    public CropManager getCropManager() {
        return cropManager;
    }

    private CurrencyManager currencyManager;
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    @Override
    public void onEnable() {
        instance = this;
        settingsManager = new SettingsManager(this);
        database = new SQLiteDatabase(getDataFolder() + "/crops.db");
        database.initializeDatabase().whenComplete((result, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                getLogger().severe("Failed to initialize database: " + throwable.getMessage());
                getServer().getPluginManager().disablePlugin(this);
            } else {
                getLogger().info("Database initialized successfully.");
                Bukkit.getScheduler().runTask(this, () -> {
                    CropDAO cropDAO = new CropDAO(database);
                    currencyManager = new CurrencyManager(this, cropDAO);
                    if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                        new CubesPlaceholder(this).register();
                        getLogger().info("Registered PlaceholderAPI placeholders for CustomCrops");
                    } else {
                        getLogger().warning("PlaceholderAPI not found, skipping placeholder registration");
                    }
                    rotatingShopManager = new RotatingShopManager(this, cropDAO);
                    cropManager = new CropManager(this, cropDAO);
                    CubesCommand cubesCommand = new CubesCommand(this);
                    getCommand("customcrops").setExecutor(cubesCommand);
                    getCommand("customcrops").setTabCompleter(cubesCommand);
                });
            }

            getLogger().info("CustomCrops plugin has been enabled!");
        });
    }

    @Override
    public void onDisable() {
        cropManager.stopTasks();
        currencyManager.stopAutoSave();
        rotatingShopManager.stopAutoSave();
        database.close();
        getLogger().info("CustomCrops plugin has been disabled!");
    }
}
