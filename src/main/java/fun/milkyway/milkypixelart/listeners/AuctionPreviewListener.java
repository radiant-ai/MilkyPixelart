package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import fun.milkyway.milkypixelart.utils.MaterialUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AuctionPreviewListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRightClick(InventoryClickEvent event) {
        if (!event.getClick().isRightClick()) {
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }
        if (!event.isCancelled()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (MilkyPixelart.getInstance().getConfig().getBoolean("pixelarts.previewEverywhere", false)) {
            preview(player, event.getCurrentItem());
            return;
        }
        var currentInventoryName = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        var previewInventories = MilkyPixelart.getInstance().getConfig().getStringList("pixelarts.previewInventories");
        var isPreviewInventory = previewInventories.stream().anyMatch(currentInventoryName::contains);
        if (isPreviewInventory) {
            preview(player, event.getCurrentItem());
        }
    }

    private void preview(Player player, ItemStack stack) {
        var result = false;
        if (stack.getType() == Material.FILLED_MAP) {
            result = PixelartManager.getInstance().renderArts(player, List.of(stack), 1, 1);
        }
        else if (MaterialUtils.isBundle(stack.getType())) {
            result = PixelartManager.getInstance().renderBundle(player, stack);
        }
        if (result) {
            player.closeInventory();
        }
    }
}
