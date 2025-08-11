package fun.milkyway.milkypixelart;

import co.aikar.commands.Locales;
import co.aikar.commands.PaperCommandManager;
import fun.milkyway.milkypixelart.commands.CommandAddons;
import fun.milkyway.milkypixelart.commands.PixelartCommand;
import fun.milkyway.milkypixelart.managers.*;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class MilkyPixelart extends JavaPlugin {
    private static MilkyPixelart instance;
    private static boolean usingFolia;
    private Economy economy;
    private Metrics metrics;
    private FileConfiguration configuration;
    private FileConfiguration lang;

    private PaperCommandManager paperCommandManager;
    private PixelartCommand pixelartCommand;

    @Override
    public void onLoad() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            usingFolia = true;
        } catch (ClassNotFoundException e) {
            usingFolia = false;
        }
    }

    @Override
    public void onEnable() {
        instance = this;

        if (getDataFolder().mkdir()) {
            getLogger().info("Created data folder!");
        }

        configuration = loadConfig("config.yml", "config.yml");

        paperCommandManager = new PaperCommandManager(this);

        loadLocales();

        CommandAddons.addResolvers(paperCommandManager);
        CommandAddons.loadAliases(paperCommandManager);
        pixelartCommand = new PixelartCommand();
        paperCommandManager.registerCommand(pixelartCommand);

        if (!setupEconomy()) {
            getLogger().warning("No Economy implementation found, all operations will be free for players!");
        }

        //Inject managers
        PixelartManager.getInstance();
        BannerManager.getInstance();

        reloadBstats();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    @Override
    public void onDisable() {
        try {
            PixelartManager.getInstance().shutdown();
        }
        catch (Exception exception) {
            getLogger().log(Level.WARNING, exception.getMessage(), exception);
        }

        try {
            BannerManager.getInstance().shutdown();
        }
        catch (Exception exception) {
            getLogger().log(Level.WARNING, exception.getMessage(), exception);
        }
    }

    public @NotNull FileConfiguration loadConfig(@NotNull String fileName, @NotNull String fallbackFile) {
        try {
            File configFile = new File(getDataFolder(), fileName);
            InputStream defaultConfigStream = getResource(fileName);
            if (defaultConfigStream == null) {
                getLogger().warning("Could not find " + fileName + " in jar! Falling back to " + fallbackFile);
                defaultConfigStream = getResource(fallbackFile);
            }
            var configuration = YamlConfiguration.loadConfiguration(configFile);
            if (defaultConfigStream != null) {
                configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8)));
            }
            configuration.options().copyDefaults(true);
            configuration.save(configFile);
            return configuration;
        }
        catch (IOException exception) {
            getLogger().log(Level.WARNING, "Error occurred while loading config file:"+fileName, exception);
        }

        return new YamlConfiguration();
    }

    public void loadLocales() {
        switch (configuration.getString("lang", "en").toLowerCase(Locale.ROOT)) {
            case "ru" -> {
                lang = loadConfig("lang_ru.yml", "lang.yml");
                paperCommandManager.getLocales().setDefaultLocale(Locales.RUSSIAN);
            }
            default -> {
                lang = loadConfig("lang.yml", "lang.yml");
                paperCommandManager.getLocales().setDefaultLocale(Locales.ENGLISH);
            }
        }
    }

    public CompletableFuture<Void> reload() {
        configuration = loadConfig("config.yml", "config.yml");

        loadLocales();

        LangManager.reload();
        CopyrightManager.reload();
        reloadBstats();

        CommandAddons.loadAliases(paperCommandManager);

        paperCommandManager.unregisterCommand(pixelartCommand);
        pixelartCommand = new PixelartCommand();
        paperCommandManager.registerCommand(pixelartCommand);

        CompletableFuture<Object> reload1 = PixelartManager.reload().handle((m, e) -> {
            if (e != null) getLogger().log(Level.WARNING, "Error occurred while reloading pixelart manager!", e);
            return m;
        });
        CompletableFuture<Object> reload2 = BannerManager.reload().handle((m, e) -> {
            if (e != null) getLogger().log(Level.WARNING, "Error occurred while reloading banner manager!", e);
            return m;
        });
        CompletableFuture<Object> reload3 = MigrationManager.reload().handle((m, e) -> {
            if (e != null) getLogger().log(Level.WARNING, "Error occurred while reloading migration manager!", e);
            return m;
        });
        return CompletableFuture.allOf(reload1, reload2, reload3);
    }

    public static boolean isFolia() {
        return usingFolia;
    }

    public @Nullable Economy getEconomy() {
        return economy;
    }

    public static @NotNull MilkyPixelart getInstance() {
        return instance;
    }

    public @NotNull FileConfiguration getConfig() {
        return configuration;
    }

    public @NotNull FileConfiguration getLang() {
        return lang;
    }

    private void reloadBstats() {
        if (!configuration.getBoolean("bStats", true) && metrics == null) {
            return;
        }
        if (!configuration.getBoolean("bStats", true) && metrics != null) {
            getLogger().info("Disabling bStats");
            metrics.shutdown();
            metrics = null;
            return;
        }
        if (metrics != null) {
            return;
        }
        getLogger().info("Enabling bStats");
        metrics = new Metrics(this, 21107);
    }
}
