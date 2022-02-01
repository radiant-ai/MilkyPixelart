package fun.milkyway.milkypixelart.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import fun.milkyway.milkypixelart.MilkyPixelart;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@CommandAlias("pixelart|pxt")
public class PixelartCommand extends BaseCommand {

    private MilkyPixelart plugin;

    private final int PER_PAGE = 12;

    public PixelartCommand(MilkyPixelart plugin) {
        this.plugin = plugin;
    }

    @Default
    @CatchUnknown
    @Subcommand("help")
    public void onHelpCommand(Player player) {
        Component component = Component.text()
                .append(
                        Component.newline()
                )
                .append(
                        Component.text(plugin.getName()).color(TextColor.fromHexString("#FFFF99")).decorate(TextDecoration.BOLD)
                )
                .append(
                        Component.text(" ")
                )
                .append(
                        Component.text("by Radiant ").color(TextColor.fromHexString("#9AFF0F"))
                )
                .append(
                        Component.text(plugin.getDescription().getVersion()).color(TextColor.fromHexString("#FFFF99"))
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("--------------------------------------").color(TextColor.fromHexString("#FFFF99"))
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("/pixelart protect").color(TextColor.fromHexString("#9AFF0F"))
                )
                .append(
                        Component.text("- защитить пиксельарт в руке (100$ за штуку), никто, кроме вас, не сможет скопировать ваш пиксельарт").color(TextColor.fromHexString("#FFFF99"))
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("/pixelart fix").color(TextColor.fromHexString("#9AFF0F"))
                )
                .append(
                        Component.text("- исправить пиксельарт, если вы больше не можете его копировать после переноса базы данных").color(TextColor.fromHexString("#FFFF99"))
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("--------------------------------------").color(TextColor.fromHexString("#FFFF99"))
                )
                .build();
        player.sendMessage(component);
    }

    @CommandPermission("pixelart.protect")
    @Subcommand("protect")
    public void onProtect(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType()== Material.FILLED_MAP) {
            UUID uuid = plugin.getPixelartManager().getAuthor(item);
            if (uuid == null) {
                int price = item.getAmount()*100;
                if (plugin.getEconomy().getBalance(player)>=price) {
                    plugin.getPixelartManager().protect(player, item);
                    plugin.getPixelartManager().getPlugin().getEconomy().withdrawPlayer(player, price);
                    player.sendMessage(
                            Component.text("Вы защитили ваш пиксельарт! Денег списано: "+price+"$")
                                    .color(TextColor.fromHexString("#9AFF0F")));
                }
                else {
                    player.sendMessage(
                            Component.text("У вас недостаточно денег для установки защиты от копирования, вам нужно: "+price+"$")
                                    .color(TextColor.fromHexString("#FF995E")));
                }
            }
            else {
                player.sendMessage(Component.text("Эта карта уже защищена!").color(TextColor.fromHexString("#FF995E")));
            }
        }
        else {
            player.sendMessage(Component.text("Вы должны держать пиксельарт в руке!").color(TextColor.fromHexString("#FF995E")));
        }
    }

    @CommandPermission("pixelart.findduplicated")
    @Subcommand("findduplicated")
    public void onFindDuplicates(CommandSender commandSender, Integer mapId) {
        plugin.getPixelartManager().getDuplicates(commandSender, mapId).thenAccept(list -> {
            commandSender.sendMessage(ChatColor.GREEN+"Найдено "+list.size()+" дубликатов:");
            for (String fileName : list) {
               commandSender.sendMessage(ChatColor.GRAY+""+fileName);
           }
        });
    }

    @CommandPermission("pixelart.fix")
    @Subcommand("fix")
    public void onFix(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        UUID fromUUID = plugin.getPixelartManager().getAuthor(itemStack);
        if (fromUUID != null) {
            UUID toUUID = plugin.getPixelartManager().fromLegacyUUID(fromUUID);
            if (toUUID != null) {
                plugin.getPixelartManager().protect(toUUID, null, itemStack);
                player.sendMessage(Component.text("Вы успешно исправили карту!").color(TextColor.fromHexString("#9AFF0F")));
                return;
            }
        }
        player.sendMessage(Component.text("Не удалось исправить карту!").color(TextColor.fromHexString("#FF995E")));
    }

    @CommandPermission("pixelart.reload")
    @Subcommand("reload")
    public void onReload(Player player) {
        player.sendMessage(Component.text("Перезагружаем плагин...").color(TextColor.fromHexString("#FFFF99")));
        plugin.reloadPixeartManager().thenAcceptAsync(manager -> {
            if (manager != null) {
                player.sendMessage(Component.text("Плагин перезагружен!").color(TextColor.fromHexString("#9AFF0F")));
            }
            else {
                player.sendMessage(Component.text("Ошибка перезагрузки, срочно проверьте консоль!").color(TextColor.fromHexString("#FF995E")));
            }
        });
    }

    @Subcommand("blacklist")
    @CommandPermission("pixelart.blacklist")
    public class BlacklistCommands extends BaseCommand{

        @Subcommand("add")
        @CommandCompletion("номер_карты uuid_владельца")
        public void onAdd(Player player, Integer mapId, UUID uuid) {
            plugin.getPixelartManager().blacklistAdd(mapId, uuid);
            TextComponent.Builder builder = Component.text();
            builder.append(Component.text("Карта ").color(TextColor.fromHexString("#FFFF99")))
                    .append(Component.text(mapId).color(TextColor.fromHexString("#9AFF0F")))
                    .append(Component.text(" была добавлена в черный список с настоящим владельцем ").color(TextColor.fromHexString("#FFFF99")))
                    .append(Component.text(uuid.toString()).color(TextColor.fromHexString("#9AFF0F")));
            player.sendMessage(builder.build());
        }

        @Subcommand("remove")
        @CommandCompletion("номер_карты")
        public void onRemove(Player player, Integer mapId) {
            if (plugin.getPixelartManager().blacklistRemove(mapId) != null) {
                player.sendMessage(
                        Component.text("Карта "+mapId+" была удалена из черного списка")
                                .color(TextColor.fromHexString("#9AFF0F")));
            }
            else {
                player.sendMessage(Component.text("Эта карта не в черном списке!").color(TextColor.fromHexString("#FF995E")));
            }
        }

        @Subcommand("list")
        @CommandCompletion("страница")
        public void onList(Player player, @Default("1") Integer page) {
            page = page < 1 ? 1 : page;
            ArrayList<Map.Entry<Integer, UUID>> list = plugin.getPixelartManager().blacklistList();
            if (list.isEmpty()) {
                player.sendMessage(Component.text("Черный список пуст").color(TextColor.fromHexString("#FFFF99")));
            }
            else {
                int pages = (list.size() - 1) / PER_PAGE + 1;
                page = page > pages ? pages : page;

                player.sendMessage(Component.text()
                        .append(Component.newline())
                        .append(Component.text("Черный список: ").color(TextColor.fromHexString("#FFFF99"))
                                .decorate(TextDecoration.BOLD))
                        .append(Component.newline())
                        .append(Component.text("--------------------------------------").color(TextColor.fromHexString("#FFFF99")))
                        .build());

                TextComponent.Builder builder = Component.text();
                for (int i = (page - 1) * PER_PAGE; i < page * PER_PAGE && i < list.size(); i++) {

                    int id = list.get(i).getKey();
                    String uuid = list.get(i).getValue().toString();

                    builder.append(Component.text("X").color(TextColor.fromHexString("#FF995E"))
                            .hoverEvent(HoverEvent.showText(Component.text("Удалить").color(TextColor.fromHexString("#FF995E"))))
                            .clickEvent(ClickEvent.runCommand("/pixelart blacklist remove "+id)));
                    builder.append(Component.text(" - ").color(TextColor.fromHexString("#FFFF99")));
                    builder.append(Component.text(String.format("%08d ", id)).color(TextColor.fromHexString("#9AFF0F"))
                            .hoverEvent(HoverEvent.showText(Component.text("Номер карты").color(TextColor.fromHexString("#9AFF0F")))));
                    builder.append(Component.text(uuid).color(TextColor.fromHexString("#FFEA6D"))
                            .hoverEvent(HoverEvent.showText(Component.text("UUID законного владельца (скопировать)").color(TextColor.fromHexString("#FFEA6D"))))
                            .clickEvent(ClickEvent.copyToClipboard(uuid)));
                    builder.append(Component.newline());
                }
                player.sendMessage(builder.build());

                builder = Component.text();
                builder.append(Component.text("--------------------------------------").color(TextColor.fromHexString("#FFFF99")));
                builder.append(Component.newline());
                builder.append(Component.text("   <<<< ").color(TextColor.fromHexString("#9AFF0F"))
                        .clickEvent(ClickEvent.runCommand("/pixelart blacklist list "+(page - 1))));
                builder.append(Component.text(page).color(TextColor.fromHexString("#FFFF99")))
                        .append(Component.text("/").color(TextColor.fromHexString("#9AFF0F")))
                        .append(Component.text(pages).color(TextColor.fromHexString("#FFFF99")));
                builder.append(Component.text(" >>>> ").color(TextColor.fromHexString("#9AFF0F"))
                        .clickEvent(ClickEvent.runCommand("/pixelart blacklist list "+(page + 1))));
                player.sendMessage(builder.build());
            }
        }
    }
}
