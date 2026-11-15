package com.ashbill.trainresync.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** Shared configuration for TrainResync. Missing options are disabled by default. */
public record TrainResyncConfig(boolean railwayDataProtection, String railwayTempArchive, boolean playerSponsorship) {
    public static final TrainResyncConfig DEFAULT = new TrainResyncConfig(false, null, false);

    /** Select each configuration independently, so existing single-file installations still work. */
    public static Path file(Path configDirectory, String filename) {
        Path current = configDirectory.resolve("trainresync").resolve(filename);
        return Files.notExists(current) ? configDirectory.resolve("trainresync.json") : current;
    }

    public static TrainResyncConfig load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) throw new IOException("Expected a JSON object: " + path);
            JsonElement option = root.getAsJsonObject().get("railwayDataProtection");
            if (option != null && (!option.isJsonPrimitive() || !option.getAsJsonPrimitive().isBoolean())) {
                com.mojang.logging.LogUtils.getLogger().warn("Ignoring non-boolean railwayDataProtection in {}", path);
                option = null;
            }
            JsonElement archive = root.getAsJsonObject().get("railwayTempArchive");
            JsonElement sponsor = root.getAsJsonObject().get("playerSponsorship");
            return new TrainResyncConfig(option != null && option.getAsBoolean(),
                    archive != null && archive.isJsonPrimitive() && archive.getAsJsonPrimitive().isString()
                            ? archive.getAsString() : null,
                    sponsor != null && sponsor.isJsonPrimitive() && sponsor.getAsJsonPrimitive().isBoolean() && sponsor.getAsBoolean());
        } catch (NoSuchFileException missing) {
            return DEFAULT;
        } catch (com.google.gson.JsonParseException invalid) {
            throw new IOException("Invalid TrainResync configuration: " + path, invalid);
        }
    }
}
