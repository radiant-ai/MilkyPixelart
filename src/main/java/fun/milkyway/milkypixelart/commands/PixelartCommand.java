package fun.milkyway.milkypixelart.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.ArtManager;
import fun.milkyway.milkypixelart.managers.BannerManager;
import fun.milkyway.milkypixelart.managers.CopyrightManager;
import fun.milkyway.milkypixelart.managers.PixelartManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
@CommandAlias("pixelart|pxt|art")
public class PixelartCommand extends BaseCommand {

    private final MilkyPixelart plugin;

    public PixelartCommand() {
        this.plugin = MilkyPixelart.getInstance();
    }

    @Default
    @CatchUnknown
    @Subcommand("help")
    public void onHelpCommand(CommandSender commandSender) {
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
                        Component.text(plugin.getDescription().getVersion()).color(TextColor.fromHexString("#FFFF99"))
                )
                .append(
                        Component.text(" by Radiant").color(TextColor.fromHexString("#9AFF0F"))
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("Плагин позволяет вам защищать ваши авторские баннеры и пиксельарты на картах. А также просматривать арты прямо на рынке.").color(TextColor.fromHexString("#CDCD6E")).decorate(TextDecoration.ITALIC)
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("Также вы можете добавлять до 16 дополнительных узоров на баннеры нажав на них ПКМ и держа нужную краску в руках.").color(TextColor.fromHexString("#CDCD6E")).decorate(TextDecoration.ITALIC)
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
                        Component.text("Стоимость защиты пиксельарта: ").color(TextColor.fromHexString("#FFFF99")).decorate(TextDecoration.ITALIC)
                )
                .append(
                        Component.text(PixelartManager.getInstance().getProtectionCost()+"$").color(TextColor.fromHexString("#9AFF0F")).decorate(TextDecoration.ITALIC)
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("Стоимость защиты баннера: ").color(TextColor.fromHexString("#FFFF99")).decorate(TextDecoration.ITALIC)
                )
                .append(
                        Component.text(BannerManager.getInstance().getProtectionCost()+"$").color(TextColor.fromHexString("#9AFF0F")).decorate(TextDecoration.ITALIC)
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("/art protect").color(TextColor.fromHexString("#9AFF0F"))
                )
                .append(
                        Component.text("- защитить предмет в руке, деньги будут списаны сразу и за все предметы").color(TextColor.fromHexString("#FFFF99"))
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("/art fix").color(TextColor.fromHexString("#9AFF0F"))
                )
                .append(
                        Component.text("- исправить предмет, если вы больше не можете его копировать после переноса базы данных").color(TextColor.fromHexString("#FFFF99"))
                )
                .append(
                        Component.newline()
                )
                .append(
                        Component.text("--------------------------------------").color(TextColor.fromHexString("#FFFF99"))
                )
                .build();
        commandSender.sendMessage(component);
    }

    @CommandPermission("pixelart.protect")
    @Subcommand("protect")
    public void onProtect(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        ArtManager protectionManager;

        if (ArtManager.isMap(item)) {
            protectionManager = PixelartManager.getInstance();
        }
        else if (ArtManager.isBanner(item)) {
            protectionManager = BannerManager.getInstance();
        }
        else {
            player.sendMessage(Component.text("Вы должны держать пиксельарт или баннер в руке!").color(TextColor.fromHexString("#FF995E")));
            return;
        }

        CopyrightManager.Author author = protectionManager.getAuthor(item);
        if (author == null) {
            int price = item.getAmount()*protectionManager.getProtectionCost();
            if (plugin.getEconomy().getBalance(player)>=price) {
                if (protectionManager.protect(player, item)) {
                    plugin.getEconomy().withdrawPlayer(player, price);
                    player.sendMessage(
                            Component.text("Вы защитили ваше творение! Денег списано: "+price+"$")
                                    .color(TextColor.fromHexString("#9AFF0F")));
                }
                else {
                    player.sendMessage(Component.text("Не удалось защитить предмет, на нём точно есть, что защищать?").color(TextColor.fromHexString("#FF995E")));
                }
            }
            else {
                player.sendMessage(
                        Component.text("У вас недостаточно денег для установки защиты от копирования, вам нужно: "+price+"$")
                                .color(TextColor.fromHexString("#FF995E")));
            }
        }
        else {
            player.sendMessage(Component.text("Этот предмет уже защищен!").color(TextColor.fromHexString("#FF995E")));
        }
    }

    @CommandPermission("pixelart.findduplicated")
    @Subcommand("findduplicated")
    public void onFindDuplicates(CommandSender commandSender, Integer mapId) {
        PixelartManager.getInstance().getDuplicates(commandSender, mapId).thenAccept(list -> {
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
        CopyrightManager.Author author = PixelartManager.getInstance().getAuthor(itemStack);
        if (author != null) {
            UUID toUUID = PixelartManager.getInstance().fromLegacyUUID(author.getUuid());
            if (toUUID != null) {
                PixelartManager.getInstance().protect(toUUID, null, itemStack);
                player.sendMessage(Component.text("Вы успешно исправили защищенный предмет!").color(TextColor.fromHexString("#9AFF0F")));
                return;
            }
        }
        player.sendMessage(Component.text("Не удалось исправить защищенный предмет!").color(TextColor.fromHexString("#FF995E")));
    }

    @CommandPermission("pixelart.reload")
    @Subcommand("reload")
    public void onReload(CommandSender commandSender) {
        commandSender.sendMessage(Component.text("Перезагружаем плагин...").color(TextColor.fromHexString("#FFFF99")));
        MilkyPixelart.getInstance().reload().thenRun(() -> {
            commandSender.sendMessage(Component.text("Плагин перезагружен!").color(TextColor.fromHexString("#9AFF0F")));
        });
    }

    @Subcommand("blacklist")
    @CommandPermission("pixelart.blacklist")
    public static class BlacklistCommands extends BaseCommand{

        @Subcommand("add")
        @CommandCompletion("номер_карты uuid_владельца")
        public void onAdd(CommandSender commandSender, Integer mapId, UUID uuid) {
            PixelartManager.getInstance().blacklistAdd(mapId, uuid);
            TextComponent.Builder builder = Component.text();
            builder.append(Component.text("Карта ").color(TextColor.fromHexString("#FFFF99")))
                    .append(Component.text(mapId).color(TextColor.fromHexString("#9AFF0F")))
                    .append(Component.text(" была добавлена в черный список с настоящим владельцем ").color(TextColor.fromHexString("#FFFF99")))
                    .append(Component.text(uuid.toString()).color(TextColor.fromHexString("#9AFF0F")));
            commandSender.sendMessage(builder.build());
        }

        @Subcommand("remove")
        @CommandCompletion("номер_карты")
        public void onRemove(CommandSender commandSender, Integer mapId) {
            if (PixelartManager.getInstance().blacklistRemove(mapId) != null) {
                commandSender.sendMessage(
                        Component.text("Карта "+mapId+" была удалена из черного списка")
                                .color(TextColor.fromHexString("#9AFF0F")));
            }
            else {
                commandSender.sendMessage(Component.text("Эта карта не в черном списке!").color(TextColor.fromHexString("#FF995E")));
            }
        }

        @Subcommand("list")
        @CommandCompletion("страница")
        public void onList(CommandSender commandSender, @Default("1") Integer page) {
            page = page < 1 ? 1 : page;
            ArrayList<Map.Entry<Integer, UUID>> list = PixelartManager.getInstance().blacklistList();
            if (list.isEmpty()) {
                commandSender.sendMessage(Component.text("Черный список пуст").color(TextColor.fromHexString("#FFFF99")));
            }
            else {
                int PER_PAGE = 12;
                int pages = (list.size() - 1) / PER_PAGE + 1;
                page = page > pages ? pages : page;

                commandSender.sendMessage(Component.text()
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
                commandSender.sendMessage(builder.build());

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
                commandSender.sendMessage(builder.build());
            }
        }
    }
}
