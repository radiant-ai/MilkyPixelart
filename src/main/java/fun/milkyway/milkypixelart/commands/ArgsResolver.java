package fun.milkyway.milkypixelart.commands;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;

import java.util.UUID;

public class ArgsResolver {
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
}
