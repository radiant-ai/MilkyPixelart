package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.managers.ArtManager;
import fun.milkyway.milkypixelart.managers.BannerManager;
import fun.milkyway.milkypixelart.managers.CopyrightManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BannerProtectionListener implements Listener {

    @EventHandler
    public void onBannerCopy(PrepareItemCraftEvent event) {
        BannerManager artManager = BannerManager.getInstance();
        if (event.getInventory().getHolder() instanceof Player player) {
            CraftingInventory inventory = event.getInventory();
            BannerCraftDetails bannerCraftDetails = bannerDuplicate(inventory.getMatrix());
            if (bannerCraftDetails.getPatternedBanners().size() == 1) {
                ItemStack copiedBanner = bannerCraftDetails.getPatternedBanners().get(0);
                CopyrightManager.Author author = artManager.getAuthor(copiedBanner);
                if (author != null) {
                    //BANNER DUPLICATE RECIPE
                    if (bannerCraftDetails.getClearBanners().size() == 1 && bannerCraftDetails.getOtherItems().size() == 0) {
                        ItemStack clearbanner = bannerCraftDetails.getClearBanners().get(0);
                        if (copiedBanner.getType().equals(clearbanner.getType())) {
                            if (!author.getUuid().equals(player.getUniqueId())) {
                                inventory.setResult(null);
                                player.sendMessage(Component.text("Вы не можете копировать чужие защищенные баннеры!").color(TextColor.fromHexString("#FF995E")));
                            }
                            else {
                                ItemStack result = artManager.getUnprotectedCopy(copiedBanner);
                                copiedBanner.setAmount(1);
                                inventory.setResult(result);
                                player.sendMessage(Component.text("Помните, копии защищенных баннеров не являются защищенными!").color(TextColor.fromHexString("#FFFF99")));
                            }
                        }
                    }
                    // SHIELD BANNER ATTACH RECIPE
                    else if (bannerCraftDetails.getClearBanners().size() == 0 && bannerCraftDetails.getOtherItems().size() == 1 &&
                            bannerCraftDetails.getOtherItems().get(0).getType().equals(Material.SHIELD)) {
                        ItemStack shield = bannerCraftDetails.getOtherItems().get(0);
                        if (!artManager.hasShieldPatterns(shield)) {
                            ItemStack resultShield = artManager.applyBannerToShield(shield, copiedBanner);
                            artManager.hidePatterns(resultShield);
                            inventory.setResult(resultShield);
                        }
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

    private @NotNull BannerCraftDetails bannerDuplicate(ItemStack[] itemStacks) {
        List<ItemStack> clearBanners = new ArrayList<>();
        List<ItemStack> patternedBanners = new ArrayList<>();
        List<ItemStack> otherItems = new ArrayList<>();
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null) {
                if (ArtManager.isBanner(itemStack) &&
                        !BannerManager.getInstance().hasPatterns(itemStack)) {
                    clearBanners.add(itemStack);
                }
                else if (ArtManager.isBanner(itemStack) &&
                        BannerManager.getInstance().hasPatterns(itemStack)) {
                    patternedBanners.add(itemStack);
                }
                else if (!itemStack.getType().equals(Material.AIR)) {
                    otherItems.add(itemStack);
                }
            }
        }
        return new BannerCraftDetails(clearBanners, patternedBanners, otherItems);
    }

    private class BannerCraftDetails {
        private final List<ItemStack> clearBanners;
        private final List<ItemStack> patternedBanners;
        private final List<ItemStack> otherItems;

        private BannerCraftDetails(@NotNull List<ItemStack> clearBanners,
                                   @NotNull List<ItemStack> patternedBanners,
                                   @NotNull List<ItemStack> otherItems) {
            this.clearBanners = clearBanners;
            this.patternedBanners = patternedBanners;
            this.otherItems = otherItems;
        }

        public @NotNull List<ItemStack> getClearBanners() {
            return clearBanners;
        }

        public @NotNull List<ItemStack> getPatternedBanners() {
            return patternedBanners;
        }

        public @NotNull List<ItemStack> getOtherItems() {
            return otherItems;
        }
    }
}
