package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.managers.ArtManager;
import fun.milkyway.milkypixelart.managers.BannerManager;
import fun.milkyway.milkypixelart.managers.CopyrightManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.block.Banner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BannerProtectionListener implements Listener {

    @EventHandler
    public void onBannerCopy(PrepareItemCraftEvent event) {
        BannerManager artManager = BannerManager.getInstance();
        if (event.getInventory().getHolder() instanceof Player player) {
            CraftingInventory inventory = event.getInventory();
            List<ItemStack> clearBanners = getClearBannersFromCraft(inventory.getMatrix());
            List<ItemStack> patternedBanners = getPatternedBannersFromCraft(inventory.getMatrix());
            if (clearBanners.size() == 1 && patternedBanners.size() == 1 &&
            clearBanners.get(0).getType().equals(patternedBanners.get(0).getType())) {
                CopyrightManager.Author author = artManager.getAuthor(patternedBanners.get(0));
                if (author != null) {
                    if (!author.getUuid().equals(player.getUniqueId())) {
                        inventory.setResult(null);
                        player.sendMessage(Component.text("Вы не можете копировать чужие защищенные баннеры!").color(TextColor.fromHexString("#FF995E")));
                    }
                    else {
                        inventory.setResult(artManager.getUnprotectedCopy(patternedBanners.get(0)));
                        player.sendMessage(Component.text("Помните, копии защищенных баннеров не являются защищенными!").color(TextColor.fromHexString("#FFFF99")));
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBannerBreak(BlockDropItemEvent event) {
        if (event.getBlockState() instanceof Banner bannerState) {
            BannerManager bannerManager = BannerManager.getInstance();
            CopyrightManager.Author author = bannerManager.getAuthor(bannerState);
            if (author != null) {
                if (event.getItems().size() == 1) {
                    ItemStack banner = event.getItems().get(0).getItemStack();
                    bannerManager.protect(author.getUuid(), author.getName(), banner);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBannerPlace(BlockPlaceEvent blockPlaceEvent) {
        if (ArtManager.isBanner(blockPlaceEvent.getBlock().getType())) {
            BannerManager bannerManager = BannerManager.getInstance();
            Banner banner = (Banner) blockPlaceEvent.getBlock().getState();
            ItemStack itemStack = blockPlaceEvent.getItemInHand();
            CopyrightManager.Author author = bannerManager.getAuthor(itemStack);
            if (author != null) {
                bannerManager.protect(author.getUuid(), author.getName(), banner);
                banner.update();
            }
        }
    }

    private List<ItemStack> getPatternedBannersFromCraft(ItemStack[] itemStacks) {
        List<ItemStack> banners = new ArrayList<>();
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null && BannerManager.getInstance().hasPatterns(itemStack)) {
                banners.add(itemStack);
            }
        }
        return banners;
    }

    private List<ItemStack> getClearBannersFromCraft(ItemStack[] itemStacks) {
        List<ItemStack> banners = new ArrayList<>();
        for (ItemStack itemStack : itemStacks) {
            if (ArtManager.isBanner(itemStack) &&
                    !BannerManager.getInstance().hasPatterns(itemStack)) {
                banners.add(itemStack);
            }
        }
        return banners;
    }
}
