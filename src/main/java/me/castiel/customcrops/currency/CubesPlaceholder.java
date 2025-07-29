package me.castiel.customcrops.currency;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.castiel.customcrops.CustomCropsPlugin;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CubesPlaceholder extends PlaceholderExpansion {

    private final CustomCropsPlugin plugin;
    private final CurrencyManager currencyManager;

    public CubesPlaceholder(CustomCropsPlugin plugin) {
        this.plugin = plugin;
        this.currencyManager = plugin.getCurrencyManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cubes";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Castiel";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "N/A";
        long bal = currencyManager.getBalance(player.getUniqueId().toString());
        return String.valueOf(bal);
    }
}
