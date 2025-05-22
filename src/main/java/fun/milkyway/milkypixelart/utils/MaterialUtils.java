package fun.milkyway.milkypixelart.utils;

import org.bukkit.Material;

public class MaterialUtils {
    public static boolean isBundle(Material material) {
        return material.name().endsWith("BUNDLE");
    }
}
