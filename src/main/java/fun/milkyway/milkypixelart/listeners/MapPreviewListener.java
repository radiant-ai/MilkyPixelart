package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.managers.PixelartManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
}
