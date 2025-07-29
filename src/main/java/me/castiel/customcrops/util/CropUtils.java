package me.castiel.customcrops.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;

import java.util.EnumSet;
import java.util.Set;

public class CropUtils {

    public static boolean isCrop(Block block) {
        return isCrop(block.getType());
    }

    public static boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, COCOA, MELON_STEM, PUMPKIN_STEM, ATTACHED_MELON_STEM, ATTACHED_PUMPKIN_STEM,
                 SUGAR_CANE, CACTUS -> true;
            default -> false;
        };
    }

    public static boolean isGrowableNotAgeable(Material material) {
        return switch (material) {
            case SUGAR_CANE, CACTUS -> true;
            default -> false;
        };
    }

    public static Block findGrowableBaseBlock(Block block) {
        Material type = block.getType();
        Block below = block;
        while (below.getRelative(BlockFace.DOWN).getType() == type) {
            below = below.getRelative(BlockFace.DOWN);
        }
        return below;
    }

    public static Block findGrownCropStem(Block fruitBlock) {
        Material fruitType = fruitBlock.getType();

        // Only applies to melon or pumpkin
        if (fruitType != Material.MELON && fruitType != Material.PUMPKIN) return null;

        // Check all 4 cardinal directions
        for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST }) {
            Block possibleStem = fruitBlock.getRelative(face);

            if (fruitType == Material.MELON && possibleStem.getType() == Material.ATTACHED_MELON_STEM ||
                    fruitType == Material.PUMPKIN && possibleStem.getType() == Material.ATTACHED_PUMPKIN_STEM) {

                BlockData data = possibleStem.getBlockData();

                if (data instanceof Directional directional) {
                    // Stem must be facing *toward* the fruit
                    if (directional.getFacing() == face.getOppositeFace()) {
                        return possibleStem;
                    }
                }
            }
        }
        return null; // No grown attached stem found
    }

    public static boolean canGrowGrowable(Block block) {
        Material type = block.getType();
        if (!isGrowableNotAgeable(type)) {
            return false;
        }

        Block base = findGrowableBaseBlock(block);
        int height = 1;
        Block current = base;

        // Count the total height
        while (current.getRelative(BlockFace.UP).getType() == type) {
            height++;
            current = current.getRelative(BlockFace.UP);
        }

        // Check if the next block above is air (i.e., growth possible)
        Block above = current.getRelative(BlockFace.UP);
        boolean hasSpaceAbove = above.getType() == Material.AIR;

        return height < 3 && hasSpaceAbove;
    }

    public static boolean canGrowAgeable(BlockData blockData) {
        if (isStemBlock(blockData.getMaterial())) {
            return true;
        }
        if (blockData instanceof Ageable ageable) {
            return ageable.getAge() < ageable.getMaximumAge();
        }
        return isGrowableNotAgeable(blockData.getMaterial());
    }

    private static final Set<Material> VALID_STEM_SOIL = EnumSet.of(
            Material.FARMLAND,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.GRASS_BLOCK,
            Material.PODZOL,
            Material.ROOTED_DIRT,
            Material.MOSS_BLOCK,
            Material.MUDDY_MANGROVE_ROOTS,
            Material.MUD
    );

    public static boolean isValidStemSoil(Material material) {
        return VALID_STEM_SOIL.contains(material);
    }
    public static boolean isStemBlock(Material material) {
        return material == Material.MELON_STEM || material == Material.PUMPKIN_STEM
                || material == Material.ATTACHED_MELON_STEM || material == Material.ATTACHED_PUMPKIN_STEM;
    }
}
