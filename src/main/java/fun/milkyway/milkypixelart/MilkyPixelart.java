package fun.milkyway.milkypixelart;

import co.aikar.commands.Locales;
import co.aikar.commands.PaperCommandManager;
import fun.milkyway.milkypixelart.commands.ArgsResolver;
import fun.milkyway.milkypixelart.commands.PixelartCommand;
import fun.milkyway.milkypixelart.pixelartmanager.PixelartManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class MilkyPixelart extends JavaPlugin {
    private PixelartManager pixelartManager;
    private PaperCommandManager paperCommandManager;
    private Economy economy;

    @Override
    public void onEnable() {
        getDataFolder().mkdir();

        paperCommandManager = new PaperCommandManager(this);
        paperCommandManager.getLocales().setDefaultLocale(Locales.RUSSIAN);
        ArgsResolver.addResolvers(paperCommandManager);
        paperCommandManager.registerCommand(new PixelartCommand(this));

        if (!setupEconomy()) {
            getLogger().severe("Economy not found, shutting down the server as it may cause big issues otherwise!");
            getServer().shutdown();
        }
        pixelartManager = new PixelartManager(this);
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
        return economy != null;
    }

    public CompletableFuture<PixelartManager> reloadPixeartManager() {
        CompletableFuture<PixelartManager> result = new CompletableFuture();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                pixelartManager.shutdown();
                pixelartManager = new PixelartManager(this);
                result.complete(pixelartManager);
            } catch (InterruptedException e) {
                result.complete(null);
            }
        });
        return result;
    }

    @Override
    public void onDisable() {
        try {
            pixelartManager.shutdown();
        } catch (InterruptedException e) {
            getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }

    public Economy getEconomy() {
        return economy;
    }

    public PixelartManager getPixelartManager() {
        return pixelartManager;
    }

    public PaperCommandManager getPaperCommandManager() {
        return paperCommandManager;
    }
}
