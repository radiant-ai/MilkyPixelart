package fun.milkyway.milkypixelart.ownedblocks;

import fun.milkyway.milkypixelart.MilkyPixelart;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class OwnedBlockManager {
    private static final NamespacedKey bannerLocationsKey = new NamespacedKey(MilkyPixelart.getInstance(), "bannerlocations");
    private static final OwnedBlocksPDCMap ownedBlocksPDCMap = new OwnedBlocksPDCMap();
    public @Nullable UUID popAuthorOfPlaced(@NotNull Location location) {
        Chunk chunk = location.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (pdc.has(bannerLocationsKey)) {
            OwnedBlockMap ownedBlocks = pdc.get(bannerLocationsKey, ownedBlocksPDCMap);
            if (ownedBlocks == null || ownedBlocks.isEmpty()) {
                pdc.remove(bannerLocationsKey);
                return null;
            }
            else {
                OwnedBlock ownedBlock = ownedBlocks.remove(location);
                if (ownedBlock != null) {
                    if (ownedBlocks.isEmpty()) {
                        pdc.remove(bannerLocationsKey);
                    }
                    return ownedBlock.getOwnerUUID();
                }
            }
        }
        return null;
    }

    public void storeAuthorOfPlaced(@NotNull Location location, @NotNull UUID uuid) {
        Chunk chunk = location.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        OwnedBlockMap ownedBlockMap = null;

        if (pdc.has(bannerLocationsKey)) {
            ownedBlockMap = pdc.get(bannerLocationsKey, ownedBlocksPDCMap);
        }

        if (ownedBlockMap == null) {
            ownedBlockMap = new OwnedBlockMap();
        }

        ownedBlockMap.put(location, uuid);
        pdc.set(bannerLocationsKey, ownedBlocksPDCMap, ownedBlockMap);
    }
}
