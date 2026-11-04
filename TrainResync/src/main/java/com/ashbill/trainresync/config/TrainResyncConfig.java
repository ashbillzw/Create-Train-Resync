package com.ashbill.trainresync.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** Shared configuration for TrainResync. Missing options are disabled by default. */
public record TrainResyncConfig(boolean railwayDataProtection, String railwayTempArchive) {
    public static final TrainResyncConfig DEFAULT = new TrainResyncConfig(false, null);

    public static TrainResyncConfig load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) throw new IOException("Expected a JSON object: " + path);
            JsonElement option = root.getAsJsonObject().get("railwayDataProtection");
            if (option == null) return DEFAULT;
            if (!option.isJsonPrimitive() || !option.getAsJsonPrimitive().isBoolean()) {
                throw new IOException("railwayDataProtection must be a boolean: " + path);
            }
            JsonElement archive = root.getAsJsonObject().get("railwayTempArchive");
            return new TrainResyncConfig(option.getAsBoolean(),
                    archive != null && archive.isJsonPrimitive() && archive.getAsJsonPrimitive().isString()
                            ? archive.getAsString() : null);
        } catch (NoSuchFileException missing) {
            return DEFAULT;
        } catch (com.google.gson.JsonParseException invalid) {
            throw new IOException("Invalid TrainResync configuration: " + path, invalid);
        }
    }
}
