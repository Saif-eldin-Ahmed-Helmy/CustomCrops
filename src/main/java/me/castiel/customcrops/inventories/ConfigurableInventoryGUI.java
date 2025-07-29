package me.castiel.customcrops.inventories;

import com.iridium.iridiumcolorapi.IridiumColorAPI;
import me.castiel.customcrops.CustomCropsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A configurable inventory GUI that parses its layout from a ConfigurationSection.
 * Supports background filling, barrier edges, and periodic auto-refresh with placeholders.
 */
public class ConfigurableInventoryGUI implements Listener {
    private final CustomCropsPlugin plugin;
    private final Inventory inventory;
    private final ConfigurationSection config;
    private final Player viewer;
    private final Map<Integer, Consumer<InventoryClickEvent>> buttonActions = new HashMap<>();
    private final boolean fillBackground;
    private final Material backgroundMaterial;
    private final boolean barrierEdges;
    private final long updateIntervalTicks;
    private final Map<String, Supplier<String>> placeholderSuppliers;
    private BukkitTask refreshTask;
    private BukkitTask updateTask;
    private Runnable onDestroy;

    /**
     * @param plugin main plugin instance
     * @param section configuration section for this GUI
     * @param viewer player to open for
     * @param updateIntervalSeconds seconds between automatic rebuilds (<=0 to disable)
     * @param placeholderSuppliers map of placeholder key to supplier for dynamic replacement
     */
    public ConfigurableInventoryGUI(CustomCropsPlugin plugin,
                                    ConfigurationSection section,
                                    Player viewer,
                                    int updateIntervalSeconds,
                                    Map<String, Supplier<String>> placeholderSuppliers) {
        this.plugin = plugin;
        this.config = section;
        this.viewer = viewer;
        this.placeholderSuppliers = placeholderSuppliers != null ? placeholderSuppliers : Map.of();
        this.fillBackground = config.isConfigurationSection("Fill-Background");
        this.backgroundMaterial = Material.valueOf(translate(config.getString("Fill-Background.Material", "AIR")));
        this.barrierEdges = config.isConfigurationSection("Barrier");
        this.updateIntervalTicks = updateIntervalSeconds > 0 ? updateIntervalSeconds * 20L : 0;

        int size = config.getInt("Size", 9);
        String title = translate(config.getString("Title", ""));
        this.inventory = Bukkit.createInventory(null, size, title);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        buildContents();
        viewer.openInventory(inventory);

        if (updateIntervalTicks > 0) startAutoRefresh();
    }

    private void startAutoRefresh() {
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::buildContents, updateIntervalTicks, updateIntervalTicks);
    }

    public void addButton(int slot, Consumer<InventoryClickEvent> action) {
        buttonActions.put(slot, action);
    }

    public void buildContents() {
        inventory.clear();
        buttonActions.clear();

        if (fillBackground) {
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, new ItemStack(backgroundMaterial));
            }
        }
        if (barrierEdges) {
            ConfigurationSection b = config.getConfigurationSection("Barrier");
            Material mat = Material.valueOf(translate(b.getString("Material", "BARRIER")));
            String name = translate(b.getString("Name", " "));
            ItemStack barrier = new ItemStack(mat);
            ItemMeta bm = barrier.getItemMeta(); bm.setDisplayName(name); barrier.setItemMeta(bm);
            int size = inventory.getSize();
            for (int i = 0; i < size; i++) {
                if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                    inventory.setItem(i, barrier);
                }
            }
        }
        ConfigurationSection items = config.getConfigurationSection("Items");
        if (items == null) return;
        for (String key : items.getKeys(false)) {
            ConfigurationSection sec = items.getConfigurationSection(key);
            int slot = sec.getInt("Slot", -1);
            if (slot < 0 || slot >= inventory.getSize()) continue;
            Material mat = Material.valueOf(translate(sec.getString("Material", "STONE")));
            String name = translate(sec.getString("Name", ""));
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            if (sec.isList("Lore")) {
                meta.setLore(sec.getStringList("Lore").stream().map(this::translate).toList());
            }
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            if (sec.isString("Action")) {
                String action = sec.getString("Action");
                buttonActions.put(slot, e -> handleAction(action, e));
            }
        }
    }

    private String translate(String text) {
        if (text == null) return null;
        String t = ChatColor.translateAlternateColorCodes('&', text);
        for (var entry : placeholderSuppliers.entrySet()) {
            t = t.replace(entry.getKey(), entry.getValue().get());
        }
        return IridiumColorAPI.process(t);
    }

    private void handleAction(String action, InventoryClickEvent e) {
        if ("close".equalsIgnoreCase(action)) {
            e.getWhoClicked().closeInventory();
        }
        // extend actions as needed
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTopInventory().equals(inventory)) return;
        event.setCancelled(true);
        Consumer<InventoryClickEvent> action = buttonActions.get(event.getSlot());
        if (action != null) {
            action.accept(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().equals(inventory)) destroy();
    }

    /**
     * Call to clean up and stop auto refresh.
     */
    private void destroy() {
        HandlerList.unregisterAll(this);
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (onDestroy != null) onDestroy.run();
    }

    public void setOnDestroy(Runnable onDestroy) {
        this.onDestroy = onDestroy;
    }

    public void setItem(int slot, ItemStack displayItem, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, displayItem);
        if (action != null) {
            buttonActions.put(slot, action);
        }
    }

    public void setUpdateTask(Runnable updateTask) {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        this.refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, updateTask, 0L, updateIntervalTicks);
    }


    public void addUpdateTask(Runnable updateTask) {
        this.updateTask = Bukkit.getScheduler().runTaskTimer(plugin, updateTask, 0L, updateIntervalTicks);
    }
}
