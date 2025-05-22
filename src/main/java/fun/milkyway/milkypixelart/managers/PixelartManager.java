package fun.milkyway.milkypixelart.managers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.listeners.*;
import fun.milkyway.milkypixelart.utils.ActiveFrame;
import fun.milkyway.milkypixelart.utils.Utils;
import fun.milkyway.milkypixelart.utils.Versions;
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
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.IntStream;

public class PixelartManager extends ArtManager {
    public static final String BLACKLIST_FILENAME = "blacklist.yml";
    private static final NamespacedKey PREVIEW_ART_KEY = new NamespacedKey(MilkyPixelart.getInstance(), "previewArt");
    private static PixelartManager instance;

    private final Random random;
    private final ThreadPoolExecutor executorService;

    private final Map<Integer, UUID> blackList;
    private final Map<UUID, Long> lastMapShownTime;
    private final Map<UUID, List<Entity>> previewArts;
    private final Cache<UUID, BundleArt> previewBundles;

    //SIGNLETON
    private PixelartManager() {
        super();

        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        executorService.setMaximumPoolSize(2);

        random = new Random();
        blackList = new ConcurrentHashMap<>();
        lastMapShownTime = new HashMap<>();
        previewArts = new HashMap<>();
        previewBundles = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build();

        loadBlacklist();

        registerListener(new PixelartProtectionListener());
        registerListener(new AuctionPreviewListener());
        registerListener(new IllegitimateArtListener());
        registerListener(new MapCreateListener());
        registerListener(new MapPreviewListener());
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
        return MilkyPixelart.getInstance().getConfig().getInt("pixelarts.copyrightPrice");
    }

    public void showMaps(@NotNull Player player, boolean all, boolean local) {
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
        if (local) {
            player.getLocation().getNearbyPlayers(100).forEach(p -> {
                p.sendMessage(message);
            });
        }
        else {
            MilkyPixelart.getInstance().getServer().broadcast(message, "pixelart.preview");
        }
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
        lastMapShownTime.put(uuid, System.currentTimeMillis());
    }

    public long getShowCooldown(@NotNull UUID uuid) {
        if (!lastMapShownTime.containsKey(uuid)) {
            return 0;
        }
        int cooldown = MilkyPixelart.getInstance().getConfig().getInt("pixelarts.showArtCooldown");
        if (lastMapShownTime.get(uuid) + cooldown * 1000L < System.currentTimeMillis()) {
            lastMapShownTime.remove(uuid);
            return 0;
        }
        return (lastMapShownTime.get(uuid) - System.currentTimeMillis()) / 1000L + cooldown;
    }

    private @Nullable Component createPreviewComponent(@NotNull ItemStack itemStack, @NotNull String name) {
        BundleArt bundleArt = null;
        if (itemStack.getType() == Material.FILLED_MAP || MaterialUtils.isBundle(itemStack.getType())) {
            bundleArt = BundleArt.of(itemStack);
        }
        if (bundleArt == null) {
            return null;
        }
        var uuid = UUID.randomUUID();
        previewBundles.put(uuid, bundleArt);

        TextComponent.Builder builder = Component.text();
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName() && itemMeta.displayName() != null) {
            var displayName = itemMeta.displayName();
            if (displayName != null) {
                builder.append(LangManager.getInstance().getLang("show.map_component",
                        uuid+"",
                        PlainTextComponentSerializer.plainText().serialize(displayName)));
            }
        }
        else {
            builder.append(LangManager.getInstance().getLang("show.map_component",
                    uuid+"",
                    LangManager.getInstance().getLangPlain("show.map_default_name")));
        }
        builder.hoverEvent(HoverEvent.showText(LangManager.getInstance().getLang("show.click_to_preview", name)));
        return builder.build();
    }

    public boolean renderBundle(@NotNull Player player, UUID bundleUuid) {
        var bundleArt = previewBundles.getIfPresent(bundleUuid);
        if (bundleArt == null) {
            return false;
        }
        renderArts(player, bundleArt.getItemStacks(), bundleArt.getWidth(), bundleArt.getHeight());
        return true;
    }

    public boolean renderBundle(@NotNull Player player, @NotNull ItemStack stack) {
        var bundleArt = BundleArt.of(stack);
        if (bundleArt == null) {
            return false;
        }
        renderArts(player, bundleArt.getItemStacks(), bundleArt.getWidth(), bundleArt.getHeight());
        return true;
    }

    public boolean renderArts(@NotNull Player player, @NotNull List<ItemStack> stacks, int width, int height) {
        var playerUuid = player.getUniqueId();
        clearPreviewArts(playerUuid);
        Bukkit.getAsyncScheduler().runNow(MilkyPixelart.getInstance(), t -> {

            var face = OrientationUtils.calculateOpositeBlockFace(player.getLocation().getYaw());
            var grid = OrientationUtils.calculateGridInFrontOfPlayer(player.getLocation(), 2, width, height);

            var itemFrames = IntStream.range(0, grid.size()).mapToObj(i -> {
                var stack = stacks.get(i);
                var location = grid.get(i);
                if (MilkyPixelart.getInstance().getConfig().getBoolean("pixelarts.preventOverlapDisplay", true)) {
                    var otherHanging = location.getNearbyEntitiesByType(Hanging.class, 1.01);
                    if (otherHanging.stream().anyMatch(hanging -> !hanging.getPersistentDataContainer().has(PREVIEW_ART_KEY))) {
                        return null;
                    }
                }
                return player.getWorld().spawn(location, GlowItemFrame.class, glowItemFrame -> {
                    glowItemFrame.setPersistent(false);
                    glowItemFrame.setInvulnerable(true);
                    glowItemFrame.setFixed(true);
                    glowItemFrame.setItem(stack, false);
                    glowItemFrame.setVisible(false);
                    glowItemFrame.setFacingDirection(face, true);
                    glowItemFrame.getPersistentDataContainer().set(PREVIEW_ART_KEY, PersistentDataType.BYTE, (byte) 1);
                });
            }).filter(Objects::nonNull).toList();

            MilkyPixelart.getInstance().getServer().getAsyncScheduler().runAtFixedRate(MilkyPixelart.getInstance(), task -> {
                itemFrames.forEach(entity -> {
                    if (entity.isValid()) {
                        entity.remove();
                    }
                });
            }, MilkyPixelart.getInstance().getConfig().getInt("pixelarts.previewDuration", 100));

            MilkyPixelart.getInstance().getServer().getOnlinePlayers().forEach(p -> {
                if (p.getUniqueId() == player.getUniqueId()) {
                    // do not hide for self
                    return;
                }
                itemFrames.forEach(itemFrame -> {
                    p.hideEntity(MilkyPixelart.getInstance(), itemFrame);
                });
            });

            previewArts.put(player.getUniqueId(), itemFrames);

        });
        return true;
    }

    public void hidePreviewArts(@NotNull Player player) {
        previewArts.values().forEach(itemFrames -> {
            itemFrames.forEach(itemFrame -> {
                player.hideEntity(MilkyPixelart.getInstance(), itemFrame);
            });
        });
    }

    public void clearPreviewArts(@NotNull UUID playerUuid) {
        var removed = previewArts.remove(playerUuid);
        if (removed == null) {
            return;
        }
        removed.forEach(entity -> {
            if (entity.isValid()) {
                entity.remove();
            }
        });
    }

    public boolean isPreviewItemFrame(@NotNull Entity entity) {
        return entity instanceof ItemFrame && entity.getPersistentDataContainer().has(PREVIEW_ART_KEY, PersistentDataType.BYTE);
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
        blackList.clear();
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
}
