package fun.milkyway.milkypixelart.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import fun.milkyway.milkypixelart.pixelartmanager.PixelartManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@CommandAlias("pixelart")
public class PixelartCommand extends BaseCommand {

    private final PixelartManager pixelartManager;
    private final Economy economy;

    public PixelartCommand(Economy economy, PixelartManager pixelartManager) {
        this.economy = economy;
        this.pixelartManager = pixelartManager;
    }

    @Default
    @CatchUnknown
    @HelpCommand
    @Subcommand("help")
    public void onHelpCommand(Player player) {
        Component component = Component.text()
                .append(
                        Component.text(pixelartManager.getPlugin().getName()).color(TextColor.fromHexString("#FFFF99"))
                )
                .append(
                        Component.text(" ")
                )
                .append(
                        Component.text("by Radiant").color(TextColor.fromHexString("#9AFF0F"))
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("-----------------------------").color(TextColor.fromHexString("#FFFF99"))
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("/pixelart protect").color(TextColor.fromHexString("#9AFF0F"))
                )
                .append(
                        Component.text("- защитить пиксельарт в руке (100$ за штуку)").color(TextColor.fromHexString("#FFFF99"))
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("-----------------------------").color(TextColor.fromHexString("#FFFF99"))
                )
                .build();
        player.sendMessage(component);
    }

    @CommandPermission("protect")
    @Subcommand("protect")
    public void onProtect(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType()== Material.FILLED_MAP) {
            UUID uuid = pixelartManager.getAuthor(item);
            if (uuid == null) {
                int price = item.getAmount()*100;
                if (economy.getBalance(player)>=price) {
                    pixelartManager.protect(player, item);
                    economy.withdrawPlayer(player, price);
                    player.sendMessage(
                            Component.text("Вы защитили ваш пиксельарт! Денег списано: "+price+"$")
                                    .color(NamedTextColor.GREEN));
                }
                else {
                    player.sendMessage(
                            Component.text("У вас недостаточно денег для установки защиты от копирования, вам нужно: "+price+"$")
                                    .color(NamedTextColor.RED));
                }
            }
            else {
                player.sendMessage(Component.text("Эта карта уже защищена!").color(NamedTextColor.RED));
            }
        }
        else {
            player.sendMessage(Component.text("Вы должны держать пиксельарт в руке!").color(NamedTextColor.RED));
        }
    }
}
