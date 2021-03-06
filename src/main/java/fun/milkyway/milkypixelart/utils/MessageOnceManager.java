package fun.milkyway.milkypixelart.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class MessageOnceManager {
    private final Map<UUID, Integer> uuidIntegerMap;

    public MessageOnceManager() {
        this.uuidIntegerMap = new WeakHashMap<>();
    }

    public void sendMessageOnce(@NotNull Player player, @NotNull Component component) {
        Integer lastTick = uuidIntegerMap.get(player.getUniqueId());
        if (lastTick == null || Bukkit.getServer().getCurrentTick() > lastTick+20) {
            player.sendMessage(component);
            uuidIntegerMap.put(player.getUniqueId(), Bukkit.getServer().getCurrentTick());
        }
    }
}
