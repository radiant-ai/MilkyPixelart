package fun.milkyway.milkypixelart.utils;

import fun.milkyway.milkypixelart.MilkyPixelart;

public class Versions {
    public static String getNMSVersion() {
        return switch (MilkyPixelart.getInstance().getServer().getMinecraftVersion()) {
            case "1.21", "1.21.1" -> "v1_21_R1";
            case "1.20.4" -> "v1_20_R3";
            case "1.19.4" -> "v1_19_R3";
            case "1.18.2" -> "v1_18_R2";
            default -> throw new IllegalStateException("Unsupported minecraft version: " + MilkyPixelart.getInstance().getServer().getMinecraftVersion());
        };
    }

    public static VersionLevel getVersionLevel() {
        return switch (MilkyPixelart.getInstance().getServer().getMinecraftVersion()) {
            case "1.21", "1.21.1" -> VersionLevel.v1_21;
            case "1.20.4", "1.19.4" -> VersionLevel.v1_20;
            case "1.18.2" -> VersionLevel.v1_18;
            default -> throw new IllegalStateException("Unsupported minecraft version: " + MilkyPixelart.getInstance().getServer().getMinecraftVersion());
        };
    }

    public enum VersionLevel {
        v1_18,
        v1_20,
        v1_21
    }
}
