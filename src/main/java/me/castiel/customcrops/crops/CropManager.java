package me.castiel.customcrops.crops;

import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.config.SettingsManager;
import me.castiel.customcrops.events.CropEventListener;
import me.castiel.customcrops.storage.CropDAO;
import me.castiel.customcrops.util.CropUtils;
import me.castiel.customcrops.util.LocationUtils;
import me.castiel.customcrops.util.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CropManager {

    private final SettingsManager settingsManager;
    private final CropDAO cropDAO;
    private final CropGrowthTask cropGrowthTask;
    private final CropSaveQueuedTask cropSaveQueuedTask;
    private final ConcurrentHashMap<String, CropChunk> crops;

    public CropManager(CustomCropsPlugin plugin, CropDAO cropDAO) {
        this.cropDAO = cropDAO;
        this.settingsManager = plugin.getSettingsManager();
        this.cropGrowthTask = new CropGrowthTask(plugin, this);
        this.cropSaveQueuedTask = new CropSaveQueuedTask(plugin, this);
        this.crops = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(new CropEventListener(this), plugin);
        initSavedData();

    }

    public void initSavedData() {
        cropDAO.loadAllCrops().whenCompleteAsync((savedCrops, throwable) -> {
            for (CropData cropData : savedCrops) {
                CropChunk cropChunk = getCropChunk(cropData.getWorld().getChunkAt(Math.floorDiv(cropData.getX(), 16), Math.floorDiv(cropData.getZ(), 16)));
                cropChunk.addCrop(cropData);
            }
            CustomCropsPlugin.getInstance().getLogger().info("Loaded " + savedCrops.size() + " crops from the database.");
        });
    }

    public CropChunk getCropChunk(Chunk chunk) {
        String chunkID = LocationUtils.getChunkID(chunk);
        return crops.computeIfAbsent(chunkID, id ->
                new CropChunk(chunk.getX(), chunk.getZ(), chunk.getWorld().getName()));
    }

    public CropChunk getCropChunk(Location location) {
        return getCropChunk(location.getChunk());
    }

    public void addCropIfAbsent(Location location) {
        if (!isTracked(location)) {
            CropChunk cropChunk = getCropChunk(location);
            CropData cropData = CropFactory.fromLocation(location);
            cropChunk.addCrop(cropData);
            cropChunk.setDirty(true);
        }
    }

    public boolean isTracked(Location location) {
        CropChunk cropChunk = getCropChunk(location);
        if (cropChunk == null) {
            return false;
        }
        for (CropData crop : cropChunk.getCrops()) {
            if (crop.equals(location)) {
                return true;
            }
        }
        return false;
    }

    public void removeCrop(Location location) {
        CropChunk cropChunk = getCropChunk(location);
        if (cropChunk != null) {
            CropData cropToRemove = cropChunk.getCrops().stream()
                    .filter(crop -> crop.equals(location))
                    .findFirst()
                    .orElse(null);
            if (cropToRemove != null) {
                cropChunk.removeCrop(cropToRemove);
                cropChunk.setDirty(true);
            }
        }
    }

    public void loadChunkCrops(Chunk chunk) {
      //  CropChunk cropChunk = getCropChunk(chunk);
      //  cropChunk.setLoaded(true);
    }

    public void unloadChunkCrops(Chunk chunk) {
      //  CropChunk cropChunk = getCropChunk(chunk);
      //  cropChunk.setLoaded(false);
    }

    public void stopTasks() {
        cropGrowthTask.stop();
        cropSaveQueuedTask.stop();

    }

    public void saveDirtyCrops(boolean async) {
        List<CropData> dirtyCrops = new ArrayList<>();
        for (CropChunk chunk : crops.values()) {
            if (chunk.isDirty()) {
                dirtyCrops.addAll(chunk.getCrops());
                chunk.setDirty(false);
            }
        }
        if (!dirtyCrops.isEmpty()) {
            if (async) {
                cropDAO.saveCrops(dirtyCrops);
                        //.thenRun(() ->
                        //Bukkit.getLogger().info("Saved " + dirtyCrops.size() + " crops to the database."));
            }
            else {
                cropDAO.saveCropsSync(dirtyCrops);
            }
        }
    }

    public void updateCrops() {
        long nowSec = System.currentTimeMillis() / 1000L;
        for (CropChunk cropChunk : crops.values()) {
            for (CropData crop : cropChunk.getCrops()) {
                Location loc = crop.toLocation();
                if (loc == null) {
                    continue;
                }
                if (!cropChunk.isLoaded()) {
                    if (loc.getWorld().isChunkLoaded(cropChunk.getX(), cropChunk.getZ())) {
                        cropChunk.setLoaded(true);
                    }
                    else {
                        continue;
                    }
                }

                Block block = loc.getBlock();

                if (!CropUtils.isCrop(block)) {
                    continue;
                }

                BlockData data = block.getBlockData();
                long growthTimeSec = settingsManager.getGrowthTime(crop.getMaterial());
                long timeSinceLastGrowth = nowSec - (crop.getLastGrowth() / 1000L);
                double progress = Math.min(1.0, timeSinceLastGrowth / (double) Math.max(growthTimeSec, 1));

                // ──────────────────────────────
                // Stackable growables (sugar/cactus)
                // ──────────────────────────────
                if (CropUtils.isGrowableNotAgeable(crop.getMaterial())) {
                    if (CropUtils.canGrowGrowable(block)) {
                        Block base = CropUtils.findGrowableBaseBlock(block);

                        if (base.getType() == crop.getMaterial()) {
                            int height = 1;
                            Block above = base.getRelative(0, 1, 0);
                            while (height < 3 && above.getType() == crop.getMaterial()) {
                                height++;
                                above = above.getRelative(0, 1, 0);
                            }

                            if (height < 3 && progress >= 1.0 && above.getType() == Material.AIR) {
                                above.setType(crop.getMaterial());
                                crop.setLastGrowth(System.currentTimeMillis());
                                cropChunk.setDirty(true);
                            }
                        }
                    }
                }
                else if (data instanceof Ageable ageable) {
                    int maxAge = ageable.getMaximumAge();

                    int targetAge = Math.min(maxAge, (int) Math.floor(progress * (maxAge + 1)));
                    int currentAge = ageable.getAge();

                    if (targetAge > currentAge) {
                        Ageable newData = (Ageable) data.clone();
                        newData.setAge(targetAge);
                        block.setBlockData(newData);
                        cropChunk.setDirty(true);
                    }

                    if (targetAge == maxAge
                            && (crop.getMaterial() == Material.MELON_STEM || crop.getMaterial() == Material.PUMPKIN_STEM)) {

                        long now = System.currentTimeMillis();
                        long fruitIntervalMs = settingsManager.getGrowthTime(crop.getMaterial()) * 1000L;

                        long dt = now - crop.getLastGrowth();

                        if (dt >= fruitIntervalMs) {
                            BlockFace[] directions = {
                                    BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
                            };
                            for (BlockFace face : directions) {
                                Block adjacent = block.getRelative(face);
                                if (adjacent.getType() != Material.AIR) {
                                    continue;
                                }
                                Block soil = adjacent.getRelative(BlockFace.DOWN);
                                if (!CropUtils.isValidStemSoil(soil.getType())) {
                                    continue;
                                }

                                Material fruit = (crop.getMaterial() == Material.MELON_STEM
                                        ||crop.getMaterial() == Material.ATTACHED_MELON_STEM)
                                        ? Material.MELON : Material.PUMPKIN;

                                if (adjacent.getType() != Material.AIR && adjacent.getType() != fruit) {
                                    crop.setLastGrowth(now);
                                    continue;
                                }
                                adjacent.setType(fruit);

                                Material attachedMat = (crop.getMaterial() == Material.MELON_STEM)
                                        ? Material.ATTACHED_MELON_STEM
                                        : Material.ATTACHED_PUMPKIN_STEM;
                                BlockData attachedData = Bukkit.createBlockData(attachedMat);
                                if (attachedData instanceof Directional dir) {
                                    dir.setFacing(face);
                                    block.setBlockData(dir);
                                }

                                crop.setLastGrowth(now);
                                cropChunk.setDirty(true);
                                break;
                            }
                        }
                    }
                }
                else if (CropUtils.isStemBlock(crop.getMaterial())) {
                    crop.setLastGrowth(System.currentTimeMillis());
                }
            }
        }
    }

    public ConcurrentHashMap<String, CropChunk> getAllCropChunks() {
        return crops;
    }

    public CropData getCropData(Location location) {
        String chunkID = LocationUtils.getChunkID(location.getChunk());
        CropChunk cropChunk = crops.get(chunkID);
        if (cropChunk != null) {
            for (CropData crop : cropChunk.getCrops()) {
                if (crop.equals(location)) {
                    return crop;
                }
            }
        }
        return null; // Not found
    }

    public void updateCropItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }
        int price = settingsManager.getSellPrice(itemStack.getType());
        if (price == -1) {
            return;
        }
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(
                StringUtils.colorGraident(settingsManager.getCustomCropName()
                        .replace("%crop_name%", StringUtils.getType(itemStack.getType()))
                        .replace("%price%", String.valueOf(price)))
        );
        meta.setLore(
                settingsManager.getCustomCropLore().stream()
                        .map(line -> StringUtils.colorGraident(line.replace("%crop_name%", StringUtils.getType(itemStack.getType()))
                                .replace("%price%", String.valueOf(price))))
                        .toList()
        );
        itemStack.setItemMeta(meta);
    }
}