package fun.milkyway.milkypixelart;

import co.aikar.commands.Locales;
import co.aikar.commands.PaperCommandManager;
import fun.milkyway.milkypixelart.commands.CommandAddons;
import fun.milkyway.milkypixelart.commands.PixelartCommand;
import fun.milkyway.milkypixelart.managers.BannerManager;
import fun.milkyway.milkypixelart.managers.CopyrightManager;
import fun.milkyway.milkypixelart.managers.LangManager;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class MilkyPixelart extends JavaPlugin {
    private static MilkyPixelart instance;
    private Economy economy;
    private FileConfiguration configuration;

    private PaperCommandManager paperCommandManager;
    private PixelartCommand pixelartCommand;

    @Override
    public void onEnable() {
        instance = this;

        if (getDataFolder().mkdir()) {
            getLogger().info("Created data folder!");
        }

        try {
            loadConfig();
        } catch (IOException e) {
            getLogger().log(Level.WARNING, e.getMessage(), e);
        }

        paperCommandManager = new PaperCommandManager(this);
        paperCommandManager.getLocales().setDefaultLocale(Locales.RUSSIAN);

        CommandAddons.addResolvers(paperCommandManager);
        CommandAddons.loadAliases(paperCommandManager);
        pixelartCommand = new PixelartCommand();
        paperCommandManager.registerCommand(pixelartCommand);

        if (!setupEconomy()) {
            getLogger().severe("Economy not found, it may cause great issues! Install any economy provider like Essentials or CMI!");
        }

        //Inject managers
        PixelartManager.getInstance();
        BannerManager.getInstance();
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

    public void loadConfig() throws IOException {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getDataFolder().mkdir();
            saveDefaultConfig();
        }
        InputStream defaultConfigStream = getResource("config.yml");
        configuration = YamlConfiguration.loadConfiguration(configFile);
        if (defaultConfigStream != null) {
            configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream)));
        }
        configuration.options().copyDefaults(true);
        configuration.save(configFile);
    }

    public CompletableFuture<Void> reload() {
        try {
            loadConfig();
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Error occurred while reloading the plugin!", e);
        }
        LangManager.reload();
        CopyrightManager.reload();

        CommandAddons.loadAliases(paperCommandManager);

        paperCommandManager.unregisterCommand(pixelartCommand);
        pixelartCommand = new PixelartCommand();
        paperCommandManager.registerCommand(pixelartCommand);

        CompletableFuture<MilkyPixelart> result = new CompletableFuture<>();
        CompletableFuture<Object> reload1 = PixelartManager.reload().handle((m, e) -> {
            if (e != null) getLogger().log(Level.WARNING, "Error occurred while reloading pixelart manager!", e);
            return m;
        });
        CompletableFuture<Object> reload2 = BannerManager.reload().handle((m, e) -> {
            if (e != null) getLogger().log(Level.WARNING, "Error occurred while reloading banner manager!", e);
            return m;
        });
        return CompletableFuture.allOf(reload1, reload2);
    }

    public Economy getEconomy() {
        return economy;
    }

    public static @NotNull MilkyPixelart getInstance() {
        return instance;
    }

    public FileConfiguration getConfiguration() {
        return configuration;
    }
}
