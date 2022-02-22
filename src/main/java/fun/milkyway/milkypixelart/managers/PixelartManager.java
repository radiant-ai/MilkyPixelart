package fun.milkyway.milkypixelart.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.listeners.AuctionPreviewListener;
import fun.milkyway.milkypixelart.listeners.IllegitimateArtListener;
import fun.milkyway.milkypixelart.listeners.PixelartProtectionListener;
import fun.milkyway.milkypixelart.utils.Utils;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class PixelartManager extends ArtManager {
    private static PixelartManager instance;

    private final Random random;
    private final ThreadPoolExecutor executorService;

    private Map<UUID, UUID> legacyToNewUUIDMap;

    private Map<Integer, UUID> blackList;

    public static final String BLACKLIST_FILENAME = "blacklist.yml";

    //SIGNLETON
    private PixelartManager() {
        super();

        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        executorService.setMaximumPoolSize(2);

        random = new Random();

        initializeFixMap(new File(plugin.getDataFolder(), "replacementData.txt").getPath());
        loadBlacklist();

        registerListener(new PixelartProtectionListener());
        registerListener(new AuctionPreviewListener());
        registerListener(new IllegitimateArtListener());
    }

    public synchronized static @NotNull PixelartManager getInstance() {
        if (instance == null) {
            instance = new PixelartManager();
        }
        return instance;
    }

    public synchronized static CompletableFuture<PixelartManager> reload() {
        CompletableFuture<PixelartManager> result = new CompletableFuture<>();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                getInstance().shutdown();
            }
            catch (Exception exception) {
                MilkyPixelart.getInstance().getLogger().log(Level.WARNING, exception.getMessage(), exception);
                result.completeExceptionally(exception);
            }
            instance = new PixelartManager();
            result.complete(getInstance());
        });
        return result;
    }

    @Override
    public boolean protect(@NotNull Player player, @NotNull ItemStack map) {
        if (ArtManager.isMap(map)) {
            CopyrightManager.getInstance().protect(player, map);
            return true;
        }
        return false;
    }

    @Override
    public boolean protect(@NotNull UUID uuid, @Nullable String name, @NotNull ItemStack map) {
        if (ArtManager.isMap(map)) {
            CopyrightManager.getInstance().protect(uuid, name, map);
            return true;
        }
        return false;
    }

    @Override
    public @Nullable CopyrightManager.Author getAuthor(ItemStack map) {
        return CopyrightManager.getInstance().getAuthor(map);
    }

    @Override
    public @NotNull ItemStack getUnprotectedCopy(ItemStack map) {
        return CopyrightManager.getInstance().getUnprotectedCopy(map);
    }

    @Override
    public int getProtectionCost() {
        return MilkyPixelart.getInstance().getConfiguration().getInt("pixelarts.copyrightPrice");
    }

    public void renderArtToUser(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (itemStack.getType().equals(Material.FILLED_MAP)) {
            Location l = Utils.calculatePlayerFace(player);
            int id = createItemFrame(player, l);
            populateItemFrame(player, id, itemStack);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> killItemFrame(player, id), 100);
        }
    }

    public int createItemFrame(@NotNull Player p, @NotNull Location l) {
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

    public void populateItemFrame(@NotNull Player player, int id, @NotNull ItemStack is) {
        MapMeta mapMeta = (MapMeta) is.getItemMeta();
        if (mapMeta.hasMapView()) {
            MapView mapView = mapMeta.getMapView();
            if (mapView == null) {
                return;
            }
            CompletableFuture.supplyAsync(() -> getMapBytes(mapView.getId()), executorService).thenAccept(bytes -> {
                try {
                    if (!mapView.getRenderers().isEmpty()) {

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

    public void killItemFrame(@NotNull Player p, int id) {
        PacketContainer pc = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        pc.getIntLists().write(0, List.of(id));
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


    private byte[] getMapBytesFromFile(@NotNull File file) {
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

    public CompletableFuture<List<String>> getDuplicates(@NotNull CommandSender commandSender, int mapId) {

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        CompletableFuture<List<String>> finalResult = new CompletableFuture<>();

        threadPoolExecutor.submit(() -> {

            byte[] originalBytes = getMapBytes(mapId);

            if (originalBytes != null) {

                File dataDirectory = getMapsDirectory();
                File[] files = dataDirectory.listFiles();

                if (files != null) {
                    commandSender.sendMessage(ChatColor.GREEN+"Найдено "+files.length+" файлов карт, начинаем поиск...");

                    AtomicInteger count = new AtomicInteger(0);
                    List<String> resultList = Collections.synchronizedList(new ArrayList<>());
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
                                Player freshPlayer = plugin.getServer().getPlayer(player.getUniqueId());
                                if ( freshPlayer != null &&
                                        freshPlayer.isOnline() &&
                                        countUnboxed % 2500 == 0) {
                                    commandSender.sendMessage(ChatColor.GRAY+"Просмотрено "+ChatColor.WHITE+count+ChatColor.GRAY+" файлов карт!");
                                }
                            }
                        }, threadPoolExecutor);
                    }

                    CompletableFuture.allOf(tasks).thenRun(() -> finalResult.complete(resultList));
                }

            }
            else {
                commandSender.sendMessage(ChatColor.RED+"Не удалось найти указанную карту!");
            }
        });

        return finalResult;
    }

    private void initializeFixMap(@NotNull String migrationFilePath) {
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

    public @Nullable UUID fromLegacyUUID(@NotNull UUID uuid) {
        return legacyToNewUUIDMap.get(uuid);
    }

    private void saveBlacklist() {
        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), BLACKLIST_FILENAME));
        fileConfiguration.set("blacklist", null);

        int count = 0;
        for (Map.Entry<Integer, UUID> entry : blackList.entrySet()) {
            fileConfiguration.set("blacklist."+entry.getKey(), entry.getValue().toString());
            count++;
        }
        plugin.getLogger().info(ChatColor.GREEN+"Saved "+count+" blacklist entries into the savefile!");

        try {
            fileConfiguration.save(new File(plugin.getDataFolder(), BLACKLIST_FILENAME));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }

    private void loadBlacklist() {
        blackList = new ConcurrentHashMap<>();
        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), BLACKLIST_FILENAME));

        ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection("blacklist");
        int count = 0;
        if (configurationSection != null) {
            for (String key : configurationSection.getKeys(false)) {
                String uuidString = fileConfiguration.getString("blacklist."+key);
                if (uuidString != null) {
                    blackList.put(Integer.parseInt(key), UUID.fromString(uuidString));
                    count++;
                }
            }
        }
        plugin.getLogger().info(ChatColor.GREEN+"Loaded "+count+" blacklist entries from the savefile!");
    }

    public void blacklistAdd(int mapId, @NotNull UUID ownerUUID) {
        blackList.put(mapId, ownerUUID);
        saveBlacklistAsync().thenAccept(exception -> {
            if (exception != null)
                plugin.getLogger().log(Level.WARNING, exception.getMessage(), exception);
        });
    }

    public @Nullable  UUID blacklistRemove(int mapId) {
        UUID uuid = blackList.remove(mapId);
        if (uuid != null)
            saveBlacklistAsync().thenAccept(exception -> {
                if (exception != null)
                    plugin.getLogger().log(Level.WARNING, exception.getMessage(), exception);
            });
        return uuid;
    }

    private CompletableFuture<IOException> saveBlacklistAsync() {
        plugin.getLogger().info(ChatColor.YELLOW+"Async save blacklist...");
        CompletableFuture<IOException> completableFuture = new CompletableFuture<>();
        executorService.submit(() -> {
            saveBlacklist();
            completableFuture.complete(null);
        });
        return completableFuture;
    }

    public @NotNull ArrayList<Map.Entry<Integer, UUID>> blacklistList() {
        return new ArrayList<>(blackList.entrySet().stream().toList());
    }

    public boolean isLegitimateOwner(@NotNull ItemStack map) {
        CopyrightManager.Author author = CopyrightManager.getInstance().getAuthor(map);
        MapView mapView = ((MapMeta) map.getItemMeta()).getMapView();

        if (mapView == null) {
            return true;
        }

        return isLegitimateOwner(mapView.getId(), author);
    }

    public boolean isLegitimateOwner(int mapId, @Nullable CopyrightManager.Author author) {
        UUID realOwner = blackList.get(mapId);
        if (realOwner != null) {

            //no owner but in blacklist
            if (author == null) {
                return false;
            }

            //this owner is legitimate
            if (realOwner.equals(author.getUuid())) {
                return true;
            }

            //owner used to be legitimate in past
            UUID realOwnerFromLegacy = fromLegacyUUID(author.getUuid());
            return realOwnerFromLegacy != null && realOwnerFromLegacy.equals(realOwner);
        }

        //not in the blacklist at all
        return true;
    }

    public int getMapId(@NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() instanceof MapMeta mapMeta) {
            if (mapMeta.getMapView() != null) {
                return mapMeta.getMapView().getId();
            }
        }
        return -1;
    }

    @Override
    public void shutdown() throws Exception {
        saveBlacklist();
        executorService.shutdown();
        unregisterListeners();
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            plugin.getLogger().warning("Force terminated executor service after timeout!");
        }
    }
}
