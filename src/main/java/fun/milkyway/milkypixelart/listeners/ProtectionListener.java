package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.pixelartmanager.PixelartManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;

import java.util.UUID;

public class ProtectionListener implements Listener {
    private final PixelartManager pixelartManager;

    public ProtectionListener(PixelartManager pixelartManager) {
        this.pixelartManager = pixelartManager;
    }

    @EventHandler
    public void onPixelartCopy(PrepareItemCraftEvent e) {
        CraftingInventory inv = e.getInventory();
        InventoryHolder ih = inv.getHolder();
        if (ih instanceof Player p) {
            for (int i = 0; i<inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() == Material.FILLED_MAP) {
                    UUID author = pixelartManager.getAuthor(item);
                    if (author!= null && !p.getUniqueId().equals(author)) {
                        inv.setResult(null);
                        p.sendMessage(Component.text("Вы не можете копировать чужие защищенные пиксельарты!").color(NamedTextColor.RED));
                        return;
                    }
                    else if (author!=null && p.getUniqueId().equals(author)) {
                        inv.setResult(null);
                        p.sendMessage(Component.text("Вам нужен стол картографа!").color(NamedTextColor.RED));
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPixelartCopy2(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            Inventory inv = e.getInventory();
            if (inv.getType() == InventoryType.CARTOGRAPHY) {
                if (e.getClick() == ClickType.NUMBER_KEY || e.getClick() == ClickType.SWAP_OFFHAND) {
                    e.setCancelled(true);
                    return;
                }
                ItemStack cursor = e.getCursor();
                ItemStack clickedItem = e.getCurrentItem();
                Inventory invClicked = e.getClickedInventory();
                ItemStack toCheck = null;
                if (invClicked != null && invClicked.getType() == InventoryType.PLAYER &&
                        (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT)) {
                    toCheck = clickedItem;
                }
                else if (invClicked != null && invClicked.getType() == InventoryType.CARTOGRAPHY) {
                    toCheck = cursor;
                }

                if (toCheck != null) {
                    UUID author = pixelartManager.getAuthor(toCheck);
                    if (author != null && !author.equals(p.getUniqueId())) {
                        //p.closeInventory();
                        e.setCancelled(true);
                        p.sendMessage(Component.text("Вы не можете копировать чужие защищенные пиксельарты!").color(NamedTextColor.RED));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPixelartCopy2(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            Inventory inv = e.getInventory();
            if (inv instanceof AnvilInventory || inv instanceof CartographyInventory) {
                if (e.getInventorySlots().contains(0) || e.getInventorySlots().contains(1)) {
                    ItemStack cursor = e.getOldCursor();
                    UUID author = pixelartManager.getAuthor(cursor);
                    if (author != null && !author.equals(p.getUniqueId())) {
                        //p.closeInventory();
                        e.setCancelled(true);
                        p.sendMessage(Component.text("Вы не можете копировать чужие защищенные пиксельарты!").color(NamedTextColor.RED));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPixelartCopyTake(InventoryClickEvent e) {
        Inventory inv = e.getClickedInventory();
        if (inv != null && inv.getType() == InventoryType.CARTOGRAPHY) {
            if (e.getClick() == ClickType.NUMBER_KEY || e.getClick() == ClickType.SWAP_OFFHAND) {
                e.setCancelled(true);
                return;
            }
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() == Material.FILLED_MAP && e.getSlot() == 2 && pixelartManager.getAuthor(clickedItem) != null &&
                    inv.getItem(1) != null && inv.getItem(1).getType() == Material.MAP) {
                ItemStack copy = pixelartManager.getUnprotectedCopy(clickedItem);
                ItemStack original = inv.getItem(0).clone();
                if (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.LEFT) {
                    ItemStack lessMaps1 = inv.getItem(1).clone();

                    if (e.getCursor() != null && e.getCursor().isSimilar(copy)) {
                        if (e.getCursor().getMaxStackSize()==e.getCursor().getAmount()) {
                            e.setCancelled(true);
                            return;
                        }
                        copy.setAmount(e.getCursor().getAmount()+1);
                        e.getWhoClicked().setItemOnCursor(copy);
                    }
                    else {
                        copy.setAmount(1);
                        e.getWhoClicked().setItemOnCursor(copy);
                    }

                    int lessAmt1 = lessMaps1.getAmount();
                    if (lessAmt1!=1) {
                        lessMaps1.setAmount(lessAmt1-1);
                        inv.setItem(1, lessMaps1);
                        inv.setItem(2, copy);
                    }
                    else {
                        inv.setItem(1, null);
                        inv.setItem(2, null);
                    }

                    inv.setItem(0, original);

                    e.setCancelled(true);
                    e.getWhoClicked().sendMessage(Component.text("Помните, копии защищенных артов не являются защищенными!").color(NamedTextColor.YELLOW));
                }
                else {
                    e.setCancelled(true);
                }
            }
        }
    }
}
