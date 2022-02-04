package fun.milkyway.milkypixelart.ownedblocks;

import fun.milkyway.milkypixelart.MilkyPixelart;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.*;

public class OwnedBlock {
    private final UUID ownerUUID;
    private final Vector blockLocation;
    private final int packedBlockLocation;

    protected static final int OBJECT_LENGTH = 20;

    protected OwnedBlock(@NotNull UUID ownerUUID, @NotNull Vector blockLocation) {
        this.ownerUUID = ownerUUID;
        this.blockLocation = blockLocation;
        this.packedBlockLocation = packBlockLocation(blockLocation);
    }

    protected OwnedBlock(byte[] bytes) throws OwnedBlocksPDCException {
        if (bytes.length != OBJECT_LENGTH) {
            throw new OwnedBlocksPDCException("Expected to get "+OBJECT_LENGTH+" bytes, but received "+bytes.length+"instead!");
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        this.packedBlockLocation = byteBuffer.getInt();
        this.blockLocation = unpackBlockLocation(packedBlockLocation);
        this.ownerUUID = new UUID(byteBuffer.getLong(), byteBuffer.getLong());
    }

    public @NotNull UUID getOwnerUUID() {
        return ownerUUID;
    }

    @SuppressWarnings("unused")
    public @NotNull Vector getBlockLocation() {
        return blockLocation;
    }

    public int getPackedBlockLocation() {
        return packedBlockLocation;
    }

    protected byte[] getByteArray() {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[OBJECT_LENGTH]);
        byteBuffer.putInt(packedBlockLocation);
        byteBuffer.putLong(ownerUUID.getMostSignificantBits());
        byteBuffer.putLong(ownerUUID.getLeastSignificantBits());
        return byteBuffer.array();
    }

    protected static int packBlockLocation(@NotNull Vector location) {
        int blockKey = 256 * location.getBlockY() + 16 * location.getBlockZ() + location.getBlockX();
        MilkyPixelart.getInstance().getLogger().info(location+"--->"+blockKey);
        return blockKey;
    }

    protected static @NotNull Vector unpackBlockLocation(int blockKey) {
        Vector vector = new Vector(Math.floorMod(blockKey, 16), blockKey / 256, Math.floorMod((blockKey / 16), 16));
        MilkyPixelart.getInstance().getLogger().info(blockKey+"--->"+vector);
        return vector;
    }

    protected static @NotNull Vector toChunkBlock(@NotNull Location location) {
        Vector vector = new Vector(Math.floorMod(location.getBlockX(), 16), location.getBlockY(), Math.floorMod(location.getBlockZ(), 16));
        MilkyPixelart.getInstance().getLogger().info(location+"--->"+vector);
        return vector;
    }

    protected static byte[] bytesFromList(OwnedBlockMap complex) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[complex.size()*OBJECT_LENGTH]);
        for (OwnedBlock ownedBlock : complex.values()) {
            byteBuffer.put(ownedBlock.getByteArray());
        }
        return byteBuffer.array();
    }

    protected static @NotNull OwnedBlockMap listFromBytes(byte[] primitive) throws OwnedBlocksPDCException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(primitive);
        int blockNumber = primitive.length / OBJECT_LENGTH;
        OwnedBlockMap result = new OwnedBlockMap();
        for (int i = 0; i < blockNumber; i++) {
            byte[] bytes = new byte[OBJECT_LENGTH];
            byteBuffer.get(bytes);
            OwnedBlock block = new OwnedBlock(bytes);
            result.put(block.getPackedBlockLocation(), block);
        }
        return result;
    }
}
