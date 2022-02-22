package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import fun.milkyway.milkypixelart.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class AuctionPreviewListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClick(InventoryClickEvent event) {
        PixelartManager pixelartManager = PixelartManager.getInstance();
        if (event.getClick().isRightClick() &&
                event.getClickedInventory() != null &&
                event.getClickedInventory().getLocation() == null &&
                event.getCurrentItem() != null &&
                event.getCurrentItem().getType().equals(Material.FILLED_MAP)) {
            String invName = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
            if (Utils.containsAny(invName, MilkyPixelart.getInstance().getConfiguration().getStringList("pixelarts.previewInventories"))
                    && event.getWhoClicked() instanceof Player player) {
                ItemStack stack = event.getCurrentItem();
                player.closeInventory();
                pixelartManager.renderArtToUser(player, stack);
                Location l = player.getLocation();
                l.setPitch(0);
                l.setYaw(Utils.alignYaw(l));
                player.teleport(l);
                player.sendActionBar(Component.text("Включен предпросмотр!").color(NamedTextColor.GREEN));
                event.setCancelled(true);
            }
        }
    }
}
