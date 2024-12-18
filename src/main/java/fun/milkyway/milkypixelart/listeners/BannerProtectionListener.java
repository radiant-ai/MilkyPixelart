package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.managers.ArtManager;
import fun.milkyway.milkypixelart.managers.BannerManager;
import fun.milkyway.milkypixelart.managers.CopyrightManager;
import fun.milkyway.milkypixelart.managers.LangManager;
import fun.milkyway.milkypixelart.utils.MessageOnceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Banner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BannerProtectionListener implements Listener {

    private final MessageOnceManager messageOnceManager;

    public BannerProtectionListener() {
        messageOnceManager = new MessageOnceManager();
    }

    @EventHandler
    public void onBannerCopy(PrepareItemCraftEvent event) {
        BannerManager artManager = BannerManager.getInstance();
        if (event.getInventory().getHolder() instanceof Player player) {
            CraftingInventory inventory = event.getInventory();
            BannerCraftDetails bannerCraftDetails = bannerDuplicate(inventory.getMatrix());
            if (bannerCraftDetails.getPatternedBanners().size() == 1) {
                ItemStack copiedBanner = bannerCraftDetails.getPatternedBanners().getFirst().clone();
                CopyrightManager.Author author = artManager.getAuthor(copiedBanner);
                //BANNER DUPLICATE RECIPE
                if (bannerCraftDetails.getClearBanners().size() == 1 && bannerCraftDetails.getOtherItems().isEmpty()
                        && bannerCraftDetails.getPatternedBanners().getFirst().getAmount() == 1) {
                    ItemStack clearbanner = bannerCraftDetails.getClearBanners().getFirst().clone();
                    if (copiedBanner.getType().equals(clearbanner.getType())) {
                        if (author != null && !author.getUuid().equals(player.getUniqueId())) {
                            inventory.setResult(null);
                            player.sendMessage(LangManager.getInstance().getLang("copy.banner.fail_not_your_banner"));
                        }
                        else {
                            ItemStack result = artManager.getUnprotectedCopy(copiedBanner);
                            result.setAmount(1);
                            inventory.setResult(result);
                            if (author != null) {
                                messageOnceManager.sendMessageOnce(player,
                                        LangManager.getInstance().getLang("copy.banner.unprotected_reminder"));
                            }
                        }
                    }
                }
                // SHIELD BANNER ATTACH RECIPE
                else if (bannerCraftDetails.getClearBanners().isEmpty() && bannerCraftDetails.getOtherItems().size() == 1 &&
                        bannerCraftDetails.getOtherItems().getFirst().getType().equals(Material.SHIELD)) {
                    ItemStack shield = bannerCraftDetails.getOtherItems().getFirst().clone();
                    if (!artManager.hasShieldPatterns(shield)) {
                        ItemStack resultShield = artManager.applyBannerToShield(shield, copiedBanner);
                        if (author != null) {
                            artManager.hidePatterns(resultShield);
                        }
                        inventory.setResult(resultShield);
                    }
                }
                else {
                    inventory.setResult(null);
                }
            }
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!event.getRecipe().getKey().equals(NamespacedKey.minecraft("banner_duplicate"))) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBannerCopyOversize(InventoryClickEvent event) {
        BannerManager artManager = BannerManager.getInstance();
        if (event.getView().getTopInventory() instanceof CraftingInventory craftingInventory
        && event.getSlotType().equals(InventoryType.SlotType.RESULT)
        && event.getCurrentItem() != null
        && ArtManager.isBanner(event.getCurrentItem())) {
            ItemStack banner = event.getCurrentItem().clone();
            if (artManager.patternNumber(banner) > 6) {
                switch (event.getAction()) {
                    case PICKUP_ALL, MOVE_TO_OTHER_INVENTORY -> {
                        BannerCraftDetails craftDetails = bannerDuplicate(craftingInventory.getMatrix());
                        if (craftDetails.getClearBanners().size() == 1 && craftDetails.getPatternedBanners().size() == 1 &&
                                craftDetails.getOtherItems().isEmpty()) {
                            banner.setAmount(craftDetails.getClearBanners().getFirst().getAmount());
                            event.getView().setCursor(banner);
                            ItemStack[] newMatrix = new ItemStack[craftingInventory.getMatrix().length];
                            ItemStack patteredBanner = craftDetails.getPatternedBanners().getFirst().clone();
                            patteredBanner.setAmount(1);
                            newMatrix[0] = patteredBanner;
                            craftingInventory.setMatrix(newMatrix);
                        }
                        else {
                            event.setCancelled(true);
                        }
                    }
                    default -> event.setCancelled(true);
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
                    ItemStack banner = event.getItems().getFirst().getItemStack();
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
                        BannerManager.getInstance().patternNumber(itemStack) == 0) {
                    clearBanners.add(itemStack);
                }
                else if (ArtManager.isBanner(itemStack) &&
                        BannerManager.getInstance().patternNumber(itemStack) > 0) {
                    patternedBanners.add(itemStack);
                }
                else if (!itemStack.getType().equals(Material.AIR)) {
                    otherItems.add(itemStack);
                }
            }
        }
        return new BannerCraftDetails(clearBanners, patternedBanners, otherItems);
    }

    private static class BannerCraftDetails {
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
