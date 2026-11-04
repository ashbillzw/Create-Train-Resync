package com.ashbill.trainresync.config;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.fml.loading.FMLPaths;

/** Prism writes its marker at the instance root; a dedicated server owns its game directory. */
public final class NiijimaPack {
    public static final boolean MANAGED = hasMarker(FMLPaths.GAMEDIR.get().toAbsolutePath().normalize());

    static boolean hasMarker(Path game) {
        Path marker = game.resolve("niijima-pack.json");
        Path parent = game.getParent();
        if (!Files.exists(marker) && parent != null && Files.isRegularFile(parent.resolve("instance.cfg"))
                && ("minecraft".equals(game.getFileName().toString()) || ".minecraft".equals(game.getFileName().toString())))
            marker = parent.resolve("niijima-pack.json");
        if (!Files.isRegularFile(marker)) return false;
        try (var reader = Files.newBufferedReader(marker)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            var token = json.getAsJsonPrimitive("token");
            var id = json.getAsJsonPrimitive("pack-id");
            if (token == null || !token.isNumber() || token.getAsDouble() != 114514 || id == null || !id.isString()) return false;
            String digits = id.getAsString();
            return digits.matches("[1-9][0-9]{0,19}") && Long.parseUnsignedLong(digits) != 0;
        } catch (IOException | RuntimeException invalid) {
            return false;
        }
    }
}
