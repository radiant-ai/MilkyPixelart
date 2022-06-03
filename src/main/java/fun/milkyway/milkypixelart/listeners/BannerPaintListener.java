package fun.milkyway.milkypixelart.listeners;


import fun.milkyway.milkypixelart.managers.ArtManager;
import fun.milkyway.milkypixelart.managers.BannerManager;
import fun.milkyway.milkypixelart.managers.CopyrightManager;
import fun.milkyway.milkypixelart.managers.LangManager;
import fun.milkyway.milkypixelart.utils.BannerUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BannerPaintListener implements Listener {

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        BannerManager.getInstance().handleMenuClick(event);
    }

    @EventHandler
    public void onBannerPaint(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getHand() == null || event.getHand().equals(EquipmentSlot.HAND) ||
                !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null || !ArtManager.isBanner(block.getType())) {
            return;
        }

        DyeColor dyeColor = getDyeColorInHands(event.getPlayer());

        if (dyeColor == null) {
            return;
        }

        event.setCancelled(false);

        if (!isLoomNearby(block) && !hasLoom(player)) {
            player.sendActionBar(LangManager.getInstance().getLang("banner_paint.apply.fail_need_loom"));
            return;
        }

        if (!hasBlockAccess(block, player)) {
            player.sendActionBar(LangManager.getInstance().getLang("banner_paint.apply.fail_no_access"));
            return;
        }

        CopyrightManager.Author author = BannerManager.getInstance().getAuthor(block);

        if (author != null && !author.getUuid().equals(player.getUniqueId())) {
            player.sendActionBar(LangManager.getInstance().getLang("banner_paint.apply.fail_protected"));
            return;
        }

        BannerManager.getInstance().showPatternMenu(player, block, dyeColor);
    }

    @EventHandler
    public void onBannerErase(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getHand() == null || event.getHand().equals(EquipmentSlot.HAND) ||
                !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null || !ArtManager.isBanner(block.getType())) {
            return;
        }

        if (!hasPhantomMembrane(player)) {
            return;
        }

        event.setCancelled(false);

        if (!hasBlockAccess(block, player)) {
            player.sendActionBar(LangManager.getInstance().getLang("banner_paint.erase.fail_no_access"));
            return;
        }

        CopyrightManager.Author author = BannerManager.getInstance().getAuthor(block);

        if (author != null && !author.getUuid().equals(player.getUniqueId())) {
            player.sendActionBar(LangManager.getInstance().getLang("banner_paint.erase.fail_protected"));
            return;
        }

        if (!BannerManager.getInstance().eraseTopPattern(block)) {
            player.sendActionBar(LangManager.getInstance().getLang("banner_paint.erase.fail_no_pattern"));
            return;
        }

        player.sendActionBar(LangManager.getInstance().getLang("banner_paint.erase.success"));
        player.playSound(Sound.sound(Key.key(Key.MINECRAFT_NAMESPACE, "block.composter.ready"), Sound.Source.BLOCK, 0.5f, 1.2f));
        player.getInventory().removeItem(new ItemStack(Material.PHANTOM_MEMBRANE, 1));
    }

    @EventHandler
    public void bannerMenuClose(InventoryCloseEvent event) {
        BannerManager.getInstance().handleMenuClose(event);
    }

    private @Nullable DyeColor getDyeColorInHands(@NotNull Player player) {
        DyeColor result = BannerUtils.getDyeColorFromItemStack(player.getInventory().getItemInMainHand());
        result = result == null ? BannerUtils.getDyeColorFromItemStack(player.getInventory().getItemInOffHand()) : result;
        return result;
    }

    private boolean hasPhantomMembrane(@NotNull Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        return itemStack.getType().equals(Material.PHANTOM_MEMBRANE);
    }

    private boolean isLoomNearby(@NotNull Block bannerBlock) {
        for (int i = -4; i < 5; i++) {
            for (int j = -4; j < 5; j++) {
                for (int k = -1; k < 2; k++) {
                    Block block = bannerBlock.getLocation().getWorld().getBlockAt(
                            bannerBlock.getLocation().getBlockX()+i,
                            bannerBlock.getLocation().getBlockY()+k,
                            bannerBlock.getLocation().getBlockZ()+j);
                    if (block.getType().equals(Material.LOOM)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasLoom(@NotNull Player player) {
        return player.getInventory().contains(Material.LOOM) || player.hasPermission("pixelart.bypassloom");
    }

    private boolean hasBlockAccess(@NotNull Block block, @NotNull Player player) {
        BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
        Bukkit.getServer().getPluginManager().callEvent(blockBreakEvent);
        return !blockBreakEvent.isCancelled();
    }
}
