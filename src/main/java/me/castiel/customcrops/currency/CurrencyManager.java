package me.castiel.customcrops.currency;

import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.storage.CropDAO;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyManager {

    private final CropDAO cropDAO;
    private final AutoSaveTask autoSaveTask;
    private final ConcurrentHashMap<String, CubeCurrency> playerBalances;

    public CurrencyManager(CustomCropsPlugin plugin, CropDAO cropDAO) {
        this.autoSaveTask = new AutoSaveTask(plugin, this);
        this.cropDAO = cropDAO;
        playerBalances = new ConcurrentHashMap<>();
        loadSavedBalances();
    }

    public void loadSavedBalances() {
        cropDAO.loadAllBalances().whenCompleteAsync((balances, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                for (HashMap.Entry<String, Long> entry : balances.entrySet()) {
                    String playerUUID = entry.getKey();
                    Long balance = entry.getValue();
                    CubeCurrency cubeCurrency = new CubeCurrency(playerUUID, balance);
                    playerBalances.put(playerUUID, cubeCurrency);
                    cubeCurrency.setDirty(false);
                }
                CustomCropsPlugin.getInstance().getLogger().info("Loaded saved balances for " + playerBalances.size() + " players.");
            }
        });
    }

    public CubeCurrency getCurrency(String playerUUID) {
        return playerBalances.computeIfAbsent(playerUUID, uuid -> new CubeCurrency(uuid, 0L));
    }

    public void addCurrency(String playerUUID, Long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to add must be non-negative");
        }
        CubeCurrency currency = getCurrency(playerUUID);
        currency.add(amount);
        currency.setDirty(true);
    }

    public void removeCurrency(String playerUUID, Long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to remove must be non-negative");
        }
        CubeCurrency currency = getCurrency(playerUUID);
        if (currency.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        currency.remove(amount);
        currency.setDirty(true);
    }

    public Long getBalance(String playerUUID) {
        CubeCurrency currency = getCurrency(playerUUID);
        return currency.getBalance();
    }

    public boolean hasEnough(String playerUUID, Long amount) {
        return getBalance(playerUUID) >= amount;
    }

    public boolean transferCurrency(String fromPlayerUUID, String toPlayerUUID, Long amount) {
        if (hasEnough(fromPlayerUUID, amount)) {
            removeCurrency(fromPlayerUUID, amount);
            addCurrency(toPlayerUUID, amount);
            return true;
        }
        return false;
    }

    public void saveBalances(boolean async) {
        HashMap<String, Long> dirtyBalances = new HashMap<>();
        for (CubeCurrency currency : playerBalances.values()) {
            if (currency.isDirty()) {
                dirtyBalances.put(currency.getPlayerUUID(), currency.getBalance());
                currency.setDirty(false); // reset dirty flag after saving
            }
        }
        if (!dirtyBalances.isEmpty()) {
            if (async) {
                cropDAO.setBalances(dirtyBalances).whenCompleteAsync((result, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                    } else {
                        CustomCropsPlugin.getInstance().getLogger().info("Saved balances for " + dirtyBalances.size() + " players.");
                    }
                });
            }
            else {
                cropDAO.setBalancesSync(dirtyBalances);
            }
        }
    }

    public void stopAutoSave() {
        autoSaveTask.stop();
    }
}
