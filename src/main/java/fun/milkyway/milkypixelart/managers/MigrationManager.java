package fun.milkyway.milkypixelart.managers;

import fun.milkyway.milkypixelart.MilkyPixelart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class MigrationManager {
    private static MigrationManager instance;

    private Map<UUID, UUID> legacyToNewUUIDMap;

    public static MigrationManager getInstance() {
        if (instance == null) {
            instance = new MigrationManager();
        }
        return instance;
    }

    public synchronized static CompletableFuture<MigrationManager> reload() {
        CompletableFuture<MigrationManager> result = new CompletableFuture<>();
        Executors.newSingleThreadExecutor().execute(() -> {
            instance = new MigrationManager();
            result.complete(getInstance());
        });
        return result;
    }

    private MigrationManager() {
        initializeFixMap(new File(MilkyPixelart.getInstance().getDataFolder(), "replacementData.txt").getPath());
    }

    private void initializeFixMap(@NotNull String migrationFilePath) {
        legacyToNewUUIDMap = new HashMap<>();
        File entriesFile = new File(migrationFilePath);
        if (entriesFile.exists()) {
            try {
                Scanner scanner = new Scanner(entriesFile);
                while(scanner.hasNext()) {
                    String line = scanner.nextLine();
                    String[] tokens = line.split(":");
                    UUID uuidFrom = UUID.fromString(tokens[0]);
                    UUID uuidTo = UUID.fromString(tokens[1]);
                    legacyToNewUUIDMap.put(uuidFrom, uuidTo);
                }
            }
            catch (IllegalArgumentException | ArrayIndexOutOfBoundsException | FileNotFoundException exception) {
                MilkyPixelart.getInstance().getLogger().log(Level.WARNING, exception.getMessage(), exception);
            }
        }
    }
    public @Nullable UUID fromLegacyUUID(@NotNull UUID uuid) {
        return legacyToNewUUIDMap.get(uuid);
    }
}
