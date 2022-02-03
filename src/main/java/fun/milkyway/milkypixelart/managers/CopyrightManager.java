package fun.milkyway.milkypixelart.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

public class CopyrightManager {
    @SuppressWarnings("deprecation")
    protected final NamespacedKey copyrightKey = new NamespacedKey("survivaltweaks", "copyright");

    protected static final String COPYRIGHT_STRING_LEGACY = "Copyrighted by";
    protected static final String COPYRIGHT_STRING = "Защищено от копирования";
    protected static final String PREFIX = "© ";

    private static CopyrightManager instance;

    //SINGLETON
    private CopyrightManager() {

    }

    protected synchronized static @NotNull CopyrightManager getInstance() {
        if (instance == null) {
            instance = new CopyrightManager();
        }
        return instance;
    }

    protected boolean protect(@NotNull Player p, @NotNull ItemStack itemStack) {
        UUID uuid = p.getUniqueId();
        String name = p.getName();
        return protect(uuid, name, itemStack);
    }

    protected boolean protect(@NotNull UUID uuid, @Nullable String name, @NotNull ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();

        applyPDCCopyright(itemMeta, uuid);

        if (name != null) {
            applyCopyrightLore(itemMeta, name);
        }

        if (itemMeta instanceof MapMeta mapMeta) {
            freezeMap(mapMeta);
        }
        else if (itemMeta instanceof BannerMeta bannerMeta) {
            hidePatterns(bannerMeta);
        }

        itemStack.setItemMeta(itemMeta);

        return true;
    }

    private void applyCopyrightLore(@NotNull ItemMeta itemMeta, @NotNull String name) {
        List<Component> lore;
        if (itemMeta.hasLore()) {
            lore = itemMeta.lore();
        }
        else {
            lore = new ArrayList<>();
        }

        if (lore != null) {
            lore.add(Component.text()
                    .append(Component.text(COPYRIGHT_STRING).color(TextColor.fromHexString("#FFFF99")).decoration(TextDecoration.ITALIC, false))
                    .build());
            lore.add(Component.text()
                    .append(Component.text(PREFIX).color(TextColor.fromHexString("#FFFF99")).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(name).color(TextColor.fromHexString("#9AFF0F")).decoration(TextDecoration.ITALIC, false))
                    .build());
        }

        itemMeta.lore(lore);
    }

    private void removeCopyrightLore(@NotNull ItemMeta itemMeta) {
        List<Component> lore;

        if (itemMeta.hasLore()) {
            lore = itemMeta.lore();
        }
        else {
            lore = new ArrayList<>();
        }

        if (lore != null) {
            ListIterator<Component> iterator = lore.listIterator();
            while(iterator.hasNext()){
                String line = PlainTextComponentSerializer.plainText().serialize(iterator.next());
                if(line.contains(COPYRIGHT_STRING_LEGACY) || line.contains(COPYRIGHT_STRING)
                        || line.contains(PREFIX)) {
                    iterator.remove();
                }
            }
        }

        itemMeta.lore(lore);
    }

    private void applyPDCCopyright(@NotNull ItemMeta itemMeta, @NotNull UUID uuid) {
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        pdc.set(copyrightKey, PersistentDataType.STRING, uuid.toString());
    }

    private void removePDCCopyright(@NotNull ItemMeta itemMeta) {
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        if (pdc.has(copyrightKey, PersistentDataType.STRING)) {
            pdc.remove(copyrightKey);
        }
    }

    private void freezeMap(@NotNull MapMeta mapMeta) {
        if (mapMeta.hasMapView() && mapMeta.getMapView() != null) {
            mapMeta.getMapView().setLocked(true);
            mapMeta.getMapView().setTrackingPosition(false);
            mapMeta.getMapView().setUnlimitedTracking(false);
        }
    }

    private void hidePatterns(@NotNull BannerMeta bannerMeta) {
        if (!bannerMeta.hasItemFlag(ItemFlag.HIDE_POTION_EFFECTS)) {
            bannerMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        }
    }

    private void showPatterns(@NotNull BannerMeta bannerMeta) {
        if (bannerMeta.hasItemFlag(ItemFlag.HIDE_POTION_EFFECTS)) {
            bannerMeta.removeItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        }
    }

    protected @Nullable UUID getAuthor(@NotNull ItemStack itemStack) {
        PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
        if (pdc.has(copyrightKey, PersistentDataType.STRING)) {
            String stringUUID = pdc.get(copyrightKey, PersistentDataType.STRING);
            if (stringUUID != null) {
                return UUID.fromString(stringUUID);
            }
        }
        return null;
    }

    protected @NotNull  ItemStack getUnprotectedCopy(@NotNull ItemStack itemStack) {
        ItemStack copy = itemStack.clone();
        ItemMeta itemMeta = copy.getItemMeta();

        removePDCCopyright(itemMeta);
        removeCopyrightLore(itemMeta);

        if (itemMeta instanceof BannerMeta bannerMeta) {
            showPatterns(bannerMeta);
        }
        copy.setItemMeta(itemMeta);

        return copy;
    }
}
