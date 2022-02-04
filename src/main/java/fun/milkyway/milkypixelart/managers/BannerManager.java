package fun.milkyway.milkypixelart.managers;

import fun.milkyway.milkypixelart.listeners.BannerProtectionListener;
import org.bukkit.block.Banner;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BannerManager extends ArtManager {
    private static BannerManager instance;

    public BannerManager() {
        super();
        registerListener(new BannerProtectionListener());
    }

    public synchronized static @NotNull BannerManager getInstance() {
        if (instance == null) {
            instance = new BannerManager();
        }
        return instance;
    }

    @Override
    public boolean protect(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (ArtManager.isBanner(itemStack) && hasPatterns(itemStack)) {
            copyrightManager.protect(player, itemStack);
            return true;
        }
        return false;
    }

    @Override
    public boolean protect(@NotNull UUID uuid, @Nullable String name, @NotNull ItemStack itemStack) {
        if (ArtManager.isBanner(itemStack) && hasPatterns(itemStack)) {
            copyrightManager.protect(uuid, name, itemStack);
            return true;
        }
        return false;
    }

    public void protect(@NotNull UUID uuid, @Nullable String name, @NotNull Banner banner) {
        copyrightManager.protect(uuid, name, banner);
    }

    @Override
    public @Nullable CopyrightManager.Author getAuthor(ItemStack itemStack) {
        return copyrightManager.getAuthor(itemStack);
    }

    public @Nullable CopyrightManager.Author getAuthor(@NotNull Banner banner) {
        return copyrightManager.getAuthor(banner);
    }

    @Override
    public @NotNull ItemStack getUnprotectedCopy(ItemStack itemStack) {
        return copyrightManager.getUnprotectedCopy(itemStack);
    }

    @Override
    public int getProtectionCost() {
        return 20;
    }

    public boolean hasPatterns(@NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() instanceof BannerMeta bannerMeta) {
            return !bannerMeta.getPatterns().isEmpty();
        }
        return false;
    }

    @Override
    public void shutdown() {
        unregisterListeners();
    }
}
