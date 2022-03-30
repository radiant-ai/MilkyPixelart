package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import fun.milkyway.milkypixelart.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionPreviewListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClick(InventoryClickEvent event) {
        if (event.getClick().isRightClick() &&
                event.getClickedInventory() != null &&
                event.getCurrentItem() != null &&
                event.getCurrentItem().getType().equals(Material.FILLED_MAP)) {
            String invName = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
            if ((MilkyPixelart.getInstance().getConfiguration().getBoolean("pixelarts.previewEverywhere", false) ||
                    Utils.containsAny(invName, MilkyPixelart.getInstance().getConfiguration().getStringList("pixelarts.previewInventories")))
                    && event.getWhoClicked() instanceof Player player) {

                ItemStack stack = event.getCurrentItem();

                PixelartManager.getInstance().renderArt(player, stack);

                player.closeInventory();
                event.setCancelled(true);
            }
        }
    }
}
