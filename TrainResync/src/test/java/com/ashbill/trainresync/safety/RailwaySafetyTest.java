package com.ashbill.trainresync.safety;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

/** Dependency-free executable regression tests. No game/server is started. */
public final class RailwaySafetyTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("child")) {
            AtomicRailwayFile.write(Path.of(args[1]), output -> {
                output.write("unfinished".getBytes(StandardCharsets.UTF_8));
                output.flush();
                System.out.println("WRITING");
                System.out.flush();
                try { Thread.sleep(Duration.ofMinutes(2).toMillis()); }
                catch (InterruptedException e) { throw new IOException(e); }
            });
            return;
        }
        Path base = Files.createTempDirectory("trainresync-railway-test-");
        try {
            Path newWorld = Files.createDirectory(base.resolve("new-world"));
            check(AtomicRailwayFile.isNewWorld(newWorld), "Genuinely new world may initialize");
            Files.write(newWorld.resolve("level.dat"), new byte[] {1});
            check(!AtomicRailwayFile.isNewWorld(newWorld), "Existing level metadata prevents initialization bypass");
            Files.delete(newWorld.resolve("level.dat"));
            Files.write(newWorld.resolve("level.dat_old"), new byte[] {1});
            check(!AtomicRailwayFile.isNewWorld(newWorld), "Old level metadata also identifies an existing world");
            Files.delete(newWorld.resolve("level.dat_old"));
            Files.createDirectory(newWorld.resolve("region"));
            check(!AtomicRailwayFile.isNewWorld(newWorld), "Existing regions remain protected when metadata is missing");
            Path target = base.resolve("create_tracks.dat");
            byte[] old = "previous-complete-file".getBytes(StandardCharsets.UTF_8);
            byte[] next = "new-complete-file".getBytes(StandardCharsets.UTF_8);
            Files.write(target, old);
            var permissions = Files.getFileAttributeView(target, PosixFileAttributeView.class);
            var previousPermissions = permissions == null ? null : permissions.readAttributes().permissions();
            AtomicRailwayFile.write(target, output -> output.write(next));
            check(Arrays.equals(Files.readAllBytes(target), next), "Successful replacement publishes new content");
            check(!Files.exists(AtomicRailwayFile.temporary(target)), "Successful commit removes temporary pathname");
            if (previousPermissions != null) {
                check(Files.getPosixFilePermissions(target).equals(previousPermissions), "POSIX permissions preserved");
            }
            Files.write(target, old);
            expectFailure(() -> AtomicRailwayFile.write(target, output -> {
                output.write(next, 0, 3);
                throw new IOException("Injected write failure");
            }), "Writer failure propagates");
            check(Arrays.equals(Files.readAllBytes(target), old), "Writer failure preserves old content");
            check(Files.exists(AtomicRailwayFile.temporary(target)), "Writer failure keeps evidence");
            expectFailure(() -> AtomicRailwayFile.checkState(target, false), "Leftover temporary blocks load");
            expectFailure(() -> AtomicRailwayFile.write(target, output -> output.write(next)), "Leftover temporary blocks save");
            Files.delete(AtomicRailwayFile.temporary(target)); // Explicit recovery in this isolated test only.

            expectFailure(() -> AtomicRailwayFile.write(target, output -> output.write(next),
                    (temporary, destination) -> { throw new IOException("Injected atomic replacement failure"); }),
                    "Replacement failure propagates");
            check(Arrays.equals(Files.readAllBytes(target), old), "Replacement failure preserves old content");
            check(Arrays.equals(Files.readAllBytes(AtomicRailwayFile.temporary(target)), next),
                    "Replacement failure keeps the completed but uncommitted file");
            Files.delete(AtomicRailwayFile.temporary(target));

            Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", System.getProperty("java.class.path"), RailwaySafetyTest.class.getName(),
                    "child", target.toString()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
            try {
                var ready = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try { return child.inputReader().readLine(); }
                    catch (IOException e) { throw new java.io.UncheckedIOException(e); }
                });
                check("WRITING".equals(ready.get(20, TimeUnit.SECONDS)), "Child reached real write operation");
                child.destroyForcibly();
                check(child.waitFor(10, TimeUnit.SECONDS), "Interrupted writer exits");
            } finally {
                if (child.isAlive()) { child.destroyForcibly(); child.waitFor(10, TimeUnit.SECONDS); }
            }
            check(Arrays.equals(Files.readAllBytes(target), old), "Process termination preserves old content");
            expectFailure(() -> AtomicRailwayFile.checkState(target, false), "Process interruption blocks next load");
            Files.delete(AtomicRailwayFile.temporary(target));
            Files.delete(target);
            expectFailure(() -> AtomicRailwayFile.checkState(target, false), "Missing established DAT blocks load");
            AtomicRailwayFile.checkState(target, true);
            checks++;

            CompoundTag root = minimalRoot();
            NbtIo.writeCompressed(root, target.toFile());
            RailwayDataGuard.validateGzip(target);
            RailwayDataGuard.validateRoot(root);
            checks++;
            byte[] gzip = Files.readAllBytes(target);
            Files.write(target, Arrays.copyOf(gzip, gzip.length - 4));
            expectFailure(() -> RailwayDataGuard.validateGzip(target), "Missing Gzip footer is rejected");
            gzip[gzip.length - 8] ^= 1;
            Files.write(target, gzip);
            expectFailure(() -> RailwayDataGuard.validateGzip(target), "Gzip checksum corruption is rejected");
            CompoundTag missing = root.copy();
            missing.getCompound("data").remove("Trains");
            expectFailure(() -> RailwayDataGuard.validateRoot(missing), "Missing railway list is rejected");
            CompoundTag invalid = root.copy();
            CompoundTag train = new CompoundTag();
            train.putUUID("Id", UUID.randomUUID());
            train.put("Carriages", new ListTag());
            ListTag trains = new ListTag();
            trains.add(train);
            trains.add(train.copy());
            invalid.getCompound("data").put("Trains", trains);
            expectFailure(() -> RailwayDataGuard.validateRoot(invalid), "Duplicate train identity is rejected");
            trains.remove(1);
            train.putUUID("Graph", UUID.randomUUID());
            expectFailure(() -> RailwayDataGuard.validateRoot(invalid), "Missing graph reference is rejected");

            // Optional user-provided fixture is read only; never copied back or rewritten.
            if (args.length >= 1) {
                Path fixture = Path.of(args[0]);
                RailwayDataGuard.validateGzip(fixture);
                RailwayDataGuard.validateRoot(NbtIo.readCompressed(fixture.toFile()));
                checks++;
                System.out.println("Healthy fixture accepted: " + fixture.getFileName());
            }
            if (args.length >= 2) {
                expectFailure(() -> RailwayDataGuard.validateGzip(Path.of(args[1])),
                        "Real truncated DAT is rejected");
                System.out.println("Damaged fixture rejected: " + Path.of(args[1]).getFileName());
            }
            System.out.println("PASS: " + checks + " railway safety checks");
        } finally {
            try (var paths = Files.walk(base)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static CompoundTag minimalRoot() {
        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", 3465);
        CompoundTag data = new CompoundTag();
        data.put("RailGraphs", new ListTag());
        data.put("SignalBlocks", new ListTag());
        data.put("Trains", new ListTag());
        ListTag dimensions = new ListTag();
        CompoundTag dimension = new CompoundTag();
        dimension.putString("Id", "minecraft:overworld");
        dimensions.add(dimension);
        data.put("DimensionPalette", dimensions);
        root.put("data", data);
        return root;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }

    private static void expectFailure(Checked action, String message) throws Exception {
        try { action.run(); }
        catch (IOException expected) { checks++; return; }
        throw new AssertionError(message);
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }
}
