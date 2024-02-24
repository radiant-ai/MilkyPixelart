package fun.milkyway.milkypixelart.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.listeners.AuctionPreviewListener;
import fun.milkyway.milkypixelart.listeners.IllegitimateArtListener;
import fun.milkyway.milkypixelart.listeners.MapCreateListener;
import fun.milkyway.milkypixelart.listeners.PixelartProtectionListener;
import fun.milkyway.milkypixelart.utils.ActiveFrame;
import fun.milkyway.milkypixelart.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class PixelartManager extends ArtManager {
    private static PixelartManager instance;

    private final Random random;
    private final ThreadPoolExecutor executorService;

    private Map<Integer, UUID> blackList;

    public static final String BLACKLIST_FILENAME = "blacklist.yml";

    private final Map<Integer, ItemStack> previewMapKeys;
    private final Map<UUID, Long> lastShowMap;

    private final Map<UUID, ActiveFrame> activeFrames;

    //SIGNLETON
    private PixelartManager() {
        super();

        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        executorService.setMaximumPoolSize(2);

        random = new Random();
        previewMapKeys = new HashMap<>();
        lastShowMap = new HashMap<>();
        activeFrames = new HashMap<>();

        loadBlacklist();

        registerListener(new PixelartProtectionListener());
        registerListener(new AuctionPreviewListener());
        registerListener(new IllegitimateArtListener());
        registerListener(new MapCreateListener());
    }

    public static @NotNull PixelartManager getInstance() {
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

    public void showMaps(@NotNull Player player, boolean all) {
        long cooldown = getShowCooldown(player.getUniqueId());
        if (cooldown > 0 && !player.hasPermission("pixelart.show.cooldownbypass")) {
            player.sendMessage(LangManager.getInstance().getLang("show.fail_cooldown", ""+cooldown));
            return;
        }
        Component component;
        if (all) {
            component = buildAllMapsComponent(player);
        }
        else {
            component = buildSingleMapComponent(player);
        }
        if (component == null) {
            if (all) {
                player.sendMessage(LangManager.getInstance().getLang("show.fail_no_in_toolbar"));
            }
            else {
                player.sendMessage(LangManager.getInstance().getLang("show.fail_no_map"));
            }
            return;
        }
        Component message = LangManager.getInstance().getLang(
                "show.message", player.getName(), MiniMessage.miniMessage().serialize(component));
        MilkyPixelart.getInstance().getServer().broadcast(message, "pixelart.preview");
        putOnCooldown(player.getUniqueId());
    }

    private Component buildSingleMapComponent(@NotNull Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        return createPreviewComponent(itemStack, player.getName());
    }

    private Component buildAllMapsComponent(@NotNull Player player) {
        TextComponent.Builder builder = Component.text();
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (itemStack != null) {
                Component component = createPreviewComponent(itemStack, player.getName());
                if (component != null) {
                    count++;
                    builder.append(component);
                    if (i != 8) {
                        builder.append(Component.text(" "));
                    }
                }
            }
        }
        if (count == 0) {
            return null;
        }
        return builder.build();
    }

    public void putOnCooldown(@NotNull UUID uuid) {
        lastShowMap.put(uuid, System.currentTimeMillis());
    }

    public long getShowCooldown(@NotNull UUID uuid) {
        if (!lastShowMap.containsKey(uuid)) {
            return 0;
        }
        int cooldown = MilkyPixelart.getInstance().getConfiguration().getInt("pixelarts.showArtCooldown");
        if (lastShowMap.get(uuid) + cooldown * 1000L < System.currentTimeMillis()) {
            lastShowMap.remove(uuid);
            return 0;
        }
        return (lastShowMap.get(uuid) - System.currentTimeMillis()) / 1000L + cooldown;
    }

    private @Nullable Component createPreviewComponent(@NotNull ItemStack itemStack, @NotNull String name) {
        if (!itemStack.getType().equals(Material.FILLED_MAP)) {
            return null;
        }
        MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
        if (!mapMeta.hasMapView() || mapMeta.getMapView() == null) {
            return null;
        }
        previewMapKeys.put(mapMeta.getMapView().getId(), itemStack.clone());

        TextComponent.Builder builder = Component.text();
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName() && itemMeta.displayName() != null) {
            var displayName = itemMeta.displayName();
            if (displayName != null) {
                builder.append(LangManager.getInstance().getLang("show.map_component",
                        mapMeta.getMapView().getId()+"",
                        PlainTextComponentSerializer.plainText().serialize(displayName)));
            }
        }
        else {
            builder.append(LangManager.getInstance().getLang("show.map_component",
                    mapMeta.getMapView().getId()+"",
                    LangManager.getInstance().getLangPlain("show.map_default_name")));
        }
        builder.hoverEvent(HoverEvent.showText(LangManager.getInstance().getLang("show.click_to_preview", name)));
        return builder.build();
    }

    public void renderArt(@NotNull Player player, int mapId) {
        if (previewMapKeys.containsKey(mapId)) {
            renderArtFromItemStack(player, previewMapKeys.get(mapId));
        }
    }

    public void renderArt(@NotNull Player player, @NotNull ItemStack stack) {
        renderArtFromItemStack(player, stack);
    }

    private void renderArtFromItemStack(@NotNull Player player, @NotNull ItemStack stack) {
        UUID playerUUID = player.getUniqueId();
        renderArtToUser(player, stack).thenAccept(result -> {
            Bukkit.getScheduler().runTask(MilkyPixelart.getInstance(), () -> {
                Player player1 = Bukkit.getPlayer(playerUUID);

                if (player1 == null || !player1.isOnline()) {
                    return;
                }

                if (result) {
                    if (player1.getVehicle() != null) {
                        player1.leaveVehicle();
                    }
                    Location l = player1.getLocation();
                    l.setPitch(0);
                    l.setYaw(Utils.alignYaw(l));
                    player1.teleport(l);
                    player1.sendActionBar(LangManager.getInstance().getLang("preview.success"));
                }
                else {
                    player1.sendActionBar(LangManager.getInstance().getLang("preview.fail"));
                }
            });
        });
    }

    private CompletableFuture<Boolean> renderArtToUser(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (!itemStack.getType().equals(Material.FILLED_MAP)) {
            return CompletableFuture.completedFuture(false);
        }

        Location l = Utils.calculatePlayerFace(player);

        int id = createItemFrame(player, l);
        var task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> killFrame(player),
                MilkyPixelart.getInstance().getConfiguration().getInt("pixelarts.previewDuration", 100));

        saveFrame(player, id, task);

        if (id == 0) {
            return CompletableFuture.completedFuture(false);
        }

        return populateItemFrame(player, id, itemStack);
    }

    private int createItemFrame(@NotNull Player player, @NotNull Location l) {
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

        switch (getVersionLevel()) {
            case v1_18 -> {
                pc.getIntegers().write(6, direction);
            }
            case v1_20 -> {
                pc.getIntegers().write(4, direction);
            }
        }

        protocolManager.sendServerPacket(player, pc);

        return id;
    }

    private CompletableFuture<Boolean> populateItemFrame(@NotNull Player player, int id, @NotNull ItemStack mapItemStack) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        MapMeta mapMeta = (MapMeta) mapItemStack.getItemMeta();
        if (mapMeta.hasMapView()) {
            MapView mapView = mapMeta.getMapView();

            if (mapView == null) {
                return CompletableFuture.completedFuture(false);
            }

            var bytes = getMapBytesLive(mapView.getId());

            sendMapPacket(player, id, mapItemStack);

            if (bytes != null) {
                sendMapPacket(player, mapView.getId(), bytes);
            }
        }

        return result;
    }

    private void sendMapPacket(@NotNull Player player, int itemFrameId, @NotNull ItemStack item) {
        try {
            PacketContainer pc = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            pc.getIntegers().write(0, itemFrameId);

            switch (getVersionLevel()) {
                case v1_18 -> {
                    WrappedDataWatcher watcher = new WrappedDataWatcher();
                    watcher.setEntity(player);
                    watcher.setObject(8, WrappedDataWatcher.Registry.getItemStackSerializer(false), item);
                    pc.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
                }
                case v1_20 -> {
                    Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit."+getNMSVersion()+".inventory.CraftItemStack");
                    Method asNMSCopyMethod = craftItemStackClass.getDeclaredMethod("asNMSCopy", ItemStack.class);
                    Object nmsItemStack = asNMSCopyMethod.invoke(null, item);
                    var values = List.of(
                            new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x20),
                            new WrappedDataValue(8, WrappedDataWatcher.Registry.getItemStackSerializer(false), nmsItemStack)
                    );
                    pc.getDataValueCollectionModifier().write(0, values);
                }
            }

            protocolManager.sendServerPacket(player, pc);
        } catch (Exception e) {
            MilkyPixelart.getInstance().getLogger().log(Level.WARNING, "Error sending map packet", e);
        }
    }

    private void sendMapPacket(@NotNull Player player, int mapId,  byte @NotNull [] bytes) {
        try {
            Class<?> worldMapBClass = Class.forName("net.minecraft.world.level.saveddata.maps.WorldMap$b");
            Class<?> packetPlayOutMapClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutMap");

            Constructor<?> worldMapBConstructor = worldMapBClass.getDeclaredConstructor(int.class, int.class, int.class, int.class, byte[].class);
            Constructor<?> packetPlayOutMapConstructor = packetPlayOutMapClass.getDeclaredConstructor(int.class, byte.class, boolean.class, Collection.class, worldMapBClass);

            Object worldMapBInstance = worldMapBConstructor.newInstance(0, 0, 128, 128, bytes);
            Object packetPlayOutMapInstance = packetPlayOutMapConstructor.newInstance(mapId, (byte) 4, false, null, worldMapBInstance);
            PacketContainer pc = PacketContainer.fromPacket(packetPlayOutMapInstance);

            protocolManager.sendServerPacket(player, pc);
        } catch (Exception e) {
            MilkyPixelart.getInstance().getLogger().log(Level.WARNING, "Error sending map packet", e);
        }
    }

    private void saveFrame(@NotNull Player player, int frameId, BukkitTask bukkitTask) {
        killFrame(player);
        activeFrames.put(player.getUniqueId(), new ActiveFrame(frameId, bukkitTask));
    }

    private void killFrame(@NotNull Player player) {
        var activeFrame = activeFrames.get(player.getUniqueId());
        if (activeFrame == null) {
            return;
        }
        killItemFrame(player, activeFrame.getFrameId());
        activeFrame.getTask().cancel();
        activeFrames.remove(player.getUniqueId());
    }

    private void killItemFrame(@NotNull Player p, int id) {
        PacketContainer pc = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        pc.getIntLists().write(0, List.of(id));

        protocolManager.sendServerPacket(p, pc);
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

    private byte[] getMapBytesOffline(int id) {
        File mapFile = new File(getMapsDirectory(), "map_"+id+".dat");
        return getMapBytesFromFile(mapFile);
    }

    private byte[] getMapBytesLive(int id) {
        var map = Bukkit.getMap(id);
        if (map == null) {
            return null;
        }
        if (map.getRenderers().isEmpty()) {
            return null;
        }
        var renderer = map.getRenderers().get(0);

        try {
            Class<?> craftMapRendererClass = Class.forName("org.bukkit.craftbukkit."+getNMSVersion()+".map.CraftMapRenderer");
            Class<?> worldMapClass = Class.forName("net.minecraft.world.level.saveddata.maps.WorldMap");

            if (!craftMapRendererClass.isInstance(renderer)) {
                return null;
            }

            Field worldMapField = craftMapRendererClass.getDeclaredField("worldMap");
            worldMapField.setAccessible(true);
            Object worldMap = worldMapField.get(renderer);

            Field gField = worldMapClass.getDeclaredField("g");
            gField.setAccessible(true);

            return (byte[]) gField.get(worldMap);

        } catch (Exception e) {
            MilkyPixelart.getInstance().getLogger().log(Level.WARNING, "Error getting map bytes", e);
            return null;
        }
    }

    public CompletableFuture<List<String>> getDuplicates(@NotNull CommandSender commandSender, int mapId) {

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        CompletableFuture<List<String>> finalResult = new CompletableFuture<>();

        threadPoolExecutor.submit(() -> {

            byte[] originalBytes = getMapBytesOffline(mapId);

            if (originalBytes != null) {

                File dataDirectory = getMapsDirectory();
                File[] files = dataDirectory.listFiles();

                if (files != null) {
                    commandSender.sendMessage(LangManager.getInstance().getLang("scan.files_found", files.length+""));

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
                                        countUnboxed % 1000 == 0) {
                                    commandSender.sendMessage(LangManager.getInstance().getLang("scan.files_scanned", count+""));
                                }
                            }
                        }, threadPoolExecutor);
                    }

                    CompletableFuture.allOf(tasks).thenRun(() -> finalResult.complete(resultList));
                }

            }
            else {
                commandSender.sendMessage(LangManager.getInstance().getLang("scan.fail_no_map"));
            }
        });

        return finalResult;
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
        if (!(map.getItemMeta() instanceof MapMeta)) {
            return true;
        }
        if (!((MapMeta) map.getItemMeta()).hasMapView()) {
            return true;
        }
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
            UUID realOwnerFromLegacy = MigrationManager.getInstance().fromLegacyUUID(author.getUuid());
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

    private static String getNMSVersion() {
        return switch (MilkyPixelart.getInstance().getServer().getMinecraftVersion()) {
            case "1.20.4" -> "v1_20_R3";
            case "1.19.4" -> "v1_19_R3";
            case "1.18.2" -> "v1_18_R2";
            default -> throw new IllegalStateException("Unsupported minecraft version: " + MilkyPixelart.getInstance().getServer().getMinecraftVersion());
        };
    }

    private static VersionLevel getVersionLevel() {
        return switch (MilkyPixelart.getInstance().getServer().getMinecraftVersion()) {
            case "1.20.4", "1.19.4" -> VersionLevel.v1_20;
            case "1.18.2" -> VersionLevel.v1_18;
            default -> throw new IllegalStateException("Unsupported minecraft version: " + MilkyPixelart.getInstance().getServer().getMinecraftVersion());
        };
    }

    private enum VersionLevel {
        v1_18,
        v1_20
    }
}
