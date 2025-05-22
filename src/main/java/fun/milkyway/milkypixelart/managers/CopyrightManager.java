package fun.milkyway.milkypixelart.managers;

import fun.milkyway.milkypixelart.utils.OrientationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

public class CopyrightManager {

    public record Author(UUID uuid, String name) {

        public @NotNull
        UUID getUuid() {
            return uuid;
        }

        public @Nullable
        String getName() {
            return name;
        }
    }

    //we have to use old namespace to support legacy copyrighted items
    protected final NamespacedKey copyrightKey = new NamespacedKey("survivaltweaks", "copyright");

    protected final NamespacedKey copyrightNameKey = new NamespacedKey("survivaltweaks", "copyrightname");

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

    public synchronized static void reload() {
        instance = new CopyrightManager();
    }

    protected void protect(@NotNull Player p, @NotNull ItemStack itemStack) {
        UUID uuid = p.getUniqueId();
        String name = p.getName();
        protect(uuid, name, itemStack);
    }

    protected void protect(@NotNull UUID uuid, @Nullable String name, @NotNull ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();

        protect(uuid, name, itemMeta);

        applyCopyrightLore(itemMeta, name);

        if (itemMeta instanceof MapMeta mapMeta) {
            freezeMap(mapMeta);
        } else if (itemMeta instanceof BannerMeta bannerMeta) {
            hidePatterns(bannerMeta);
        }

        itemStack.setItemMeta(itemMeta);
    }

    protected void protect(@NotNull UUID uuid, @Nullable String name, @NotNull PersistentDataHolder persistentDataHolder) {
        applyPDCCopyright(persistentDataHolder, uuid, name);
    }

    protected @Nullable Author getAuthor(@NotNull ItemStack itemStack) {
        return getAuthor(itemStack.getItemMeta());
    }

    protected @Nullable Author getAuthor(@NotNull PersistentDataHolder persistentDataHolder) {
        PersistentDataContainer pdc = persistentDataHolder.getPersistentDataContainer();
        if (pdc.has(copyrightKey, PersistentDataType.STRING)) {
            String stringUUID = pdc.get(copyrightKey, PersistentDataType.STRING);
            if (stringUUID != null) {
                String name = null;
                if (pdc.has(copyrightNameKey, PersistentDataType.STRING)) {
                    name = pdc.get(copyrightNameKey, PersistentDataType.STRING);
                }
                return new Author(UUID.fromString(stringUUID), name);
            }
        }
        return null;
    }

    protected @NotNull ItemStack getUnprotectedCopy(@NotNull ItemStack itemStack) {
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

    private void applyCopyrightLore(@NotNull ItemMeta itemMeta, @Nullable String name) {
        removeCopyrightLore(itemMeta);

        List<Component> lore;
        if (itemMeta.hasLore()) {
            lore = itemMeta.lore();
        }
        else {
            lore = new ArrayList<>();
        }

        if (lore != null) {
            Component line = LangManager.getInstance().getLang("copyright.title");
            if (!line.hasDecoration(TextDecoration.ITALIC)) { //we need to allow explicit italics, but still remove the default one
                line = line.decoration(TextDecoration.ITALIC, false);
            }
            lore.add(line);
            line = LangManager.getInstance().getLang("copyright.author", name == null ? "" : name);
            if (!line.hasDecoration(TextDecoration.ITALIC)) {
                line = line.decoration(TextDecoration.ITALIC, false);
            }
            lore.add(line);
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
                if(LangManager.getInstance().getLangPlainList("copyright.legacy").stream().anyMatch(line::contains)
                        || line.contains(LangManager.getInstance().getLangPlain("copyright.title"))
                        || line.contains(LangManager.getInstance().getLangPlain("copyright.author", ""))) {
                    iterator.remove();
                }
            }
            if (lore.isEmpty()) {
                lore = null;
            }
        }

        itemMeta.lore(lore);
    }

    private void applyPDCCopyright(@NotNull PersistentDataHolder persistentDataHolder, @NotNull UUID uuid, @Nullable String name) {
        PersistentDataContainer pdc = persistentDataHolder.getPersistentDataContainer();
        pdc.set(copyrightKey, PersistentDataType.STRING, uuid.toString());

        name = name == null ? Bukkit.getOfflinePlayer(uuid).getName() : name;

        if (name != null) {
            pdc.set(copyrightNameKey, PersistentDataType.STRING, name);
        }
    }

    private void removePDCCopyright(@NotNull PersistentDataHolder persistentDataHolder) {
        PersistentDataContainer pdc = persistentDataHolder.getPersistentDataContainer();
        if (pdc.has(copyrightKey, PersistentDataType.STRING)) {
            pdc.remove(copyrightKey);
        }
        if (pdc.has(copyrightNameKey, PersistentDataType.STRING)) {
            pdc.remove(copyrightNameKey);
        }
    }

    private void freezeMap(@NotNull MapMeta mapMeta) {
        if (mapMeta.hasMapView() && mapMeta.getMapView() != null) {
            mapMeta.getMapView().setLocked(true);
            mapMeta.getMapView().setTrackingPosition(false);
            mapMeta.getMapView().setUnlimitedTracking(false);
        }
    }

    public void hidePatterns(@NotNull ItemMeta itemMeta) {
        if (!itemMeta.hasItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }
    }

    private void showPatterns(@NotNull BannerMeta bannerMeta) {
        if (bannerMeta.hasItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)) {
            bannerMeta.removeItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }
    }
}
