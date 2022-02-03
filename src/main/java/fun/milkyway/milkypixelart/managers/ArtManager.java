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
    protected final CopyrightManager copyrightManager;

    protected List<Listener> listeners;

    public ArtManager() {
        this.plugin = MilkyPixelart.getInstance();
        this.copyrightManager = CopyrightManager.getInstance();
        this.listeners = new LinkedList<>();
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

    public abstract @Nullable UUID getAuthor(ItemStack itemStack);

    public abstract @NotNull ItemStack getUnprotectedCopy(ItemStack itemStack);

    public abstract int getProtectionCost();

    public abstract void shutdown();

    public static boolean isBanner(@Nullable ItemStack itemStack) {
        return itemStack != null && Tag.BANNERS.isTagged(itemStack.getType());
    }

    public static boolean isMap(@Nullable ItemStack itemStack) {
        return itemStack != null && itemStack.getType().equals(Material.FILLED_MAP);
    }
}
