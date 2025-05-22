package fun.milkyway.milkypixelart.managers;

import fun.milkyway.milkypixelart.MilkyPixelart;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public abstract class ArtManager {
    protected final MilkyPixelart plugin;

    protected List<Listener> listeners;

    public ArtManager() {
        this.plugin = MilkyPixelart.getInstance();
        this.listeners = new LinkedList<>();
        //Always inject copyright manager
        CopyrightManager.getInstance();
    }

    protected void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listeners.add(listener);
    }

    protected void unregisterListeners() {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners = new LinkedList<>();
    }

    public abstract boolean protect(@NotNull Player player, @NotNull ItemStack itemStack);

    public abstract boolean protect(@NotNull UUID uuid, @Nullable String name, @NotNull ItemStack itemStack);

    public void unProtect(@NotNull ItemStack itemStack) {
        var newItem = getUnprotectedCopy(itemStack);
        itemStack.setItemMeta(newItem.getItemMeta());
    }

    public abstract @Nullable CopyrightManager.Author getAuthor(ItemStack itemStack);

    public abstract @NotNull ItemStack getUnprotectedCopy(ItemStack itemStack);

    public abstract int getProtectionCost();

    public abstract void shutdown() throws Exception;

    public static boolean isBanner(@Nullable ItemStack itemStack) {
        return itemStack != null && Tag.BANNERS.isTagged(itemStack.getType());
    }

    public static boolean isBanner(@Nullable Material material) {
        return material != null && Tag.BANNERS.isTagged(material);
    }

    public static boolean isMap(@Nullable ItemStack itemStack) {
        return itemStack != null && itemStack.getType().equals(Material.FILLED_MAP);
    }

    public static @Nullable ArtManager getInstance(@NotNull ItemStack itemStack) {
        if (isBanner(itemStack)) {
            return BannerManager.getInstance();
        }

        if (isMap(itemStack)) {
            return PixelartManager.getInstance();
        }
        return null;
    }
}
