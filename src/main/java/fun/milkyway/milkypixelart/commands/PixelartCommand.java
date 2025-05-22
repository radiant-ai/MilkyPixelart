package fun.milkyway.milkypixelart.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import fun.milkyway.milkypixelart.MilkyPixelart;
import fun.milkyway.milkypixelart.managers.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

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
    public void onProtect(@NotNull Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        ArtManager protectionManager = ArtManager.getInstance(item);

        if (protectionManager == null) {
            player.sendMessage(LangManager.getInstance().getLang("protect.fail_should_hold"));
            return;
        }

        CopyrightManager.Author author = protectionManager.getAuthor(item);
        if (author != null) {
            player.sendMessage(LangManager.getInstance().getLang("protect.fail_already_protected"));
            return;
        }

        int price = item.getAmount() * protectionManager.getProtectionCost();
        // Check if economy exists and player has enough money
        if (plugin.getEconomy() != null && plugin.getEconomy().getBalance(player) < price) {
            player.sendMessage(LangManager.getInstance().getLang("protect.fail_not_enough_money", ""+price));
            return;
        }

        if (!protectionManager.protect(player, item)) {
            player.sendMessage(LangManager.getInstance().getLang("protect.fail_nothing_to_protect"));
            return;
        }

        // Only withdraw money if economy exists
        if (plugin.getEconomy() != null) {
            plugin.getEconomy().withdrawPlayer(player, price);
            player.sendMessage(LangManager.getInstance().getLang("protect.success", ""+price));
        } else {
            // Protection is free when economy is null
            player.sendMessage(LangManager.getInstance().getLang("protect.success", "0"));
        }
    }

    @CommandPermission("pixelart.findduplicated")
    @Subcommand("scan")
    public void onFindDuplicates(CommandSender commandSender, Integer mapId) {
        PixelartManager.getInstance().getDuplicates(commandSender, mapId).thenAccept(list -> {
            if (list.isEmpty()) {
                return;
            }
            commandSender.sendMessage(LangManager.getInstance().getLang("scan.success", ""+list.size()));
            for (String fileName : list) {
               commandSender.sendMessage(LangManager.getInstance().getLang("scan.item", fileName));
            }
        });
    }

    @CommandPermission("pixelart.fix")
    @Subcommand("fix")
    public void onFix(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        ArtManager protectionManager = ArtManager.getInstance(itemStack);
        if (protectionManager == null) {
            player.sendMessage(LangManager.getInstance().getLang("protect.fail_should_hold"));
            return;
        }
        CopyrightManager.Author author = protectionManager.getAuthor(itemStack);
        if (author == null) {
            player.sendMessage(LangManager.getInstance().getLang("unprotect.fail_not_protected"));
            return;
        }
        UUID toUUID = MigrationManager.getInstance().fromLegacyUUID(author.getUuid());
        if (toUUID == null) {
            player.sendMessage(Component.text("Не найдены соответствия для переноса защиты!").color(TextColor.fromHexString("#FF995E")));
            return;
        }
        protectionManager.protect(toUUID, null, itemStack);
        player.sendMessage(Component.text("Вы успешно исправили защищенный предмет!").color(TextColor.fromHexString("#9AFF0F")));
    }

    @CommandPermission("pixelart.unprotect")
    @Subcommand("unprotect")
    public void onUnprotect(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        ArtManager protectionManager = ArtManager.getInstance(itemStack);
        if (protectionManager == null) {
            player.sendMessage(LangManager.getInstance().getLang("protect.fail_should_hold"));
            return;
        }
        CopyrightManager.Author author = protectionManager.getAuthor(itemStack);
        if (author == null) {
            player.sendMessage(LangManager.getInstance().getLang("unprotect.fail_not_protected"));
            return;
        }
        if(author.getUuid().equals(player.getUniqueId())) {
            player.sendMessage(LangManager.getInstance().getLang("unprotect.success"));
            protectionManager.unProtect(itemStack);
        } else {
            player.sendMessage(LangManager.getInstance().getLang("unprotect.fail_unprotected"));
        }
    }

    @CommandPermission("pixelart.preview")
    @Subcommand("preview")
    @CommandCompletion("map_id")
    public void onPreview(Player player, String mapUuid) {
        PixelartManager.getInstance().renderBundle(player, UUID.fromString(mapUuid));
    }

    @CommandPermission("pixelart.show")
    @Subcommand("show")
    @CommandCompletion("l|g|local|global")
    public void onShow(Player player, String localOrGlobal) {
        PixelartManager.getInstance().showMaps(player, false, localOrGlobal.toLowerCase().startsWith("l"));
    }

    @CommandPermission("pixelart.showall")
    @Subcommand("showall")
    @CommandCompletion("l|g|local|global")
    public void onShowAll(Player player, String localOrGlobal) {
        PixelartManager.getInstance().showMaps(player, true, localOrGlobal.toLowerCase().startsWith("l"));
    }

    @CommandPermission("pixelart.reload")
    @Subcommand("reload")
    public void onReload(CommandSender commandSender) {
        commandSender.sendMessage(LangManager.getInstance().getLang("reload.reloading"));
        MilkyPixelart.getInstance().reload().thenRun(() -> commandSender.sendMessage(LangManager.getInstance().getLang("reload.success")));
    }

    @Subcommand("blacklist")
    @CommandPermission("pixelart.blacklist")
    public static class BlacklistCommands extends BaseCommand{

        public BlacklistCommands() {

        }

        @Subcommand("add")
        @CommandCompletion("mad_id owner_uuid")
        public void onAdd(CommandSender commandSender, Integer mapId, UUID uuid) {
            PixelartManager.getInstance().blacklistAdd(mapId, uuid);
            commandSender.sendMessage(LangManager.getInstance().getLang("blacklist.add.success", ""+mapId, uuid.toString()));
        }

        @Subcommand("remove")
        @CommandCompletion("mad_id")
        public void onRemove(CommandSender commandSender, Integer mapId) {
            if (PixelartManager.getInstance().blacklistRemove(mapId) != null) {
                commandSender.sendMessage(LangManager.getInstance().getLang("blacklist.remove.success", ""+mapId));
            }
            else {
                commandSender.sendMessage(LangManager.getInstance().getLang("blacklist.remove.fail_not_on_list", ""+mapId));
            }
        }

        @Subcommand("list")
        @CommandCompletion("page_number")
        public void onList(CommandSender commandSender, @Default("1") Integer page) {
            page = page < 1 ? 1 : page;
            ArrayList<Map.Entry<Integer, UUID>> list = PixelartManager.getInstance().blacklistList();
            if (list.isEmpty()) {
                commandSender.sendMessage(LangManager.getInstance().getLang("blacklist.list.empty"));
            }
            else {
                int PER_PAGE = 12;
                int pages = (list.size() - 1) / PER_PAGE + 1;
                page = page > pages ? pages : page;

                commandSender.sendMessage(LangManager.getInstance().getLang("blacklist.list.header"));

                for (int i = (page - 1) * PER_PAGE; i < page * PER_PAGE && i < list.size(); i++) {

                    int id = list.get(i).getKey();
                    String uuid = list.get(i).getValue().toString();

                    commandSender.sendMessage(LangManager.getInstance().getLang("blacklist.list.item", ""+id, uuid));
                }
                commandSender.sendMessage(LangManager.getInstance().getLang("blacklist.list.footer", ""+page, ""+pages));
                commandSender.sendMessage(LangManager.getInstance().getLang("blacklist.list.page_controller", ""+(page - 1), ""+page, ""+(page + 1), ""+pages));
            }
        }
    }
}
