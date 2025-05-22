package fun.milkyway.milkypixelart.utils;

import fun.milkyway.milkypixelart.managers.ArtManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BundleArt {
    private final int width;
    private final int height;
    private final List<ItemStack> itemStacks;

    private BundleArt(List<ItemStack> itemStacks, int width, int height) {
        this.itemStacks = itemStacks;
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<ItemStack> getItemStacks() {
        return itemStacks;
    }

    public static @Nullable BundleArt of(ItemStack itemStack) {
        if (itemStack.getType() == Material.FILLED_MAP) {
            return new BundleArt(List.of(itemStack), 1, 1);
        }
        if (!MaterialUtils.isBundle(itemStack.getType())) {
            return null;
        }
        if (!(itemStack.getItemMeta() instanceof BundleMeta bundleMeta)) {
            return null;
        }
        var name = bundleMeta.displayName();
        if (name == null) {
            return null;
        }
        var plainName = PlainTextComponentSerializer.plainText().serialize(name);
        var dimensions = parseDimensions(plainName);
        if (dimensions == null) {
            return null;
        }
        var width = dimensions[0];
        var height = dimensions[1];
        if (width < 1 || height < 1 || width > 64 || height > 64) {
            return null;
        }
        var items = bundleMeta.getItems().reversed().stream().filter(ArtManager::isMap).map(ItemStack::clone).limit(width * height).toList();
        if (items.size() != width * height) {
            return null;
        }
        return new BundleArt(items, dimensions[0], dimensions[1]);
    }

    private static int @Nullable [] parseDimensions(String input) {
        Pattern pattern = Pattern.compile("(\\d+)\\D(\\d+)$");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            int first = Integer.parseInt(matcher.group(1));
            int second = Integer.parseInt(matcher.group(2));
            return new int[]{first, second};
        } else {
            return null;
        }
    }
}
