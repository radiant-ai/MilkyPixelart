package fun.milkyway.milkypixelart.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
@CommandAlias("%pixelartcommand")
public class PixelartCommand extends BaseCommand {

    private final MilkyPixelart plugin;
    private final Component pluginTitleComponent;

    public PixelartCommand() {
        this.plugin = MilkyPixelart.getInstance();
        pluginTitleComponent = MiniMessage.miniMessage()
                .deserialize(" \n<#FFFF99><bold>"+plugin.getName()+"</bold> <#9AFF0F>"+plugin.getDescription().getVersion()+" by <#9AFF0F>Radiant");
    }

    @Default
    @CatchUnknown
    @Subcommand("help")
    public void onHelpCommand(CommandSender commandSender) {
        commandSender.sendMessage(pluginTitleComponent);
        LangManager.getInstance().getLangList("help.commands").forEach(commandSender::sendMessage);
        commandSender.sendMessage(LangManager.getInstance().getLang("help.prices.pixelart",
                ""+PixelartManager.getInstance().getProtectionCost()));
        commandSender.sendMessage(LangManager.getInstance().getLang("help.prices.banner",
                ""+BannerManager.getInstance().getProtectionCost()));
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
            player.sendMessage(LangManager.getInstance().getLang("protect.fail_should_hold"));
            return;
        }

        CopyrightManager.Author author = protectionManager.getAuthor(item);
        if (author == null) {
            int price = item.getAmount()*protectionManager.getProtectionCost();
            if (plugin.getEconomy().getBalance(player)>=price) {
                if (protectionManager.protect(player, item)) {
                    plugin.getEconomy().withdrawPlayer(player, price);
                    player.sendMessage(LangManager.getInstance().getLang("protect.success", ""+price));
                }
                else {
                    player.sendMessage(LangManager.getInstance().getLang("protect.fail_nothing_to_protect"));
                }
            }
            else {
                player.sendMessage(LangManager.getInstance().getLang("protect.fail_not_enough_money", ""+price));
            }
        }
        else {
            player.sendMessage(LangManager.getInstance().getLang("protect.fail_already_protected"));
        }
    }

    @CommandPermission("pixelart.findduplicated")
    @Subcommand("findduplicated")
    @CommandAlias("scan")
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

    @CommandPermission("pixelart.preview")
    @Subcommand("preview")
    @CommandCompletion("номер_карты")
    public void onPreview(Player player, Integer mapId) {
        PixelartManager.getInstance().renderArt(player, mapId);
    }

    @CommandPermission("pixelart.show")
    @Subcommand("show")
    @CommandCompletion("номер_карты")
    public void onShow(Player player) {
        PixelartManager.getInstance().showMaps(player, false);
    }

    @CommandPermission("pixelart.showall")
    @Subcommand("showall")
    @CommandCompletion("номер_карты")
    public void onShowAll(Player player) {
        PixelartManager.getInstance().showMaps(player, true);
    }

    @CommandPermission("pixelart.reload")
    @Subcommand("reload")
    public void onReload(CommandSender commandSender) {
        commandSender.sendMessage(LangManager.getInstance().getLang("reload.reloading"));
        MilkyPixelart.getInstance().reload().thenRun(() -> commandSender.sendMessage(LangManager.getInstance().getLang("reload.success")));
    }

    @Subcommand("blacklist")
    @CommandPermission("pixelart.blacklist")
    public class BlacklistCommands extends BaseCommand{

        public BlacklistCommands() {

        }

        @Subcommand("add")
        @CommandCompletion("mad_id owner_uuid")
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
        @CommandCompletion("mad_id")
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
        @CommandCompletion("page_number")
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
