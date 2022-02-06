package fun.milkyway.milkypixelart.managers;

import fun.milkyway.milkypixelart.listeners.BannerProtectionListener;
import org.bukkit.DyeColor;
import org.bukkit.block.Banner;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
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

    public boolean hasShieldPatterns(@NotNull ItemStack shieldItemStack) {
        return shieldItemStack.getItemMeta() instanceof BlockStateMeta blockStateMeta &&
                blockStateMeta.hasBlockState();
    }

    public @NotNull ItemStack applyBannerToShield(@NotNull ItemStack shieldItemStack,
                                    @NotNull ItemStack bannerItemStack) {
        ItemStack result = shieldItemStack.clone();
        if (bannerItemStack.getItemMeta() instanceof BannerMeta bannerMeta) {
            if (bannerMeta.numberOfPatterns() > 0) {
                if (result.getItemMeta() instanceof BlockStateMeta blockStateMeta
                && blockStateMeta.getBlockState() instanceof Banner shieldMeta) {
                    shieldMeta.setBaseColor(getDyeColor(bannerItemStack));
                    shieldMeta.setPatterns(bannerMeta.getPatterns());
                    blockStateMeta.setBlockState(shieldMeta);
                    result.setItemMeta(blockStateMeta);
                }
            }
        }
        return result;
    }

    public void hidePatterns(@NotNull ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            copyrightManager.hidePatterns(itemMeta);
            itemStack.setItemMeta(itemMeta);
        }
    }

    private @NotNull DyeColor getDyeColor(@NotNull ItemStack bannerItemStack) {
        if (ArtManager.isBanner(bannerItemStack)) {
            String typeName = bannerItemStack.getType().name();
            String bannerColor = typeName.substring(0, typeName.lastIndexOf('_'));
            return DyeColor.valueOf(bannerColor);
        }
        return DyeColor.WHITE;
    }

    @Override
    public void shutdown() {
        unregisterListeners();
    }
}
