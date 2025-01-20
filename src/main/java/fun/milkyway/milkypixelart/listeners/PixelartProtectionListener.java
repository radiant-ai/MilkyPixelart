package fun.milkyway.milkypixelart.listeners;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.ArtManager;
import fun.milkyway.milkypixelart.managers.CopyrightManager;
import fun.milkyway.milkypixelart.managers.LangManager;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import fun.milkyway.milkypixelart.utils.MessageOnceManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PixelartProtectionListener implements Listener {

    private final MessageOnceManager messageOnceManager;

    public PixelartProtectionListener() {
        messageOnceManager = new MessageOnceManager();
    }

    @EventHandler
    public void onPixelartCopyWorkbench(PrepareItemCraftEvent event) {
        PixelartManager artManager = PixelartManager.getInstance();
        if (event.getInventory().getHolder() instanceof Player player) {
            CraftingInventory inventory = event.getInventory();
            List<ItemStack> filledMaps = getFilledMapsFromCraft(inventory.getMatrix());
            if (filledMaps.stream().anyMatch(itemStack -> artManager.getAuthor(itemStack) != null)) {
                player.sendMessage(LangManager.getInstance().getLang("copy.pixelart.fail_need_cartography_table"));
                inventory.setResult(null);
                MilkyPixelart.getInstance().getServer().getGlobalRegionScheduler().runDelayed(MilkyPixelart.getInstance(), t -> {
                    Player newPlayer = MilkyPixelart.getInstance().getServer().getPlayer(player.getUniqueId());
                    if (newPlayer != null) {
                        newPlayer.closeInventory();
                    }
                }, 1);
            }
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!event.getRecipe().getKey().equals(NamespacedKey.minecraft("map_cloning"))) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPixelartCopyCartography(PrepareResultEvent event) {
        PixelartManager artManager = PixelartManager.getInstance();
        if (event.getViewers().size() == 1 &&
                event.getViewers().getFirst() instanceof Player player &&
                event.getInventory() instanceof CartographyInventory cartographyInventory) {
            ItemStack upperSlot = cartographyInventory.getItem(0);
            ItemStack lowerSlot = cartographyInventory.getItem(1);
            if (lowerSlot != null
                    && lowerSlot.getType().equals(Material.MAP) && ArtManager.isMap(upperSlot)) {
                CopyrightManager.Author author = artManager.getAuthor(upperSlot);
                if (author != null && author.getUuid().equals(player.getUniqueId())) {
                    ItemStack result = artManager.getUnprotectedCopy(upperSlot);
                    result.setAmount(2);
                    event.setResult(result);
                    messageOnceManager.sendMessageOnce(player,
                            LangManager.getInstance().getLang("copy.pixelart.unprotected_reminder"));
                } else if (author != null && !author.getUuid().equals(player.getUniqueId())) {
                    event.setResult(null);
                    player.sendMessage(LangManager.getInstance().getLang("copy.pixelart.fail_not_your_pixelart"));
                    MilkyPixelart.getInstance().getServer().getGlobalRegionScheduler().runDelayed(MilkyPixelart.getInstance(), t -> {
                        Player newPlayer = MilkyPixelart.getInstance().getServer().getPlayer(player.getUniqueId());
                        if (newPlayer != null) {
                            newPlayer.closeInventory();
                        }
                    }, 1);
                }
            }
        }
    }

    private @NotNull List<ItemStack> getFilledMapsFromCraft(ItemStack @NotNull [] itemStacks) {
        List<ItemStack> maps = new ArrayList<>();
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null && itemStack.getType().equals(Material.FILLED_MAP)) {
                maps.add(itemStack);
            }
        }
        return maps;
    }
}
