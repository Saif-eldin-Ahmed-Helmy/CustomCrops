package me.castiel.customcrops.commands;

import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.currency.CurrencyManager;
import me.castiel.customcrops.shop.Actions;
import me.castiel.customcrops.shop.RotatingShopGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CubesCommand implements CommandExecutor, TabCompleter {

    private final CustomCropsPlugin plugin;
    private final CurrencyManager currencyManager;

    public CubesCommand(CustomCropsPlugin plugin) {
        this.plugin = plugin;
        this.currencyManager = plugin.getCurrencyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure we have at least one argument
        if (args.length == 0) {
            Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidArguments"));
            return true;
        }

        String sub = args[0].toLowerCase();

        // Balance commands
        if (sub.equals("bal") || sub.equals("balance")) {
            // /cubes bal or /cubes bal <player>
            if (args.length == 1) {
                if (!(sender instanceof Player player)) {
                    Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("PlayerOnlyCommand"));
                    return true;
                }
                long balance = currencyManager.getBalance(player.getUniqueId().toString());
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("Balance")
                        .replace("%cubes%", String.valueOf(balance)));
                return true;
            } else if (args.length == 2) {
                String targetName = args[1];
                Player target = plugin.getServer().getPlayerExact(targetName);
                if (target == null) {
                    Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("PlayerNotFound")
                            .replace("%player%", targetName));
                    return true;
                }
                long balance = currencyManager.getBalance(target.getUniqueId().toString());
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("BalanceOther")
                        .replace("%player%", target.getName())
                        .replace("%cubes%", String.valueOf(balance)));
                return true;
            } else {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidArguments"));
                return true;
            }
        }

        // Set command
        if (sub.equals("set")) {
            if (!sender.hasPermission("customcrops.admin")) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("NoPermission"));
                return true;
            }
            if (args.length != 3) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidArguments"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("PlayerNotFound"));
                return true;
            }
            try {
                long amount = Long.parseLong(args[2]);
                if (amount < 0) {
                    Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidNumber"));
                    return true;
                }
                currencyManager.getCurrency(target.getUniqueId().toString()).setDirty(true);
                currencyManager.getCurrency(target.getUniqueId().toString()).set(amount);
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("Set")
                        .replace("%player%", target.getName())
                        .replace("%amount%", String.valueOf(amount)));
            } catch (NumberFormatException e) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidNumber"));
            }
            return true;
        }

        // Give command
        if (sub.equals("give")) {
            if (!sender.hasPermission("customcrops.admin")) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("NoPermission"));
                return true;
            }
            if (args.length != 3) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidArguments"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("PlayerNotFound"));
                return true;
            }
            try {
                long amount = Long.parseLong(args[2]);
                if (amount < 0) {
                    Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidNumber"));
                    return true;
                }
                currencyManager.getCurrency(target.getUniqueId().toString()).setDirty(true);
                currencyManager.getCurrency(target.getUniqueId().toString()).add(amount);
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("Give")
                        .replace("%player%", target.getName())
                        .replace("%amount%", String.valueOf(amount)));
            } catch (NumberFormatException e) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidNumber"));
            }
            return true;
        }

        // Pay command
        if (sub.equals("pay")) {
            if (!(sender instanceof Player)) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("PlayerOnlyCommand"));
                return true;
            }
            if (args.length != 3) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidArguments"));
                return true;
            }
            Player payer = (Player) sender;
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("PlayerNotFound"));
                return true;
            }
            try {
                long amount = Long.parseLong(args[2]);
                currencyManager.removeCurrency(payer.getUniqueId().toString(), amount);
                currencyManager.addCurrency(target.getUniqueId().toString(), amount);
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("Pay")
                        .replace("%player%", target.getName())
                        .replace("%amount%", String.valueOf(amount)));
                Actions.sendMessage(target, plugin.getSettingsManager().getMessage("Receive")
                        .replace("%amount%", String.valueOf(amount)));
            } catch (NumberFormatException e) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidNumber"));
            }
            return true;
        }

        // Sell command
        if (sub.equals("sell")) {
            if (!sender.hasPermission("customcrops.admin")) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("NoPermission"));
                return true;
            }
            if (args.length != 2) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidArguments"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("PlayerNotFound"));
                return true;
            }
            long totalCubes = 0;
            long totalItems = 0;
            for (ItemStack item : target.getInventory().getContents()) {
                if (item != null && plugin.getSettingsManager().getSellPrice(item.getType()) > 0) {
                    totalCubes += plugin.getSettingsManager().getSellPrice(item.getType()) * item.getAmount();
                    totalItems += item.getAmount();
                    item.setAmount(0);
                }
            }
            currencyManager.addCurrency(target.getUniqueId().toString(), totalCubes);
            Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("SoldCrops")
                    .replace("%player%", target.getName())
                    .replace("%amount%", String.valueOf(totalItems))
                    .replace("%total%", String.valueOf(totalCubes)));
            return true;
        }

        // Rotating shop command
        if (sub.equals("market") || sub.equals("shop") || sub.equals("rotatingshop")) {
            if (!sender.hasPermission("customcrops.admin")) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("NoPermission"));
                return true;
            }
            if (args.length != 2) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidArguments"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("PlayerNotFound"));
                return true;
            }
            new RotatingShopGUI().openShop(target);
            return true;
        }

        // Rotate shop now
        if (sub.equals("rotate")) {
            if (!sender.hasPermission("customcrops.admin")) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("NoPermission"));
                return true;
            }
            if (args.length != 1) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidArguments"));
                return true;
            }
            plugin.getRotatingShopManager().rotateNow();
            String msg = plugin.getSettingsManager().getMessage("ShopRotated");
            if (msg == null || msg.isEmpty()) msg = "Shop rotated.";
            Actions.sendMessage(sender, msg);
            return true;
        }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("customcrops.admin")) {
                Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("NoPermission"));
                return true;
            }
            plugin.getSettingsManager().reload();
            // Reload shop config and rotate to apply new items/rarities/slots immediately
            plugin.getRotatingShopManager().reloadFromConfig(true);
            Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("Reload"));
            String rotated = plugin.getSettingsManager().getMessage("ShopRotated");
            if (rotated != null && !rotated.isEmpty()) {
                Actions.sendMessage(sender, rotated);
            }
            return true;
        }

        // Invalid subcommand
        Actions.sendMessage(sender, plugin.getSettingsManager().getMessage("InvalidArguments"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("customcrops.admin")) {
                completions.add("give");
                completions.add("set");
                completions.add("sell");
                completions.add("market");
                completions.add("rotate");
                completions.add("reload");
            }
            completions.add("bal");
            completions.add("pay");
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ((sub.equals("give") || sub.equals("set") || sub.equals("bal") || sub.equals("pay") || sub.equals("sell") || sub.equals("market") || sub.equals("shop") || sub.equals("rotatingshop")) && sender.hasPermission("customcrops.admin")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if ((sub.equals("give") || sub.equals("set") || sub.equals("pay")) && sender.hasPermission("customcrops.admin")) {
                completions.add("100");
                completions.add("500");
                completions.add("1000");
            }
        }

        return completions;
    }
}