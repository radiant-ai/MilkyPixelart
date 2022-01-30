package fun.milkyway.milkypixelart;

import co.aikar.commands.PaperCommandManager;
import fun.milkyway.milkypixelart.commands.PixelartCommand;
import fun.milkyway.milkypixelart.pixelartmanager.PixelartManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class MilkyPixelart extends JavaPlugin {
    private PixelartManager pixelartManager;
    private Economy economy;

    @Override
    public void onEnable() {
        if (setupEconomy()) {
            pixelartManager = new PixelartManager(this);
            PaperCommandManager paperCommandManager = new PaperCommandManager(this);
            paperCommandManager.registerCommand(new PixelartCommand(economy, pixelartManager));
        }
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

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
