package me.castiel.customcrops.util;

import com.iridium.iridiumcolorapi.IridiumColorAPI;
import me.castiel.customcrops.CustomCropsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class StringUtils {

    public static String color(final String message) {
        String s = message.replace('&', ChatColor.COLOR_CHAR);
        return s.replace("%prefix%", CustomCropsPlugin.getInstance().getSettingsManager().getMessage("Prefix"));
    }

    public static String colorGraident(final String message) {
        String s = message.replace('&', ChatColor.COLOR_CHAR);
        return IridiumColorAPI.process(s.replace("%prefix%", CustomCropsPlugin.getInstance().getSettingsManager().getMessage("Prefix")));
    }

    public static String getType(Material material) {
        String name = material.name().replace("_", " ");
        String[] parts = name.split(" ");
        StringBuilder typeName = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                typeName.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return typeName.toString().trim();
    }
}
