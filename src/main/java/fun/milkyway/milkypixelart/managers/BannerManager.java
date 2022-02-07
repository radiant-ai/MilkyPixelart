package fun.milkyway.milkypixelart.managers;

import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.listeners.BannerPaintListener;
import fun.milkyway.milkypixelart.listeners.BannerProtectionListener;
import fun.milkyway.milkypixelart.utils.BannerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class BannerManager extends ArtManager {
    private static BannerManager instance;

    private final Map<InventoryView, Block> bannerEditorMenus;

    public BannerManager() {
        super();
        registerListener(new BannerProtectionListener());
        registerListener(new BannerPaintListener());
        bannerEditorMenus = new HashMap<>();
    }

    public synchronized static @NotNull BannerManager getInstance() {
        if (instance == null) {
            instance = new BannerManager();
        }
        return instance;
    }

    @Override
    public boolean protect(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (ArtManager.isBanner(itemStack) && patternNumber(itemStack) > 0) {
            copyrightManager.protect(player, itemStack);
            return true;
        }
        return false;
    }

    @Override
    public boolean protect(@NotNull UUID uuid, @Nullable String name, @NotNull ItemStack itemStack) {
        if (ArtManager.isBanner(itemStack) && patternNumber(itemStack) > 0) {
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

    public @Nullable CopyrightManager.Author getAuthor(@NotNull Block block) {
        if (block.getState() instanceof Banner banner) {
            return getAuthor(banner);
        }
        return null;
    }

    @Override
    public @NotNull ItemStack getUnprotectedCopy(ItemStack itemStack) {
        return copyrightManager.getUnprotectedCopy(itemStack);
    }

    @Override
    public int getProtectionCost() {
        return 20;
    }

    public int patternNumber(@NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() instanceof BannerMeta bannerMeta) {
            return bannerMeta.numberOfPatterns();
        }
        return 0;
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
                    shieldMeta.setBaseColor(BannerUtils.getDyeColor(bannerItemStack));
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

    public void handleMenuClick(@NotNull InventoryClickEvent event) {
        Block block = bannerEditorMenus.get(event.getView());
        if (block != null && ArtManager.isBanner(block.getType())) {
            try {
                if (!(event.getClickedInventory() instanceof PlayerInventory)) {
                    ItemStack itemStack = event.getCurrentItem();
                    if (itemStack != null) {
                        Pattern pattern = BannerUtils.getPatternFromBannerItem(itemStack);
                        if (pattern != null && event.getView().getPlayer() instanceof Player player) {
                            Material requiredDye = BannerUtils.getDyeMaterial(pattern.getColor());
                            if (block.getState() instanceof Banner banner) {
                                CopyrightManager.Author author = getAuthor(banner);
                                if (author != null && !author.getUuid().equals(player.getUniqueId())) {
                                    player.sendMessage(Component.text("Вы не можете добавлять узоры на чужие баннеры!").color(TextColor.fromHexString("#FF995E")));
                                }
                                else {
                                    if (player.getInventory().contains(requiredDye)) {
                                        if (addPatternToBanner(block, pattern)) {
                                            player.sendMessage(Component.text("Узор добавлен.").color(TextColor.fromHexString("#9AFF0F")));
                                            player.getInventory().removeItem(new ItemStack(requiredDye, 1));
                                        }
                                        else {
                                            player.sendMessage(Component.text("Сюда больше нельзя ничего добавлять!").color(TextColor.fromHexString("#FF995E")));
                                        }
                                    }
                                    else {
                                        player.sendMessage(Component.text("У вас не оказалось нужной краски!").color(TextColor.fromHexString("#FF995E")));
                                    }
                                }
                            }
                            player.closeInventory();
                        }
                    }
                }
            }
            catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, exception.getMessage(), exception);
            }
            event.setCancelled(true);
        }
    }

    public void handleMenuClose(InventoryCloseEvent event) {
        bannerEditorMenus.remove(event.getView());
    }

    private boolean addPatternToBanner(@NotNull Block block, @NotNull Pattern pattern) {
        if (block.getState() instanceof Banner bannerState) {
            if (bannerState.numberOfPatterns() < 16) {
                List<Pattern> patternList = bannerState.getPatterns();
                patternList.add(pattern);
                bannerState.setPatterns(patternList);
                bannerState.update();
                return true;
            }
        }
        return false;
    }

    public void showPatternMenu(@NotNull Player player, @NotNull Block block, @NotNull DyeColor dyeColor) {
        TextColor textColor = BannerUtils.getTextColorFromDye(dyeColor);
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text("Наложение узора")
                .color(textColor).decorate(TextDecoration.BOLD));
        int count = 0;
        for (PatternType patternType : PatternType.values()) {
            if (patternType.equals(PatternType.BASE)) {
                continue;
            }
            if (patternType.equals(PatternType.SKULL) &&
            !player.getInventory().contains(Material.SKULL_BANNER_PATTERN)) {
                continue;
            }
            if (patternType.equals(PatternType.PIGLIN) &&
                    !player.getInventory().contains(Material.PIGLIN_BANNER_PATTERN)) {
                continue;
            }
            if (patternType.equals(PatternType.GLOBE) &&
                    !player.getInventory().contains(Material.GLOBE_BANNER_PATTERN)) {
                continue;
            }
            if (patternType.equals(PatternType.FLOWER) &&
                    !player.getInventory().contains(Material.FLOWER_BANNER_PATTERN)) {
                continue;
            }
            if (patternType.equals(PatternType.CREEPER) &&
                    !player.getInventory().contains(Material.CREEPER_BANNER_PATTERN)) {
                continue;
            }
            if (patternType.equals(PatternType.MOJANG) &&
                    !player.getInventory().contains(Material.MOJANG_BANNER_PATTERN)) {
                continue;
            }
            ItemStack banner;
            if (dyeColor.equals(DyeColor.WHITE)) {
                banner = new ItemStack(Material.BLACK_BANNER, 1);
            }
            else {
                banner = new ItemStack(Material.WHITE_BANNER, 1);
            }
            BannerMeta bannerMeta = (BannerMeta) banner.getItemMeta();
            bannerMeta.setPatterns(List.of(new Pattern(dyeColor, patternType)));
            banner.setItemMeta(bannerMeta);
            inventory.setItem(count, banner);
            count++;
        }

        ItemStack blackStainedGlass = new ItemStack(BannerUtils.getDyePane(dyeColor), 1);
        ItemMeta itemMeta = blackStainedGlass.getItemMeta();
        itemMeta.displayName(Component.text(""));
        blackStainedGlass.setItemMeta(itemMeta);
        for (int i = 45; i < 54; i++) {
            if (i != 49) {
                inventory.setItem(i, blackStainedGlass);
            }
        }
        inventory.setItem(49, BannerUtils.generateInfoBook(textColor));
        InventoryView inventoryView = player.openInventory(inventory);
        bannerEditorMenus.put(inventoryView, block);
    }

    @Override
    public void shutdown() {
        unregisterListeners();
        for (InventoryView inventoryView : bannerEditorMenus.keySet()) {
            if (MilkyPixelart.getInstance().isEnabled()) {
                Bukkit.getScheduler().runTask(MilkyPixelart.getInstance(), () -> {
                    inventoryView.getPlayer().closeInventory();
                });
            }
        }
    }
}
