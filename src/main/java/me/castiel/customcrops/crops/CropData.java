package me.castiel.customcrops.crops;

import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.util.CropUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;

import java.util.Objects;

public class CropData {
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final Material material;
    private final long plantedAt;
    private boolean isDirty;
    private long lastGrowth = 0L;

    public CropData(String worldName, int x, int y, int z, Material material, long plantedAt, boolean isDirty) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.material = material;
        this.plantedAt = plantedAt;
        this.isDirty = isDirty;
        lastGrowth = System.currentTimeMillis();
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Material getMaterial() {
        return material;
    }

    public long getPlantedAt() {
        return plantedAt;
    }

    public Location toLocation() {
        World world = getWorld();
        return world != null ? new Location(world, x, y, z) : null;
    }

    public String getBlockKey() {
        return worldName + ":" + x + ":" + y + ":" + z;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Location) {
            Location loc = (Location) o;
            return loc.getWorld().getName().equals(worldName) &&
                    loc.getBlockX() == x &&
                    loc.getBlockY() == y &&
                    loc.getBlockZ() == z;
        } else if (o instanceof CropData) {
            CropData that = (CropData) o;
            return x == that.x && y == that.y && z == that.z && worldName.equals(that.worldName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, y, z);
    }

    public long getLastGrowth() {
        return lastGrowth;
    }

    public void setLastGrowth(long lastGrowth) {
        this.lastGrowth = lastGrowth;
    }

    public int getTimeRemaining() {
        if (isFullyGrown()) {
            return 0;
        }
        long growthTime = CustomCropsPlugin.getInstance().getSettingsManager().getGrowthTime(material); // in seconds
        long now = System.currentTimeMillis() / 1000L;
        long last = lastGrowth / 1000L;

        long elapsed = now - last;
        long remaining = growthTime - elapsed;

        return (int) Math.max(0, remaining);
    }

    public int getGrowthProgress() {
        if (isFullyGrown()) {
            return 100;
        }
        long growthTime = CustomCropsPlugin.getInstance().getSettingsManager().getGrowthTime(material); // in seconds
        long now = System.currentTimeMillis() / 1000L;
        long last = lastGrowth / 1000L;

        long elapsed = now - last;
        double progress = (elapsed / (double) growthTime) * 100.0;

        return (int) Math.min(100, Math.max(0, Math.round(progress)));
    }

    public Block getBlock() {
        World world = getWorld();
        if (world != null) {
            return world.getBlockAt(x, y, z);
        }
        return null;
    }

    public String getDisplayMaterial() {
        String basename = material.name().contains("ATTACHED_") ?
                material.name().replace("ATTACHED_", "") : material.name();
        return basename.equalsIgnoreCase("MELON_STEM") ? Material.MELON.name()
                : basename.equalsIgnoreCase("PUMPKIN_STEM") ? Material.PUMPKIN.name()
                        : basename.equalsIgnoreCase("BEETROOTS") ? Material.BEETROOT.name()
                : basename.equalsIgnoreCase("POTATOES") ? Material.POTATO.name()
                : basename.equalsIgnoreCase("COCOA") ? Material.COCOA_BEANS.name()

                        : material.name();
    }

    public boolean isFullyGrown() {
        Block block = getBlock();

        if (block == null || block.getType() != material) return false;

        if (material.name().contains("ATTACHED")) {
            return true;
        }

        if (material == Material.CACTUS || material == Material.SUGAR_CANE) {
            Block base = CropUtils.findGrowableBaseBlock(block);
            int height = 1;
            Block above = base.getRelative(BlockFace.UP);

            while (height < 3 && above.getType() == material) {
                height++;
                above = above.getRelative(BlockFace.UP);
            }

            return height >= 3;
        }

        if (block.getBlockData() instanceof Ageable ageable) {
            int maxAge = ageable.getMaximumAge();
            if (ageable.getAge() < maxAge) return false;
            return !CropUtils.isStemBlock(material);
        }
        return false;
    }
}
