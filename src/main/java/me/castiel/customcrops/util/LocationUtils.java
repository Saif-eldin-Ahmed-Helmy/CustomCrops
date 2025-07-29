package me.castiel.customcrops.util;

import org.bukkit.Chunk;

public class LocationUtils {


    public static String getChunkID(int x, int z, String worldName) {
        return worldName + ":" + x + ":" + z;
    }

    public static String getChunkID(Chunk chunk) {
        return getChunkID(chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
    }
}
