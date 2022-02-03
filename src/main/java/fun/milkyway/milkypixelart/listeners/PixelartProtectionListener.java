package fun.milkyway.milkypixelart.listeners;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import fun.milkyway.milkypixelart.managers.ArtManager;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PixelartProtectionListener implements Listener {

    @EventHandler
    public void onPixelartCopyWorkbench(PrepareItemCraftEvent event) {
        PixelartManager artManager = PixelartManager.getInstance();
        if (event.getInventory().getHolder() instanceof Player player) {
            CraftingInventory inventory = event.getInventory();
            List<ItemStack> filledMaps = getFilledMapsFromCraft(inventory.getMatrix());
            if (filledMaps.stream().anyMatch(itemStack -> artManager.getAuthor(itemStack) != null)) {
                player.sendMessage(Component.text("Работы с защищенными картами производятся только на столе картографа!")
                        .color(TextColor.fromHexString("#FF995E")));
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onPixelartCopyCartography(PrepareResultEvent event) {
        PixelartManager artManager = PixelartManager.getInstance();
        if (event.getViewers().size() == 1 &&
                event.getViewers().get(0) instanceof Player player &&
                event.getInventory() instanceof CartographyInventory cartographyInventory) {
            ItemStack upperSlot = cartographyInventory.getItem(0);
            ItemStack lowerSlot = cartographyInventory.getItem(1);
            if (lowerSlot != null
                    && lowerSlot.getType().equals(Material.MAP) && ArtManager.isMap(upperSlot)) {
                UUID authorUUID = artManager.getAuthor(upperSlot);
                if (authorUUID != null && authorUUID.equals(player.getUniqueId())) {
                    ItemStack result = artManager.getUnprotectedCopy(upperSlot);
                    result.setAmount(2);
                    event.setResult(result);
                    player.sendMessage(Component.text("Помните, копии защищенных артов не являются защищенными!").color(NamedTextColor.YELLOW));
                }
                else if (authorUUID != null && !authorUUID.equals(player.getUniqueId())) {
                    event.setResult(null);
                    player.sendMessage(Component.text("Вы не можете копировать чужие защищенные пиксельарты!").color(TextColor.fromHexString("#FF995E")));
                    player.closeInventory();
                }
            }
        }
    }

    private List<ItemStack> getFilledMapsFromCraft(ItemStack[] itemStacks) {
        List<ItemStack> maps = new ArrayList<>();
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null && itemStack.getType().equals(Material.FILLED_MAP)) {
                maps .add(itemStack);
            }
        }
        return maps ;
    }
}
