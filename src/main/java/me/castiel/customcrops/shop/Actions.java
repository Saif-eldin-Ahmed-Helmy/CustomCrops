package me.castiel.customcrops.shop;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.Particles;
import com.cryptomorin.xseries.particles.XParticle;
import com.iridium.iridiumcolorapi.IridiumColorAPI;
import me.castiel.customcrops.CustomCropsPlugin;
import me.castiel.customcrops.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class Actions {

    public static void execute(Player player, String action) {
        if (action.startsWith("{player}")) {
            player.performCommand(IridiumColorAPI.process(StringUtils.color(action
                    .replace("{player} ", "")
                    .replace("%player%", player.getName()))));
        } else if (action.startsWith("{message}")) {
            sendMessage(player, action
                    .replace("{message} ", ""));
        } else if (action.startsWith("{broadcast}")) {
            Bukkit.getServer().broadcastMessage(IridiumColorAPI.process(StringUtils.color(action
                    .replace("{broadcast} ", "")
                    .replace("%player%", player.getName()))));
        } else if (action.startsWith("{action}")) {
            ActionBar.sendActionBar(player, IridiumColorAPI.process(StringUtils.color(action
                    .replace("{action} ", "")
                    .replace("%player%", player.getName()))));
        } else if (action.startsWith("{console}")) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), IridiumColorAPI.process(StringUtils.color(action
                    .replace("{console} ", "")
                    .replace("%player%", player.getName()))));
        } else if (action.startsWith("{title}")) {
            String[] title = IridiumColorAPI.process(StringUtils.color(action
                            .replace("{title} ", "")
                            .replace("%player%", player.getName())))
                    .split(";");
            Titles.sendTitle(player, title[0], title.length > 1 ? title[1] : "");
        } else if (action.startsWith("{sound}")) {
            XSound.play(action
                    .replace("{sound} ", "")
                    .replace("%player%", player.getName()), soundPlayer -> soundPlayer.forPlayers(player));
        } else if (action.startsWith("{particle}")) {
            try {
                Particles.circle(3, 20, ParticleDisplay.display(player.getLocation(), Objects.requireNonNull(XParticle.valueOf(action
                        .replace("{particle} ", "")).get())));
            }
            catch (Exception e) {
                CustomCropsPlugin.getInstance().getLogger().warning("Make sure this effect exists in this version, Invalid effect: " + action
                        .replace("{particle} ", "")
                        .replace("%prefix%", CustomCropsPlugin.getInstance().getSettingsManager().getMessage("Prefix"))
                        .replace("%player%", player.getName()));
            }
        } else if (action.startsWith("{close}")) {
            player.closeInventory();
        }
    }

    public static void sendMessage(CommandSender commandSender, String message, String... placeholders) {
        if (message == null || message.isEmpty()) {
            return;
        }
        String msg = message;
        for (int i = 0; i < placeholders.length; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        msg = StringUtils.color(msg);
        commandSender.sendMessage(IridiumColorAPI.process(msg.replace("%player%", commandSender.getName())));
    }
}