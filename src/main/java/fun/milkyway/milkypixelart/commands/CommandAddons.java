package fun.milkyway.milkypixelart.commands;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import fun.milkyway.milkypixelart.MilkyPixelart;

import java.util.List;
import java.util.UUID;

public class CommandAddons {
    public static void addResolvers(PaperCommandManager paperCommandManager) {
        paperCommandManager.getCommandContexts().registerContext(UUID.class, context -> {
            try {
                return UUID.fromString(context.popFirstArg());
            }
            catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument("Неверный UUID!");
            }
        });
    }

    public static void loadAliases(PaperCommandManager paperCommandManager) {
        List<String> aliasList = MilkyPixelart.getInstance().getConfiguration().getStringList("commands.aliases");
        if (aliasList.isEmpty()) {
            return;
        }
        paperCommandManager.getCommandReplacements().addReplacements("pixelartcommand", String.join("|", aliasList));
    }
}
