package me.castiel.customcrops.currency;

import me.castiel.customcrops.CustomCropsPlugin;

public class CubeCurrency {

    private final String playerUUID;
    private long balance;
    private boolean dirty = true;

    public CubeCurrency(String playerUUID, long initialBalance) {
        this.playerUUID = playerUUID;
        this.balance = initialBalance;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public long getBalance() {
        return balance;
    }

    public void set(long amount) {
        if (amount < 0) {
            CustomCropsPlugin.getInstance().getLogger().severe("Attempted to set a negative balance for player " + playerUUID + ": " + amount);
            return;
        }
        this.balance = amount;
        this.dirty = true;
    }

    public void add(long amount) {
        this.balance += amount;
    }

    public void remove(long amount) {
        this.balance -= amount;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
