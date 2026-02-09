package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.managers.LangManager;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import me.Fupery.ArtMap.Easel.Easel;
import me.Fupery.ArtMap.Event.PlayerMountEaselEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ArtMapListener implements Listener {

    @EventHandler
    public void onPlayerMountEasel(PlayerMountEaselEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("artmap.admin")) {
            return;
        }

        Easel easel = event.getEasel();
        ItemStack item = easel.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }

        if (PixelartManager.getInstance().getAuthor(item) != null) {
            event.setCancelled(true);
            player.sendMessage(LangManager.getInstance().getLang("artmap.mount_protected"));
        }
    }
}
