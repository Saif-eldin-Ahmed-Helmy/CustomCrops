package me.castiel.customcrops.shop;

import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.inventories.ConfigurableInventoryGUI;
import me.castiel.customcrops.util.DateUtils;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class RotatingShopGUI {

    private Map<String, Integer> calculateRemainingStock(RotatingShopItem item, Player player) {
        int remainingPersonalStock = item.getPersonalStock() != -1
                ? item.getPersonalStock() - item.getPlayerPurchaseCount(player.getUniqueId())
                : -1;

        int remainingGlobalStock = item.getGlobalStock() != -1
                ? item.getGlobalStock() - item.getPlayerPurchases().values().stream()
                .mapToInt(Integer::intValue).sum()
                : -1;

        Map<String, Integer> stockMap = new HashMap<>();
        stockMap.put("personal", remainingPersonalStock);
        stockMap.put("global", remainingGlobalStock);
        return stockMap;
    }

    public void openShop(Player player) {
        CustomCropsPlugin plugin = CustomCropsPlugin.getInstance();
        RotatingShopManager shopManager = plugin.getRotatingShopManager();
        Map<String, Supplier<String>> basePlaceholders = new HashMap<>();
        basePlaceholders.put("%rotation_time%", () -> DateUtils.format(shopManager.getNextRotationTime() / 1000L));
        basePlaceholders.put("%balance%", () -> String.valueOf(plugin.getCurrencyManager().getBalance(player.getUniqueId().toString())));

        ConfigurableInventoryGUI gui = new ConfigurableInventoryGUI(
                plugin,
                plugin.getConfig().getConfigurationSection("RotatingShop"),
                player,
                1,
                basePlaceholders
        );

        gui.setUpdateTask(() -> {
            gui.buildContents();
            AtomicInteger index = new AtomicInteger(0);

            shopManager.getCurrentRotation().forEach(item -> {
                if (index.get() >= shopManager.getSlots().size()) return;

                int slot = shopManager.getSlots().get(index.getAndIncrement());
                Map<String, Integer> stock = calculateRemainingStock(item, player);
                int remainingPersonalStock = stock.get("personal");
                int remainingGlobalStock = stock.get("global");

                // Generate item with placeholders applied
                ItemStack displayItem = new ItemStack(item.getMaterial());
                ItemMeta meta = displayItem.getItemMeta();

                if (meta != null) {
                    String outOfStockPlaceholder = plugin.getSettingsManager().getMessage("ItemOutOfStockPlaceholder");
                    String personalStockPlaceholder = remainingPersonalStock <= 0 && remainingPersonalStock != -1
                            ? outOfStockPlaceholder
                            : String.valueOf(remainingPersonalStock);
                    String globalStockPlaceholder = remainingGlobalStock <= 0 && remainingGlobalStock != -1
                            ? outOfStockPlaceholder
                            : String.valueOf(remainingGlobalStock);

                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                            item.getName().replace("%rarity%", item.getRarity().getDisplayName())
                                    .replace("%price%", String.valueOf(item.getPrice()))
                                    .replace("%personal_stock%", personalStockPlaceholder)
                                    .replace("%global_stock%", globalStockPlaceholder)
                                    .replace("%required_level%", String.valueOf(item.getRequiredLevel()))));

                    meta.setLore(item.getLore().stream()
                            .map(line -> ChatColor.translateAlternateColorCodes('&',
                                    line.replace("%rarity%", item.getRarity().getDisplayName())
                                            .replace("%price%", String.valueOf(item.getPrice()))
                                            .replace("%personal_stock%", personalStockPlaceholder)
                                            .replace("%global_stock%", globalStockPlaceholder)
                                            .replace("%required_level%", String.valueOf(item.getRequiredLevel()))))
                            .toList());

                    displayItem.setItemMeta(meta);
                }

                if (item.isGlow()) {
                    displayItem.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
                    meta = displayItem.getItemMeta();
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    displayItem.setItemMeta(meta);
                }

                gui.setItem(slot, displayItem, event -> {
                    Map<String, Integer> currentStock = calculateRemainingStock(item, player);
                    int currentRemainingPersonalStock = currentStock.get("personal");
                    int currentRemainingGlobalStock = currentStock.get("global");

                    if (!plugin.getCurrencyManager().hasEnough(player.getUniqueId().toString(), (long) item.getPrice())) {
                        Actions.sendMessage(player, plugin.getSettingsManager().getMessage("InsufficientFunds"));
                        return;
                    }

                    if (currentRemainingPersonalStock <= 0 && currentRemainingPersonalStock != -1) {
                        Actions.sendMessage(player, plugin.getSettingsManager().getMessage("ItemOutOfStock"));
                        return;
                    }

                    if (currentRemainingGlobalStock <= 0 && currentRemainingGlobalStock != -1) {
                        Actions.sendMessage(player, plugin.getSettingsManager().getMessage("ItemOutOfStock"));
                        return;
                    }

                    int requiredLevel = item.getRequiredLevel();
                    int playerLevel = shopManager.getPlayerHighestRequirement(player.getUniqueId().toString());

                    if (playerLevel < requiredLevel - 1) {
                        Actions.sendMessage(player, plugin.getSettingsManager().getMessage("InsufficientLevel")
                                .replace("%required_level%", String.valueOf(requiredLevel - 1)));
                        return;
                    }

                    plugin.getCurrencyManager().removeCurrency(player.getUniqueId().toString(), (long) item.getPrice());
                    item.addPurchase(player.getUniqueId());

                    for (String action : item.getActions()) {
                        Actions.execute(player, action);
                    }

                    if (requiredLevel > playerLevel) {
                        shopManager.setHighestRequirement(player.getUniqueId().toString(), requiredLevel);
                    }
                });
            });
        });
    }
}