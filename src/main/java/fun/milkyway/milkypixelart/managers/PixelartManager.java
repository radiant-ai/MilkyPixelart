package fun.milkyway.milkypixelart.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.commands.CommandAddons;
import fun.milkyway.milkypixelart.listeners.AuctionPreviewListener;
import fun.milkyway.milkypixelart.listeners.IllegitimateArtListener;
import fun.milkyway.milkypixelart.listeners.PixelartProtectionListener;
import fun.milkyway.milkypixelart.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import org.bukkit.inventory.meta.ItemMeta;
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

    private Map<Integer, ItemStack> previewMapKeys;
    private Map<UUID, Long> lastShowMap;

    //SIGNLETON
    private PixelartManager() {
        super();

        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        executorService.setMaximumPoolSize(2);

        random = new Random();
        previewMapKeys = new HashMap<>();
        lastShowMap = new HashMap<>();

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

    public void showMaps(@NotNull Player player, boolean all) {
        long cooldown = getShowCooldown(player.getUniqueId());
        if (cooldown > 0 && !player.hasPermission("pixelart.show.cooldownbypass")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<#FF995E>Вы еще не можете этого делать <#FFFF99>"+cooldown+" <#FF995E>секунд!"));
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
                player.sendMessage(MiniMessage.miniMessage().deserialize("<#FF995E>У вас нет действительных карт в нижних слотах инвертаря!"));
            }
            else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<#FF995E>Вы держите в руке недействительную карту!"));
            }
            return;
        }
        Component message = LangManager.getInstance().getLang(
                "pixelarts.showMessage", player.getName(), MiniMessage.miniMessage().serialize(component));
        MilkyPixelart.getInstance().getServer().broadcast(message);
        putOnCooldown(player.getUniqueId());
    }

    private Component buildSingleMapComponent(@NotNull Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        return createPreviewComponent(itemStack);
    }

    private Component buildAllMapsComponent(@NotNull Player player) {
        TextComponent.Builder builder = Component.text();
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (itemStack != null) {
                Component component = createPreviewComponent(itemStack);
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

    private @Nullable Component createPreviewComponent(@NotNull ItemStack itemStack) {
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
        builder.append(Component.text("<").color(TextColor.fromHexString("#8BFF33")));
        if (itemMeta.hasDisplayName() && itemMeta.displayName() != null) {
            Component displayName = itemMeta.displayName();
            if (displayName != null) {
                displayName = displayName.color(TextColor.fromHexString("#8BFF33"));
                builder.append(displayName);
            }
        }
        else {
            builder.append(Component.text("Арт").color(NamedTextColor.WHITE));
        }
        builder.append(Component.text(">").color(TextColor.fromHexString("#8BFF33")));
        builder.hoverEvent(HoverEvent.showText(Component.text("Нажмите для предпросмотра").color(TextColor.fromHexString("#8BFF33"))));
        builder.clickEvent(ClickEvent.runCommand("/"+ CommandAddons.getAnyAlias()+" preview "+mapMeta.getMapView().getId()));
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
                    player1.sendActionBar(Component.text("Включен предпросмотр!").color(NamedTextColor.GREEN));
                }
                else {
                    player1.sendActionBar(Component.text("Ошибка предпросмотра!").color(NamedTextColor.RED));
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
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> killItemFrame(player, id), 100);

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
        //Pitch and yaw
        pc.getIntegers().write(4, 0);
        pc.getIntegers().write(5, (int) (l.getYaw() * 256.0F / 360.0F));
        //Data
        pc.getIntegers().write(6, direction);

        try {
            protocolManager.sendServerPacket(player, pc);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return 0;
        }

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

            CompletableFuture.supplyAsync(() -> getMapBytes(mapView.getId()), executorService).thenAcceptAsync(bytes -> {
                if (!mapView.getRenderers().isEmpty()) {

                    sendMapPacket(player, id, mapItemStack);

                    if (bytes != null) {
                        sendMapPacket(player, mapView.getId(), bytes);
                    }

                    result.complete(true);
                }

                result.complete(false);

            }, executorService);
        }

        return result;
    }

    private void sendMapPacket(@NotNull Player player, int itemFrameId, @NotNull ItemStack map) {
        PacketContainer pc = protocolManager .createPacket(PacketType.Play.Server.ENTITY_METADATA);
        pc.getIntegers().write(0, itemFrameId);
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setEntity(player);
        watcher.setObject(8, WrappedDataWatcher.Registry.getItemStackSerializer(false), map);
        pc.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

        try {
            protocolManager.sendServerPacket(player, pc);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void sendMapPacket(@NotNull Player player, int mapId,  byte @NotNull [] bytes) {
        WorldMap.b worldMap = new WorldMap.b(0 ,0, 128, 128, bytes);
        PacketPlayOutMap nmsPacket = new PacketPlayOutMap(mapId, (byte) 4, false, null, worldMap);
        PacketContainer pc2 = PacketContainer.fromPacket(nmsPacket);
        try {
            protocolManager.sendServerPacket(player, pc2);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void killItemFrame(@NotNull Player p, int id) {
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
