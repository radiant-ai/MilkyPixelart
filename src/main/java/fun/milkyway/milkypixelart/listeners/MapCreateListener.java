package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.MilkyPixelart;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;


public class MapCreateListener implements Listener {

    @EventHandler
    public void onMapUse(PlayerInteractEvent event) {
        if (!MilkyPixelart.getInstance().getConfiguration().getBoolean("pixelarts.respectRegionProtection", false)) {
            return;
        }

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) &&
                        !event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            return;
        }

        if (!event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.MAP) &&
                !event.getPlayer().getInventory().getItemInOffHand().getType().equals(Material.MAP)) {
            return;
        }

        if (canBuild(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text("Вы не можете создавать карты тут.")
                .color(TextColor.fromHexString("#FF995E")));
    }

    private boolean canBuild(@NotNull Player player) {
        var block = player.getWorld().getHighestBlockAt(player.getLocation());
        var blockPlaceEvent = new BlockBreakEvent(block, player);
        blockPlaceEvent.callEvent();
        return !blockPlaceEvent.isCancelled();
    }
}
