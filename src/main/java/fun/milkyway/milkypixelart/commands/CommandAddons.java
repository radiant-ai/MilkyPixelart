package fun.milkyway.milkypixelart.commands;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import fun.milkyway.milkypixelart.MilkyPixelart;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class CommandAddons {
    public static void addResolvers(PaperCommandManager paperCommandManager) {
        paperCommandManager.getCommandContexts().registerContext(UUID.class, context -> {
            try {
                return UUID.fromString(context.popFirstArg());
            }
            catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument("Invalid UUID!");
            }
        });
    }

    public static void loadAliases(PaperCommandManager paperCommandManager) {
        List<String> aliasList = MilkyPixelart.getInstance().getConfig().getStringList("commands.aliases");
        if (aliasList.isEmpty()) {
            throw new IllegalStateException("No command aliases found in the config.yml!");
        }
        paperCommandManager.getCommandReplacements().addReplacements("pixelartcommand", String.join("|", aliasList));
    }

    public static @NotNull String getAnyAlias() {
        List<String> aliasList = MilkyPixelart.getInstance().getConfig().getStringList("commands.aliases");
        if (aliasList.isEmpty()) {
            throw new IllegalStateException("No command aliases found in the config.yml!");
        }
        return aliasList.get(0);
    }
}
