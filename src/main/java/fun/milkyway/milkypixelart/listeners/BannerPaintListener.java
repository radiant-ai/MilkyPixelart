package fun.milkyway.milkypixelart.listeners;


import fun.milkyway.milkypixelart.managers.ArtManager;
import fun.milkyway.milkypixelart.managers.BannerManager;
import fun.milkyway.milkypixelart.managers.CopyrightManager;
import fun.milkyway.milkypixelart.utils.BannerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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
        if (event.getHand() != null && event.getHand().equals(EquipmentSlot.HAND) &&
                event.getAction().equals(Action.RIGHT_CLICK_BLOCK) &&
            !player.isSneaking()) {
            Block block = event.getClickedBlock();
            if (block != null && ArtManager.isBanner(block.getType())) {
                DyeColor dyeColor = getDyeColorInHands(event.getPlayer());
                if (dyeColor != null) {
                    if (isLoomNearby(block) || hasLoom(player)) {
                        BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
                        Bukkit.getServer().getPluginManager().callEvent(blockBreakEvent);
                        if (!blockBreakEvent.isCancelled()) {
                            CopyrightManager.Author author = BannerManager.getInstance().getAuthor(block);
                            if (author != null && !author.getUuid().equals(player.getUniqueId())) {
                                player.sendMessage(Component.text("Вы не можете добавлять узоры на чужие баннеры!").color(TextColor.fromHexString("#FF995E")));
                            }
                            else {
                                BannerManager.getInstance().showPatternMenu(player, block, dyeColor);
                            }
                        }
                        else {
                            player.sendMessage(Component.text("Вы не можете редактировать баннер тут!").color(TextColor.fromHexString("#FF995E")));
                        }
                        event.setCancelled(false);
                    }
                    else {
                        player.sendMessage(Component.text("Вы должны делать это у ткацкого станка или иметь один у себя в инвертаре!").color(TextColor.fromHexString("#FF995E")));
                    }
                }
            }
        }
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
        return player.getInventory().contains(Material.LOOM) || player.hasPermission("milkypixelart.bypassloom");
    }
}
