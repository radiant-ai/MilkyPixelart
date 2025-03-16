package fun.milkyway.milkypixelart.utils;

import org.bukkit.Material;

public class MaterialUtils {
    public static boolean isBundle(Material material) {
        return material == Material.BUNDLE ||
                material == Material.BLACK_BUNDLE ||
                material == Material.BLUE_BUNDLE ||
                material == Material.BROWN_BUNDLE ||
                material == Material.CYAN_BUNDLE ||
                material == Material.GRAY_BUNDLE ||
                material == Material.GREEN_BUNDLE ||
                material == Material.LIGHT_BLUE_BUNDLE ||
                material == Material.LIGHT_GRAY_BUNDLE ||
                material == Material.LIME_BUNDLE ||
                material == Material.MAGENTA_BUNDLE ||
                material == Material.ORANGE_BUNDLE ||
                material == Material.PINK_BUNDLE ||
                material == Material.PURPLE_BUNDLE ||
                material == Material.RED_BUNDLE ||
                material == Material.WHITE_BUNDLE ||
                material == Material.YELLOW_BUNDLE;
    }
}
