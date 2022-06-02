package fun.milkyway.milkypixelart.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

// Listens for players using empty map to restrict them from
// creating maps in places where they cannot build
public class MapCreateEvent implements Listener {

    @EventHandler
    public void onMapUse(PlayerInteractEvent event) {
        event.getPlayer().sendMessage(event.getAction().name());

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
    }

    // Checks if player can build at the top block
    private boolean canBuild(@NotNull Player player) {
        var block = player.getWorld().getHighestBlockAt(player.getLocation());
        var blockPlaceEvent = new BlockBreakEvent(block, player);
        blockPlaceEvent.callEvent();
        return !blockPlaceEvent.isCancelled();
    }
}
