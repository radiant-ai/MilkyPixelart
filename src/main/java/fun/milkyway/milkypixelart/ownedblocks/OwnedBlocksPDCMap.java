package fun.milkyway.milkypixelart.ownedblocks;

import fun.milkyway.milkypixelart.MilkyPixelart;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;


public class OwnedBlocksPDCMap implements PersistentDataType<byte[], OwnedBlockMap> {

    @Override
    public @NotNull Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public @NotNull Class<OwnedBlockMap> getComplexType() {
        return OwnedBlockMap.class;
    }

    @Override
    public byte @NotNull [] toPrimitive(@NotNull OwnedBlockMap complex, @NotNull PersistentDataAdapterContext context) {
        return OwnedBlock.bytesFromList(complex);
    }

    @Override
    public @NotNull OwnedBlockMap fromPrimitive(byte @NotNull [] primitive, @NotNull PersistentDataAdapterContext context) {
        try {
            return OwnedBlock.listFromBytes(primitive);
        } catch (OwnedBlocksPDCException e) {
            MilkyPixelart.getInstance().getLogger().log(Level.WARNING, e.getMessage(), e);
            return new OwnedBlockMap();
        }
    }
}
