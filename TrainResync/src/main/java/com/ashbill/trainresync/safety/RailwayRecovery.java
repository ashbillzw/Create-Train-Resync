package com.ashbill.trainresync.safety;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

final class RailwayRecovery {
    @FunctionalInterface
    interface Validator { void validate(Path file) throws IOException; }

    static Path archiveInterruptedSave(Path target, Path directory, Validator validator) throws IOException {
        Path tmp = AtomicRailwayFile.temporary(target);
        if (Files.notExists(tmp, LinkOption.NOFOLLOW_LINKS)) return null;
        if (!Files.isRegularFile(tmp, LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Missing or non-regular railway DAT or temporary file: " + target);
        var timestamp = Files.getLastModifiedTime(tmp).toInstant();
        var age = Duration.between(Files.getLastModifiedTime(target).toInstant(), timestamp).abs();
        if (age.compareTo(Duration.ofMinutes(15)) > 0)
            throw new IOException("Railway DAT and temporary file differ by " + age + ": " + target);
        validator.validate(target);
        if (directory == null || !directory.isAbsolute() || !Files.isDirectory(directory))
            throw new IOException("Railway temporary archive unavailable: " + directory);
        String prefix = "create_tracks-" + DateTimeFormatter.ofPattern("uuuuMMdd-HHmmssSSS'Z'")
                .withZone(ZoneOffset.UTC).format(timestamp) + "-";
        Path staging = Files.createTempFile(directory, prefix, ".part");
        Path archived = staging.resolveSibling(staging.getFileName().toString().replace(".part", ".dat.tmp"));
        try {
            Files.copy(tmp, staging, StandardCopyOption.REPLACE_EXISTING);
            try (var channel = java.nio.channels.FileChannel.open(staging, java.nio.file.StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            if (Files.mismatch(tmp, staging) != -1) throw new IOException("Railway archive verification failed: " + staging);
            Files.move(staging, archived, StandardCopyOption.ATOMIC_MOVE);
            Files.delete(tmp);
            return archived;
        } finally {
            Files.deleteIfExists(staging);
        }
    }
}
