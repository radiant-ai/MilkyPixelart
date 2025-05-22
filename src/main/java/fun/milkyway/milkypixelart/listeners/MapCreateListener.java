package fun.milkyway.milkypixelart.listeners;

import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.LangManager;
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
        if (!MilkyPixelart.getInstance().getConfig().getBoolean("pixelarts.respectRegionProtection", false)) {
            return;
        }

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
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
        event.getPlayer().sendActionBar(LangManager.getInstance().getLang("region_protection.pixelart.fail_no_access"));
    }

    private boolean canBuild(@NotNull Player player) {
        String worldMapArt = MilkyPixelart.getInstance().getConfig().getString("pixelarts.worldMapArt", "world");
        if (player.getWorld().getName().equals(worldMapArt) && player.getLocation().getY() > 255) {
            return false;
        }

        var block = player.getWorld().getBlockAt(player.getLocation());
        var blockBreakEvent = new BlockBreakEvent(block, player);
        blockBreakEvent.callEvent();
        return !blockBreakEvent.isCancelled();
    }

}
