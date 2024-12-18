package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import fun.milkyway.milkypixelart.utils.Utils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class AuctionPreviewListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRightClick(InventoryClickEvent event) {
        if (event.getClick().isRightClick() &&
                event.getClickedInventory() != null &&
                event.getCurrentItem() != null &&
                event.getCurrentItem().getType().equals(Material.FILLED_MAP) &&
                event.isCancelled() &&
                event.getWhoClicked() instanceof Player player) {
            String invName = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
            if (MilkyPixelart.getInstance().getConfiguration().getBoolean("pixelarts.previewEverywhere", false)
                    || Utils.containsAny(invName, MilkyPixelart.getInstance().getConfiguration().getStringList("pixelarts.previewInventories"))) {

                ItemStack stack = event.getCurrentItem();

                PixelartManager.getInstance().renderArt(player, stack);

                player.closeInventory();
            }
        }
    }
}
