package fun.milkyway.milkypixelart.pixelartmanager;

import co.aikar.commands.InvalidCommandArgument;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.listeners.AuctionPreviewListener;
import fun.milkyway.milkypixelart.listeners.IllegitimateArtListener;
import fun.milkyway.milkypixelart.listeners.ProtectionListener;
import fun.milkyway.milkypixelart.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.protocol.game.PacketPlayOutMap;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class PixelartManager {
    private final MilkyPixelart plugin;
    private final Random random;
    private final ExecutorService mapFileParserExecutor;
    private final ProtocolManager protocolManager;

    private Map<UUID, UUID> legacyToNewUUIDMap;

    private Map<Integer, UUID> blackList;

    private List<Listener> listeners;

    @SuppressWarnings("deprecation")
    private final NamespacedKey copyrightKey = new NamespacedKey("survivaltweaks", "copyright");

    public final String COPYRIGHT_STRING_LEGACY = "Copyrighted by";
    public final String COPYRIGHT_STRING = "Защищено от копирования";
    public final String PREFIX = "© ";

    public final String BLACKLIST_FILENAME = "blacklist.yml";

    public PixelartManager(MilkyPixelart plugin) {
        mapFileParserExecutor = Executors.newSingleThreadExecutor();

        this.plugin = plugin;

        random = new Random();
        protocolManager = ProtocolLibrary.getProtocolManager();

        initializeFixMap(new File(plugin.getDataFolder(), "replacementData.txt").getPath());
        loadBlacklist();

        registerListeners();
    }

    private void registerListeners() {
        listeners = new LinkedList<>();

        Listener protectionListener = new ProtectionListener(this);
        Listener auctionPreviewListener = new AuctionPreviewListener(this);
        Listener illegitimateArtListener = new IllegitimateArtListener(this);

        plugin.getServer().getPluginManager().registerEvents(protectionListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(auctionPreviewListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(illegitimateArtListener, plugin);

        listeners.add(protectionListener);
        listeners.add(auctionPreviewListener);
        listeners.add(illegitimateArtListener);
    }

    private void unregisterListeners() {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
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
                    List<Component> lore;
                    if (mapMeta.hasLore()) {
                        lore = mapMeta.lore();
                    }
                    else {
                        lore = new ArrayList<>();
                    }
                    lore.add(Component.text()
                            .append(Component.text(COPYRIGHT_STRING).color(TextColor.fromHexString("#FFFF99")).decoration(TextDecoration.ITALIC, false))
                            .build());
                    lore.add(Component.text()
                            .append(Component.text(PREFIX).color(TextColor.fromHexString("#FFFF99")).decoration(TextDecoration.ITALIC, false))
                            .append(Component.text(name).color(TextColor.fromHexString("#9AFF0F")).decoration(TextDecoration.ITALIC, false))
                            .build());
                    mapMeta.lore(lore);
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
                List<Component> lore;
                if (meta.hasLore()) {
                    lore = meta.lore();
                }
                else {
                    lore = new ArrayList<>();
                }
                ListIterator<Component> iter = lore.listIterator();
                while(iter.hasNext()){
                    String line = PlainTextComponentSerializer.plainText().serialize(iter.next());
                    if(line.contains(COPYRIGHT_STRING_LEGACY) || line.contains(COPYRIGHT_STRING)
                            || line.contains(PREFIX)) {
                        iter.remove();
                    }
                }
                meta.lore(lore);
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

            CompletableFuture.supplyAsync(() -> getMapBytes(mapView.getId()), mapFileParserExecutor).thenAccept(bytes -> {
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

    public File getMapsDirectory() {
        File mapDataDirectory = plugin.getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
        mapDataDirectory = new File(mapDataDirectory, "world");
        mapDataDirectory = new File(mapDataDirectory, "data");
        return mapDataDirectory;
    }


    private byte[] getMapBytesFromFile(File file) {
        if (!file.exists() || !file.canRead() || !file.isFile()) {
            return null;
        }

        byte[] arr = new byte[16384];
        try {
            NamedTag namedRoot = NBTUtil.read(file);
            CompoundTag root = (CompoundTag) namedRoot.getTag();
            CompoundTag data = root.getCompoundTag("data");
            byte[] colors = data.getByteArray("colors");
            System.arraycopy(colors, 0, arr, 0, colors.length);
            return arr;
        }
        catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, e.getMessage(), e);
            return null;
        }
    }

    private byte[] getMapBytes(int id) {
        File mapFile = new File(getMapsDirectory(), "map_"+id+".dat");
        return getMapBytesFromFile(mapFile);
    }

    public CompletableFuture<List<String>> getDuplicates(CommandSender commandSender, int mapId) {

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        CompletableFuture<List<String>> finalResult = new CompletableFuture<>();

        threadPoolExecutor.submit(() -> {

            byte[] originalBytes = getMapBytes(mapId);

            if (originalBytes != null) {

                File dataDirectory = getMapsDirectory();
                File[] files = dataDirectory.listFiles();

                commandSender.sendMessage(ChatColor.GREEN+"Найдено "+files.length+" файлов карт, начинаем поиск...");

                AtomicInteger count = new AtomicInteger(0);
                List<String> resultList = Collections.synchronizedList(new ArrayList());
                CompletableFuture[] tasks = new CompletableFuture[files.length];

                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    tasks[i] = CompletableFuture.runAsync(() -> {
                        if (!file.getName().startsWith("map")) {
                            return;
                        }
                        byte[] otherBytes = getMapBytesFromFile(file);
                        int countUnboxed = count.getAndIncrement();
                        if (otherBytes != null && Arrays.equals(originalBytes, otherBytes))
                            resultList.add(file.getName());
                        if (commandSender instanceof Player player) {
                            if (plugin.getServer().getPlayer(player.getUniqueId()) != null &&
                                    plugin.getServer().getPlayer(player.getUniqueId()).isOnline()) {
                                if (countUnboxed % 2500 == 0) {
                                    commandSender.sendMessage(ChatColor.GRAY+"Просмотрено "+ChatColor.WHITE+count+ChatColor.GRAY+" файлов карт!");
                                }
                            }
                        }
                    }, threadPoolExecutor);
                }

                CompletableFuture.allOf(tasks).thenRun(() -> {
                    finalResult.complete(resultList);
                });

            }
            else {
                commandSender.sendMessage(ChatColor.RED+"Не удалось найти указанную карту!");
            }
        });

        return finalResult;
    }

    private void initializeFixMap(String migrationFilePath) {
        legacyToNewUUIDMap = new HashMap<>();
        File entriesFile = new File(migrationFilePath);
        if (entriesFile.exists()) {
            try {
                Scanner scanner = new Scanner(entriesFile);
                while(scanner.hasNext()) {
                    String line = scanner.nextLine();
                    String[] tokens = line.split(":");
                    UUID uuidFrom = UUID.fromString(tokens[0]);
                    UUID uuidTo = UUID.fromString(tokens[1]);
                    legacyToNewUUIDMap.put(uuidFrom, uuidTo);
                }
            }
            catch (IllegalArgumentException | ArrayIndexOutOfBoundsException | FileNotFoundException exception) {
                plugin.getLogger().log(Level.WARNING, exception.getMessage(), exception);
            }
        }
    }

    public UUID fromLegacyUUID(UUID uuid) {
        return legacyToNewUUIDMap.get(uuid);
    }

    private void saveBlacklist() throws IOException {
        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), BLACKLIST_FILENAME));
        fileConfiguration.set("blacklist", null);
        if (fileConfiguration != null) {
            int count = 0;
            for (Map.Entry<Integer, UUID> entry : blackList.entrySet()) {
                fileConfiguration.set("blacklist."+entry.getKey(), entry.getValue().toString());
                count++;
            }
            plugin.getLogger().info(ChatColor.GREEN+"Saved "+count+" blacklist entries into the savefile!");
        }
        fileConfiguration.save(new File(plugin.getDataFolder(), BLACKLIST_FILENAME));
    }

    private void loadBlacklist() {
        blackList = new HashMap<>();
        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), BLACKLIST_FILENAME));
        if (fileConfiguration != null) {
            ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection("blacklist");
            if (configurationSection != null) {
                int count = 0;
                for (String key : configurationSection.getKeys(false)) {
                    blackList.put(Integer.parseInt(key), UUID.fromString(fileConfiguration.getString("blacklist."+key)));
                    count++;
                }
                plugin.getLogger().info(ChatColor.GREEN+"Loaded "+count+" blacklist entries from the savefile!");
            }
        }
    }

    public void blacklistAdd(int mapId, UUID ownerUUID) {
        blackList.put(mapId, ownerUUID);
    }

    public UUID blacklistRemove(int mapId) {
        return blackList.remove(mapId);
    }

    public ArrayList<Map.Entry<Integer, UUID>> blacklistList() {
        return new ArrayList(blackList.entrySet().stream().toList());
    }

    public boolean isLegitimateOwner(ItemStack map) {
        UUID uuid = getAuthor(map);
        MapView mapView = ((MapMeta) map.getItemMeta()).getMapView();

        if (mapView == null) {
            return true;
        }

        return isLegitimateOwner(mapView.getId(), uuid);
    }

    public boolean isLegitimateOwner(int mapId, UUID testUUID) {
        UUID realOwner = blackList.get(mapId);
        if (realOwner != null) {

            //no owner but in blacklist
            if (testUUID == null) {
                return false;
            }

            //this owner is legitimate
            if (realOwner.equals(testUUID)) {
                return true;
            }

            //owner used to be legitimate in past
            UUID realOwnerFromLegacy = fromLegacyUUID(testUUID);
            if (realOwnerFromLegacy != null && realOwnerFromLegacy.equals(realOwner)) {
                return true;
            }

            //not legitimate
            return false;
        }

        //not in the blacklist at all
        return true;
    }

    public void shutdown() throws IOException, InterruptedException {
        mapFileParserExecutor.shutdown();
        unregisterListeners();
        saveBlacklist();
        mapFileParserExecutor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
