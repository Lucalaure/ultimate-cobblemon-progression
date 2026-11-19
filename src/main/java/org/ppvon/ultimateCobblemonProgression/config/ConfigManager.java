package org.ppvon.ultimateCobblemonProgression.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

final class ConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private ConfigManager() {}

    static Path configDir() {
        Path dir = FabricLoader.getInstance().getConfigDir()
                .resolve("ultimate-cobblemon-progression");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory: " + dir, e);
        }
        return dir;
    }

    static <T> T loadOrCreate(Path file, Class<T> type, T defaults) {
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    T val = GSON.fromJson(r, type);
                    // If parsing somehow returns null (corrupt/empty), fall back to defaults.
                    if (val == null) {
                        save(file, defaults);
                        return defaults;
                    }
                    // Write it back so options added in newer versions show up in the file. Best effort:
                    // a file that can't be written must not count as broken and be reset.
                    try {
                        save(file, val);
                    } catch (RuntimeException ignored) {}
                    return val;
                }
            } else {
                save(file, defaults);
                return defaults;
            }
        } catch (Exception ex) {
            // On any error, back up the broken file and regenerate defaults.
            try {
                if (Files.exists(file)) {
                    Path bak = file.resolveSibling(file.getFileName().toString() + ".bak");
                    Files.copy(file, bak);
                }
            } catch (IOException ignored) {}
            save(file, defaults);
            return defaults;
        }
    }

    static <T> void save(Path file, T data) {
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(data, w);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + file, e);
        }
    }
}
