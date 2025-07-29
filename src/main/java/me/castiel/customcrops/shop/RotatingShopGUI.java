package me.castiel.customcrops.shop;

import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.inventories.ConfigurableInventoryGUI;
import me.castiel.customcrops.util.DateUtils;
import me.castiel.customcrops.util.StringUtils;
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
                if (index.get() >= shopManager.getSlots().size()) return; // Avoid IndexOutOfBounds

                int slot = shopManager.getSlots().get(index.getAndIncrement());

                // Generate item with placeholders applied
                ItemStack displayItem = new ItemStack(item.getMaterial());
                ItemMeta meta = displayItem.getItemMeta();

                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                            item.getName().replace("%rarity%", item.getRarity().getDisplayName())
                                    .replace("%price%", String.valueOf(item.getPrice()))));

                    meta.setLore(item.getLore().stream()
                            .map(line -> ChatColor.translateAlternateColorCodes('&',
                                    line.replace("%rarity%", item.getRarity().getDisplayName())
                                            .replace("%price%", String.valueOf(item.getPrice()))))
                            .toList());

                    displayItem.setItemMeta(meta);
                }

                // Add glow effect
                if (item.isGlow()) {
                    displayItem.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
                    meta = displayItem.getItemMeta();
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    displayItem.setItemMeta(meta);
                }

                gui.setItem(slot, displayItem, event -> {
                    if (!plugin.getCurrencyManager().hasEnough(player.getUniqueId().toString(), (long) item.getPrice())) {
                        Actions.sendMessage(player, plugin.getSettingsManager().getMessage("InsufficientFunds"));
                        return;
                    }
                    for (String action : item.getActions()) {
                        Actions.execute(player, action);
                    }
                });
            });
        });
    }
}
