package fun.milkyway.milkypixelart.utils;

import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.ArtManager;
import fun.milkyway.milkypixelart.managers.LangManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class BannerUtils {

    public static @Nullable Pattern getPatternFromBannerItem(@NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() instanceof BannerMeta bannerMeta) {
            if (bannerMeta.numberOfPatterns() > 0) {
                return bannerMeta.getPatterns().get(0);
            }
        }
        return null;
    }

    public static @NotNull Material getDyeMaterial(@NotNull DyeColor dyeColor) {
        try {
            String materialName = dyeColor.name();
            return Material.valueOf(materialName+"_DYE");
        }
        catch (Exception exception) {
            MilkyPixelart.getInstance().getLogger().log(Level.WARNING, exception.getMessage(), exception);
            return Material.WHITE_DYE;
        }
    }

    public static @NotNull Material getDyePane(@NotNull DyeColor dyeColor) {
        try {
            String materialName = dyeColor.name();
            return Material.valueOf(materialName+"_STAINED_GLASS_PANE");
        }
        catch (Exception exception) {
            MilkyPixelart.getInstance().getLogger().log(Level.WARNING, exception.getMessage(), exception);
            return Material.BLACK_STAINED_GLASS_PANE;
        }
    }

    public static @NotNull DyeColor getDyeColor(@NotNull ItemStack bannerItemStack) {
        if (ArtManager.isBanner(bannerItemStack)) {
            String typeName = bannerItemStack.getType().name();
            try {
                String bannerColor = typeName.substring(0, typeName.lastIndexOf('_'));
                return DyeColor.valueOf(bannerColor);
            }
            catch (Exception exception) {
                MilkyPixelart.getInstance().getLogger().log(Level.WARNING, exception.getMessage(), exception);
                return DyeColor.WHITE;
            }
        }
        return DyeColor.WHITE;
    }

    public static @Nullable DyeColor getDyeColorFromItemStack(@NotNull ItemStack itemStack) {
        return switch (itemStack.getType()) {
            case WHITE_DYE -> DyeColor.WHITE;
            case LIGHT_GRAY_DYE -> DyeColor.LIGHT_GRAY;
            case GRAY_DYE -> DyeColor.GRAY;
            case BLACK_DYE -> DyeColor.BLACK;
            case GREEN_DYE -> DyeColor.GREEN;
            case LIME_DYE -> DyeColor.LIME;
            case CYAN_DYE -> DyeColor.CYAN;
            case LIGHT_BLUE_DYE -> DyeColor.LIGHT_BLUE;
            case BLUE_DYE -> DyeColor.BLUE;
            case BROWN_DYE -> DyeColor.BROWN;
            case YELLOW_DYE -> DyeColor.YELLOW;
            case ORANGE_DYE -> DyeColor.ORANGE;
            case RED_DYE -> DyeColor.RED;
            case PINK_DYE -> DyeColor.PINK;
            case PURPLE_DYE -> DyeColor.PURPLE;
            case MAGENTA_DYE -> DyeColor.MAGENTA;
            default -> null;
        };
    }

    public static @NotNull TextColor getTextColorFromDye(@NotNull DyeColor dyeColor) {
        TextColor textColor = switch (dyeColor) {
            case WHITE -> NamedTextColor.WHITE;
            case LIGHT_GRAY -> TextColor.fromHexString("#878787");
            case GRAY -> TextColor.fromHexString("#414141");
            case GREEN -> TextColor.fromHexString("#2F5F00");
            case LIME -> TextColor.fromHexString("#5EB200");
            case CYAN -> TextColor.fromHexString("#359B8B");
            case LIGHT_BLUE -> NamedTextColor.BLUE;
            case BLUE -> NamedTextColor.DARK_BLUE;
            case BROWN -> TextColor.fromHexString("#924E26");
            case YELLOW -> NamedTextColor.YELLOW;
            case ORANGE -> TextColor.fromHexString("#FF9500");
            case RED -> NamedTextColor.RED;
            case PINK -> TextColor.fromHexString("#FFAEF3");
            case PURPLE -> NamedTextColor.DARK_PURPLE;
            case MAGENTA -> NamedTextColor.LIGHT_PURPLE;
            default -> NamedTextColor.BLACK;
        };
        return textColor != null ? textColor : NamedTextColor.BLACK;
    }

    public static @NotNull ItemStack generateInfoBook(@NotNull TextColor textColor) {
        ItemStack infoBook = new ItemStack(Material.BOOK, 1);
        ItemMeta bookMeta = infoBook.getItemMeta();
        bookMeta.displayName(LangManager.getInstance().getLang("banner_paint.apply.menu.information.title").color(textColor));
        bookMeta.lore(LangManager.getInstance().getLangList("banner_paint.apply.menu.information.lines"));
        infoBook.setItemMeta(bookMeta);
        return infoBook;
    }
}
