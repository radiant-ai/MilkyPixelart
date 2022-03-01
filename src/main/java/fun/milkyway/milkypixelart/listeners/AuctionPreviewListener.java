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
                UUID playerUUID = player.getUniqueId();

                pixelartManager.renderArtToUser(player, stack).thenAccept(result -> {
                    Bukkit.getScheduler().runTask(MilkyPixelart.getInstance(), () -> {
                        Player player1 = Bukkit.getPlayer(playerUUID);

                        if (player1 == null || !player1.isOnline()) {
                            return;
                        }

                        if (result) {
                            if (player1.getVehicle() != null) {
                                player1.leaveVehicle();
                            }
                            Location l = player1.getLocation();
                            l.setPitch(0);
                            l.setYaw(Utils.alignYaw(l));
                            player1.teleport(l);
                            player1.sendActionBar(Component.text("Включен предпросмотр!").color(NamedTextColor.GREEN));
                        }
                        else {
                            player1.sendActionBar(Component.text("Ошибка предпросмотра!").color(NamedTextColor.RED));
                        }
                    });
                });

                player.closeInventory();
                event.setCancelled(true);
            }
        }
    }
}
