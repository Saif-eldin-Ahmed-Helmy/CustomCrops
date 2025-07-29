package me.castiel.customcrops.crops;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class CropFactory {

    /**
     * Creates a CropData object from a Bukkit Block.
     */
    public static CropData fromBlock(Block block) {
        return new CropData(
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ(),
                block.getType(),
                System.currentTimeMillis(),
                true
        );
    }

    /**
     * Create a CropData object from a Location.
     */
    public static CropData fromLocation(Location location) {
        return new CropData(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                location.getBlock().getType(),
                System.currentTimeMillis(),
                true
        );
    }

    /**
     * Reconstruct CropData from saved data.
     */
    public static CropData fromSaved(String worldName, int x, int y, int z, Material material, long plantedAt) {
        return new CropData(worldName, x, y, z, material, plantedAt, false);
    }
}
