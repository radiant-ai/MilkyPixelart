package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MapPreviewListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PixelartManager.getInstance().hidePreviewArts(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PixelartManager.getInstance().clearPreviewArts(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onItemFrameDestroy(HangingBreakEvent event) {
        if (!MilkyPixelart.getInstance().getConfig().getBoolean("pixelarts.physicsProtection", true)) {
            return;
        }
        if (!(event.getEntity() instanceof ItemFrame itemFrame)) {
            return;
        }
        if (event.getCause() != HangingBreakEvent.RemoveCause.PHYSICS) {
            return;
        }
        var block = itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
        if (!block.getType().isSolid()) {
            return;
        }
        event.setCancelled(true);
    }
}
