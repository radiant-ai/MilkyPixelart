package fun.milkyway.milkypixelart.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import fun.milkyway.milkypixelart.pixelartmanager.PixelartManager;
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
    private final PixelartManager pixelartManager;

    public IllegitimateArtListener(PixelartManager pixelartManager) {
        this.pixelartManager = pixelartManager;
    }

    @EventHandler
    public void onArtLoad(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemFrame itemFrame) {
            ItemStack itemStack = itemFrame.getItem();
            if (itemStack.getType().equals(Material.FILLED_MAP)) {
                if (!pixelartManager.isLegitimateOwner(itemStack)) {
                    itemFrame.setItem(null);
                    pixelartManager.getPlugin().getLogger().info(ChatColor.DARK_GREEN+"Removed an illegitimate pixelart at: "+itemFrame.getLocation());
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArtInventory(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            HashMap<Integer, ? extends ItemStack> map = event.getInventory().all(Material.FILLED_MAP);
            for (Map.Entry<Integer, ? extends ItemStack> entry : map.entrySet()) {
                if (!pixelartManager.isLegitimateOwner(entry.getValue())) {
                    event.getInventory().clear(entry.getKey());
                    if (event.getInventory().getLocation() != null) {
                        pixelartManager.getPlugin().getLogger().info(ChatColor.DARK_GREEN+"Removed an illegitimate pixelart at: "+event.getInventory().getLocation());
                    }
                    else {
                        pixelartManager.getPlugin().getLogger().info(ChatColor.DARK_GREEN+"Removed an illegitimate art from "+
                                PlainTextComponentSerializer.plainText().serialize(event.getView().title()) +
                                " of player "+player.getName());
                    }
                }
            }
        }
    }
}
