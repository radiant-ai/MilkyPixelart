package fun.milkyway.milkypixelart.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class IllegitimateArtListener implements Listener {
    private final MilkyPixelart plugin;

    public IllegitimateArtListener() {
        this.plugin = MilkyPixelart.getInstance();
    }

    @EventHandler
    public void onArtLoad(EntityAddToWorldEvent event) {
        PixelartManager pixelartManager = PixelartManager.getInstance();
        Entity entity = event.getEntity();
        if (entity instanceof ItemFrame itemFrame) {
            ItemStack itemStack = itemFrame.getItem();
            if (itemStack.getType().equals(Material.FILLED_MAP)) {
                if (!pixelartManager.isLegitimateOwner(itemStack)) {
                    itemFrame.setItem(null);
                    MilkyPixelart.getInstance().getLogger().info(ChatColor.DARK_GREEN + "Removed an illegitimate pixelart at: " + itemFrame.getLocation());
                    plugin.getLogger().info(ChatColor.DARK_GREEN + "Map id: " + pixelartManager.getMapId(itemStack));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArtInventory(InventoryOpenEvent event) {
        PixelartManager pixelartManager = PixelartManager.getInstance();
        if (event.getPlayer() instanceof Player player) {
            HashMap<Integer, ? extends ItemStack> map = event.getInventory().all(Material.FILLED_MAP);
            for (Map.Entry<Integer, ? extends ItemStack> entry : map.entrySet()) {
                if (!pixelartManager.isLegitimateOwner(entry.getValue())) {
                    event.getInventory().clear(entry.getKey());
                    if (event.getInventory().getLocation() != null) {
                        plugin.getLogger().info(ChatColor.DARK_GREEN + "Removed an illegitimate pixelart at: " + event.getInventory().getLocation());
                    } else {
                        plugin.getLogger().info(ChatColor.DARK_GREEN + "Removed an illegitimate art from " +
                                PlainTextComponentSerializer.plainText().serialize(event.getView().title()) +
                                " of player " + player.getName());
                    }
                    plugin.getLogger().info(ChatColor.DARK_GREEN + "Map id: " + pixelartManager.getMapId(entry.getValue()));
                }
            }
        }
    }
}
