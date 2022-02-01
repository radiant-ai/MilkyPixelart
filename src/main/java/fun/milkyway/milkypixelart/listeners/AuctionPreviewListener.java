package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.pixelartmanager.PixelartManager;
import fun.milkyway.milkypixelart.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class AuctionPreviewListener implements Listener {
    private final PixelartManager pixelartManager;

    public AuctionPreviewListener(PixelartManager pixelartManager) {
        this.pixelartManager = pixelartManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClick(InventoryClickEvent event) {
        if (event.getClick().isRightClick() &&
                event.getClickedInventory() != null &&
                event.getClickedInventory().getLocation() == null &&
                event.getCurrentItem().getType().equals(Material.FILLED_MAP)) {
            String invName = event.getView().title().toString();
            if ((invName.contains("Рынок")
                    || invName.contains("Поиск")
                    || invName.contains("Pixelart")
                    || invName.contains("Просмотр"))
                    && event.getWhoClicked() instanceof Player p) {
                ItemStack stack = event.getCurrentItem();
                p.closeInventory();
                pixelartManager.renderArtToUser(p, stack);
                Location l = p.getLocation();
                l.setPitch(0);
                l.setYaw(Utils.alignYaw(l));
                p.teleport(l);
                p.sendMessage(Component.text("Вы предпросматриваете выбранный пиксельарт!").color(NamedTextColor.GREEN));
                event.setCancelled(true);
            }
        }
    }
}
