package fun.milkyway.milkypixelart.ownedblocks;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class OwnedBlockMap extends HashMap<Integer, OwnedBlock> {
    public @Nullable OwnedBlock remove(Location location) {
        return remove(OwnedBlock.toChunkBlock(location));
    }
    public @Nullable OwnedBlock remove(Vector location) {
        return super.remove(OwnedBlock.packBlockLocation(location));
    }
    public OwnedBlock put(Location location, UUID uuid) {
        return put(OwnedBlock.toChunkBlock(location), uuid);
    }
    public OwnedBlock put(Vector location, UUID uuid) {
        OwnedBlock ownedBlock = new OwnedBlock(uuid, location);
        return super.put(ownedBlock.getPackedBlockLocation(), ownedBlock);
    }
}
