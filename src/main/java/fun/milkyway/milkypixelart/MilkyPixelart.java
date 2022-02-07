package fun.milkyway.milkypixelart;

import co.aikar.commands.Locales;
import co.aikar.commands.PaperCommandManager;
import fun.milkyway.milkypixelart.commands.ArgsResolver;
import fun.milkyway.milkypixelart.commands.PixelartCommand;
import fun.milkyway.milkypixelart.managers.BannerManager;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class MilkyPixelart extends JavaPlugin {
    private static MilkyPixelart instance;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        if (getDataFolder().mkdir()) {
            getLogger().info("Created data folder!");
        }

        PaperCommandManager paperCommandManager = new PaperCommandManager(this);
        paperCommandManager.getLocales().setDefaultLocale(Locales.RUSSIAN);
        ArgsResolver.addResolvers(paperCommandManager);
        paperCommandManager.registerCommand(new PixelartCommand());

        if (!setupEconomy()) {
            getLogger().severe("Economy not found, shutting down the server as it may cause big issues otherwise!");
            getServer().shutdown();
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
        PixelartManager.getInstance().shutdown();
        BannerManager.getInstance().shutdown();
    }

    public Economy getEconomy() {
        return economy;
    }

    public static @NotNull MilkyPixelart getInstance() {
        return instance;
    }
}
