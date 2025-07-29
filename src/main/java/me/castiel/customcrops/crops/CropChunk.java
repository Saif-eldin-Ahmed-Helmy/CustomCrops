package me.castiel.customcrops.crops;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CropChunk {

    private final int X;
    private final int Z;
    private final String worldName;
    private final Set<CropData> crops;
    private boolean isLoaded;
    private boolean isDirty;

    public CropChunk(int X, int Z, String worldName) {
        this.X = X;
        this.Z = Z;
        this.worldName = worldName;
        this.crops = ConcurrentHashMap.newKeySet();
    }
    public int getX() {
        return X;
    }
    public int getZ() {
        return Z;
    }
    public String getWorldName() {
        return worldName;
    }

    public Set<CropData> getCrops() {
        return crops;
    }

    public void addCrop(CropData crop) {
        crops.add(crop);
    }

    public void removeCrop(CropData crop) {
        crops.remove(crop);
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }
}
