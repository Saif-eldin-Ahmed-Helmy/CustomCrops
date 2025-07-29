package me.castiel.customcrops.events;

import me.castiel.customcrops.crops.CropData;
import me.castiel.customcrops.crops.CropManager;
import me.castiel.customcrops.inventories.CropsGUI;
import me.castiel.customcrops.shop.RotatingShopGUI;
import me.castiel.customcrops.util.CropUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;

public class CropEventListener implements Listener {

    private final CropManager cropManager;
    public CropEventListener(CropManager cropManager) {
        this.cropManager = cropManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (CropUtils.isCrop(block)) {
            cropManager.addCropIfAbsent(block.getLocation());
        }
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        if (CropUtils.isCrop(block)) {
            if (!cropManager.isTracked(block.getLocation())) {
                cropManager.addCropIfAbsent(block.getLocation());
            }
            event.setCancelled(true); // Prevent the default growth behavior
        }
    }

    @EventHandler
    public void onCropClick(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && CropUtils.isCrop(block)) {
            Block base = block.getType() == Material.MELON || block.getType() == Material.PUMPKIN
                    ? CropUtils.findGrownCropStem(block) : CropUtils.findGrowableBaseBlock(block);
            if (base == null) {
                return; // No base block found, can happen
            }
            cropManager.addCropIfAbsent(base.getLocation());
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                CropData cropData = cropManager.getCropData(base.getLocation());
                if (cropData == null) {
                    Bukkit.getLogger().warning("No crop data found for block at " + base.getLocation());
                    return; // No crop data found, shouldn't happen
                }
                CropsGUI.openCropsGUI(event.getPlayer(), cropData);
            }
        }
    }

    // handle breaking, make dirty too to be removed
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (CropUtils.isCrop(block)) {
            if (cropManager.isTracked(block.getLocation())) {
                cropManager.removeCrop(block.getLocation());
            }
        }
    }

    // handle chunk load & unload
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        cropManager.loadChunkCrops(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkLoadEvent event) {
        cropManager.unloadChunkCrops(event.getChunk());
    }

    @EventHandler
    public void onBlockFertilize(BlockFertilizeEvent event) {
        if (CropUtils.isCrop(event.getBlock())) {
            Block block = event.getBlock();
            CropData cropData = cropManager.getCropData(block.getLocation());
            if (cropData != null && !cropData.isFullyGrown()) {
                cropData.setLastGrowth(System.currentTimeMillis());
                event.setCancelled(true); // Prevent default fertilization behavior
            }
        }
    }

    // handle custom crops item
    @EventHandler
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        cropManager.updateCropItemStack(stack);
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        ItemStack stack = event.getItem();
        cropManager.updateCropItemStack(stack);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack stack = event.getEntity().getItemStack();
        cropManager.updateCropItemStack(stack);
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        cropManager.updateCropItemStack(stack);
    }

    @EventHandler
    public void onEntityDrop(EntityDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        cropManager.updateCropItemStack(stack);
    }

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent event) {
        event.getItems().forEach(itemEntity -> {
            ItemStack stack = itemEntity.getItemStack();
            cropManager.updateCropItemStack(stack);
        });
    }


}
