package fun.milkyway.milkypixelart.utils;

import fun.milkyway.milkypixelart.MilkyPixelart;
import org.jetbrains.annotations.NotNull;

public class Versions {
    public static @NotNull String getNMSVersion() {
        return switch (MilkyPixelart.getInstance().getServer().getMinecraftVersion()) {
            case "1.21", "1.21.1" -> "v1_21_R1";
            default -> throw new IllegalStateException("Unsupported minecraft version: " + MilkyPixelart.getInstance().getServer().getMinecraftVersion());
        };
    }

    public static VersionLevel getVersionLevel() {
        return switch (MilkyPixelart.getInstance().getServer().getMinecraftVersion()) {
            case "1.21", "1.21.1", "1.21.4" -> VersionLevel.v1_21;
            default -> throw new IllegalStateException("Unsupported minecraft version: " + MilkyPixelart.getInstance().getServer().getMinecraftVersion());
        };
    }

    public enum VersionLevel {
        v1_21
    }
}
