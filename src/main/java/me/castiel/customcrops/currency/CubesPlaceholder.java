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

        if (params.equalsIgnoreCase("formatted")) {
            return formatBalance(bal);
        }

        return String.valueOf(bal);
    }

    private String formatBalance(long balance) {
        if (balance >= 1_000_000) {
            return String.format("%.2fM", balance / 1_000_000.0);
        } else if (balance >= 1_000) {
            double value = balance / 1_000.0;
            return value % 1 == 0 ? String.format("%.0fk", value) : String.format("%.2fk", value);
        } else {
            return String.valueOf(balance);
        }
    }
}