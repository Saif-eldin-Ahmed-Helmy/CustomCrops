package me.castiel.customcrops.inventories;

import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.shop.Actions;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Supplier;

public class SellingGUI {

    public static void openShop(Player player) {
        CustomCropsPlugin plugin = CustomCropsPlugin.getInstance();

        // Load inventory config section
        ConfigurationSection guiSection = plugin.getConfig().getConfigurationSection("SellingShop");
        if (guiSection == null) return;

        // Parse allowed selling slots from config (e.g. ["10-16", "19", "28-34"])
        Set<Integer> allowedSlots = parseSlots(guiSection.getStringList("SellingGUISlots"), guiSection.getInt("Size", 27));

        // We need a reference to the GUI inventory inside placeholders
        final Inventory[] invRef = new Inventory[1];

        Map<String, Supplier<String>> placeholders = new HashMap<>();
        placeholders.put("%value%", () -> String.valueOf(calculateGuiTotalValue(invRef[0], allowedSlots)));

        ConfigurableInventoryGUI gui = new ConfigurableInventoryGUI(
                plugin,
                guiSection,
                player,
                0,
                placeholders
        );

        // Store inventory reference now that GUI exists
        invRef[0] = gui.getInventory();

        // Clear background items in allowed input slots so players can see they're empty
        for (Integer s : allowedSlots) {
            if (s >= 0 && s < invRef[0].getSize()) {
                invRef[0].setItem(s, null);
            }
        }

        // Restrict to crops only in allowed slots
        gui.configurePlaceableSlots(allowedSlots, SellingGUI::isSellableCrop);

        // Dynamically update the Sell button's lore to reflect current GUI total
        int sellButtonSlot = guiSection.getConfigurationSection("Items") != null
                ? guiSection.getConfigurationSection("Items").getConfigurationSection("SellAllButton").getInt("Slot", 22)
                : 22;
        gui.addUpdateTask(() -> updateSellButtonLore(invRef[0], sellButtonSlot, calculateGuiTotalValue(invRef[0], allowedSlots)));

        // Action: sell only items inside the GUI input slots
        gui.addAction("sellall", event -> {
            long total = 0L;
            long count = 0L;
            Inventory inv = invRef[0];
            if (inv != null) {
                for (Integer s : allowedSlots) {
                    if (s < 0 || s >= inv.getSize()) continue;
                    ItemStack item = inv.getItem(s);
                    long value = getCropValue(item);
                    if (value > 0) {
                        total += value;
                        count += item.getAmount();
                        inv.setItem(s, null);
                    }
                }
            }

            if (total <= 0) {
                Actions.sendMessage(player, plugin.getSettingsManager().getMessage("NoCropsToSell"));
                return;
            }

            plugin.getCurrencyManager().addCurrency(player.getUniqueId().toString(), total);

            String message = plugin.getSettingsManager().getMessage("SoldCrops")
                    .replace("%amount%", String.valueOf(count))
                    .replace("%total%", String.valueOf(total));
            Actions.sendMessage(player, message);
        });

        // On close: return any items left in input slots back to the player, drop if full
        gui.setOnDestroy(() -> {
            Inventory inv = invRef[0];
            if (inv == null) return;
            List<ItemStack> toReturn = new ArrayList<>();
            for (Integer s : allowedSlots) {
                if (s < 0 || s >= inv.getSize()) continue;
                ItemStack item = inv.getItem(s);
                if (item != null) {
                    toReturn.add(item);
                    inv.setItem(s, null);
                }
            }
            if (toReturn.isEmpty()) return;

            Map<Integer, ItemStack> leftovers = new HashMap<>();
            for (ItemStack i : toReturn) {
                leftovers.putAll(player.getInventory().addItem(i));
            }
            if (!leftovers.isEmpty() && player.getWorld() != null) {
                leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            }
        });
    }

    private static boolean isSellableCrop(ItemStack item) {
        if (item == null) return false;
        int price = CustomCropsPlugin.getInstance().getSettingsManager().getSellPrice(item.getType());
        return price > 0;
    }

    private static Set<Integer> parseSlots(List<String> defs, int invSize) {
        if (defs == null) return Collections.emptySet();
        Set<Integer> set = new LinkedHashSet<>();
        for (String def : defs) {
            if (def == null) continue;
            def = def.trim();
            if (def.isEmpty()) continue;
            if (def.contains("-")) {
                String[] parts = def.split("-");
                if (parts.length == 2) {
                    try {
                        int start = Integer.parseInt(parts[0].trim());
                        int end = Integer.parseInt(parts[1].trim());
                        if (start > end) { int t = start; start = end; end = t; }
                        for (int i = start; i <= end; i++) {
                            if (i >= 0 && i < invSize) set.add(i);
                        }
                    } catch (NumberFormatException ignored) { }
                }
            } else {
                try {
                    int slot = Integer.parseInt(def);
                    if (slot >= 0 && slot < invSize) set.add(slot);
                } catch (NumberFormatException ignored) { }
            }
        }
        return set;
    }

    private static void updateSellButtonLore(Inventory inv, int slot, long totalValue) {
        if (inv == null) return;
        ItemStack item = inv.getItem(slot);
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.getLore();
        if (lore == null) return;

        String replacement = ChatColor.translateAlternateColorCodes('&', "&7Total Value: &a" + totalValue + " Cubes");
        boolean changed = false;
        for (int i = 0; i < lore.size(); i++) {
            String raw = lore.get(i);
            String stripped = ChatColor.stripColor(raw);
            if (stripped != null && stripped.toLowerCase(Locale.ROOT).contains("total value:")) {
                lore.set(i, replacement);
                changed = true;
                break;
            }
        }
        if (changed) {
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }
    }

    private static long calculateGuiTotalValue(Inventory inv, Set<Integer> allowedSlots) {
        if (inv == null || allowedSlots == null || allowedSlots.isEmpty()) return 0L;
        long total = 0L;
        for (Integer s : allowedSlots) {
            if (s < 0 || s >= inv.getSize()) continue;
            ItemStack item = inv.getItem(s);
            total += getCropValue(item);
        }
        return total;
    }

    private static long getCropValue(ItemStack item) {
        if (item == null) return 0L;
        int price = CustomCropsPlugin.getInstance().getSettingsManager().getSellPrice(item.getType());
        if (price <= 0) return 0L;
        return (long) price * item.getAmount();
    }
}

