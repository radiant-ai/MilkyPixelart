package fun.milkyway.milkypixelart.pixelartmanager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.listeners.AuctionPreviewListener;
import fun.milkyway.milkypixelart.listeners.ProtectionListener;
import fun.milkyway.milkypixelart.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.network.protocol.game.PacketPlayOutMap;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PixelartManager {
    private final MilkyPixelart plugin;
    private final Random random;
    private final ExecutorService executor;
    private final ProtocolManager protocolManager;

    @SuppressWarnings("deprecation")
    private final NamespacedKey copyrightKey = new NamespacedKey("survivaltweaks", "copyright");

    public PixelartManager(MilkyPixelart plugin) {
        this.plugin = plugin;
        executor = Executors.newSingleThreadExecutor();
        random = new Random();
        protocolManager = ProtocolLibrary.getProtocolManager();
        plugin.getServer().getPluginManager().registerEvents(new ProtectionListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new AuctionPreviewListener(this), plugin);
    }

    public void protect(Player p, ItemStack map) {
        UUID uuid = p.getUniqueId();
        String name = p.getName();
        protect(uuid, name, map);
    }

    public boolean protect(UUID uuid, String name, ItemStack map) {
        if (map != null && map.getType() == Material.FILLED_MAP) {
            ItemMeta meta = map.getItemMeta();
            MapMeta mapMeta = (MapMeta) meta;
            PersistentDataContainer pdc = mapMeta.getPersistentDataContainer();
            try {
                pdc.set(copyrightKey, PersistentDataType.STRING, uuid.toString());
                if (name != null) {
                    List<String> lore = new LinkedList<String>();
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Copyrighted by &b"+name));
                    mapMeta.setLore(lore);
                }
                mapMeta.getMapView().setLocked(true);
                mapMeta.getMapView().setTrackingPosition(false);
                mapMeta.getMapView().setUnlimitedTracking(false);
                map.setItemMeta(mapMeta);
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public UUID getAuthor(ItemStack map) {
        if (map != null && map.getType() == Material.FILLED_MAP) {
            PersistentDataContainer pdc = map.getItemMeta().getPersistentDataContainer();
            try {
                if (pdc.has(copyrightKey, PersistentDataType.STRING)) {
                    UUID uuid = UUID.fromString(pdc.get(copyrightKey, PersistentDataType.STRING));
                    return uuid;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public ItemStack getUnprotectedCopy(ItemStack map) {
        ItemStack copy = map.clone();
        if (map.getType() == Material.FILLED_MAP) {
            ItemMeta meta = copy.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            try {
                List<String> lore = meta.getLore();
                if (lore != null) {
                    ListIterator<String> iter = lore.listIterator();
                    while(iter.hasNext()){
                        if(iter.next().contains("Copyrighted by")) {
                            iter.remove();
                        }
                    }
                }
                meta.setLore(lore);
                if (pdc.has(copyrightKey, PersistentDataType.STRING)) {
                    pdc.remove(copyrightKey);
                }
                copy.setItemMeta(meta);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return copy;
    }

    public void renderArtToUser(Player p, ItemStack is) {
        if (is != null && is.getType().equals(Material.FILLED_MAP)) {
            Location l = Utils.calculatePlayerFace(p);
            int id = createItemFrame(p, l);
            populateItemFrame(p, id, is);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                killItemFrame(p, id);
            }, 100);
        }
    }

    public int createItemFrame(Player p, Location l) {
        int direction = Utils.getDirection(l);
        int id = Integer.MAX_VALUE - random.nextInt() % 100000;

        PacketContainer pc = protocolManager .createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        //Entity
        pc.getEntityTypeModifier().write(0, EntityType.GLOW_ITEM_FRAME);
        pc.getUUIDs().write(0, UUID.randomUUID());
        pc.getIntegers().write(0, id);
        //Pos
        pc.getDoubles().write(0, (double) l.getBlockX());
        pc.getDoubles().write(1, (double) l.getBlockY());
        pc.getDoubles().write(2, (double) l.getBlockZ());
        //Velocity
        pc.getIntegers().write(1, 0);
        pc.getIntegers().write(2, 0);
        pc.getIntegers().write(3, 0);
        //Pitch and yaw
        pc.getIntegers().write(4, 0);
        pc.getIntegers().write(5, (int) (l.getYaw() * 256.0F / 360.0F));
        //Data
        pc.getIntegers().write(6, direction);

        try {
            protocolManager.sendServerPacket(p, pc);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return id;
    }

    public void populateItemFrame(Player player, int id, ItemStack is) {
        MapMeta mapMeta = (MapMeta) is.getItemMeta();
        if (mapMeta.hasMapView()) {
            MapView mapView = mapMeta.getMapView();

                CompletableFuture<byte[]> completableFuture = getMapBytes(mapView.getId());
                completableFuture.thenAccept(bytes -> {
                    try {
                        if (!mapMeta.getMapView().getRenderers().isEmpty()) {

                            PacketContainer pc = protocolManager .createPacket(PacketType.Play.Server.ENTITY_METADATA);
                            pc.getIntegers().write(0, id);
                            WrappedDataWatcher watcher = new WrappedDataWatcher();
                            watcher.setEntity(player);
                            watcher.setObject(8, WrappedDataWatcher.Registry.getItemStackSerializer(false), is);
                            pc.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

                            try {
                                protocolManager.sendServerPacket(player, pc);
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }

                            if (bytes != null) {
                                WorldMap.b worldMap = new WorldMap.b(0 ,0, 128, 128, bytes);
                                PacketPlayOutMap nmsPacket = new PacketPlayOutMap(mapView.getId(), (byte) 4, false, null, worldMap);
                                PacketContainer pc2 = PacketContainer.fromPacket(nmsPacket);
                                try {
                                    protocolManager.sendServerPacket(player, pc2);
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        }
    }

    public MilkyPixelart getPlugin() {
        return plugin;
    }

    public void killItemFrame(Player p, int id) {
        PacketContainer pc = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        pc.getIntLists().write(0, Arrays.asList(id));
        try {
            protocolManager.sendServerPacket(p, pc);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public String getMapsDirectoryPath() {
        return plugin.getDataFolder().getAbsoluteFile().getParentFile().getParentFile().toString()
                + File.separator+"world"
                +File.separator+"data";
    }


    public CompletableFuture<byte[]> getMapBytesFromPath(String path) {
        CompletableFuture<byte[]> bytes = new CompletableFuture<>();
        executor.submit(() -> {


            byte[] arr = new byte[16384];

            try {
                NamedTag namedRoot = NBTUtil.read(path);
                CompoundTag root = (CompoundTag) namedRoot.getTag();
                CompoundTag data = root.getCompoundTag("data");
                byte[] colors = data.getByteArray("colors");
                System.arraycopy(colors, 0, arr, 0, colors.length);
                bytes.complete(arr);
            } catch (IOException e) {
                bytes.complete(null);
            }
        });
        return bytes;
    }

    public CompletableFuture<byte[]> getMapBytes(int id) {
        String path = getMapsDirectoryPath()+File.separator+"map_"+id+".dat";
        return getMapBytesFromPath(path);
    }
}
