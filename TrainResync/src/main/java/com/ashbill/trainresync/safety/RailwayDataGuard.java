package com.ashbill.trainresync.safety;

import com.mojang.logging.LogUtils;
import com.ashbill.trainresync.config.TrainResyncConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = "trainresync", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RailwayDataGuard {
    public static final String DATA_NAME = "create_tracks";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Path> ENABLED = ConcurrentHashMap.newKeySet();
    private static final Set<Path> NEW_WORLDS = ConcurrentHashMap.newKeySet();
    private static final Set<Path> BLOCKED = ConcurrentHashMap.newKeySet();

    private RailwayDataGuard() {}

    private static Path key(Path path) { return path.toAbsolutePath().normalize(); }

    private static Path target(MinecraftServer server) {
        return key(server.getWorldPath(LevelResource.ROOT).resolve("data/create_tracks.dat"));
    }

    @SubscribeEvent
    public static void beforeServerStart(ServerAboutToStartEvent event) {
        if (!ModList.get().isLoaded("create")) return;
        MinecraftServer server = event.getServer();
        Path target = target(server);
        ENABLED.remove(target);
        NEW_WORLDS.remove(target);
        BLOCKED.remove(target);
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("trainresync.json");
        try {
            if (!TrainResyncConfig.load(configPath).railwayDataProtection()) return;
        } catch (IOException failure) {
            LOGGER.warn("[TrainResync] Cannot read {}. Railway data protection is disabled.", configPath, failure);
            return;
        }
        ENABLED.add(target);
        LOGGER.info("[TrainResync] Railway data protection enabled for {}", target);
        // Snapshot before loadLevel initializes the world. A fresh integrated world already
        // has level.dat here, so file existence cannot identify its first initialization.
        // An initialized world installing Create for the first time still needs migration.
        boolean newWorld = !server.getWorldData().overworldData().isInitialized();
        if (newWorld) NEW_WORLDS.add(target);

        // Integrated servers validate during railway loading, after the loading
        // progress listener exists, so failures can reach the normal error screen.
        if (!server.isDedicatedServer()) return;

        try {
            checkLoad(target);
        } catch (IOException failure) {
            throw loadingFailed(target, failure);
        }
    }

    @SubscribeEvent
    public static void afterServerStop(ServerStoppedEvent event) {
        Path target = target(event.getServer());
        ENABLED.remove(target);
        NEW_WORLDS.remove(target);
        BLOCKED.remove(target);
    }

    public static boolean isEnabled(Path target) { return ENABLED.contains(key(target)); }

    public static void checkLoad(Path target) throws IOException {
        if (!isEnabled(target)) return;
        if (BLOCKED.contains(key(target))) throw new IOException("Railway data is blocked: " + target);
        AtomicRailwayFile.checkState(target, NEW_WORLDS.contains(key(target)));
    }

    public static boolean isBlocked(Path target) { return BLOCKED.contains(key(target)); }

    public static IllegalStateException loadingFailed(Path target, Exception cause) {
        BLOCKED.add(key(target));
        LOGGER.error("[TrainResync] Railway data rejected; server startup is blocked. File: {}. "
                + "Do not remove the temporary file or replace the DAT without inspecting both.", target, cause);
        return new IllegalStateException("[TrainResync] Cannot start with unsafe railway data: " + target, cause);
    }

    public static void savingFailed(Path target, Exception cause) {
        if (!BLOCKED.add(key(target))) return;
        LOGGER.error("[TrainResync] Railway save failed. Preserving the previous DAT and any temporary file; "
                + "requesting server shutdown. File: {}", target, cause);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.halt(false);
    }

    /** Read through EOF: NBT parsing alone need not consume the Gzip checksum/trailer. */
    public static void validateGzip(Path target) throws IOException {
        try (GZIPInputStream input = new GZIPInputStream(Files.newInputStream(target))) {
            byte[] buffer = new byte[8192];
            while (input.read(buffer) != -1) { /* Validate the entire compressed stream. */ }
        }
    }

    public static void validateRoot(CompoundTag root) throws IOException {
        require(root.contains("DataVersion", Tag.TAG_ANY_NUMERIC), "Missing DataVersion");
        require(root.contains("data", Tag.TAG_COMPOUND), "Missing railway data compound");
        CompoundTag data = root.getCompound("data");
        ListTag graphs = list(data, "RailGraphs", Tag.TAG_COMPOUND);
        list(data, "SignalBlocks", Tag.TAG_COMPOUND);
        ListTag dimensions = list(data, "DimensionPalette", Tag.TAG_COMPOUND);
        for (Tag value : dimensions) {
            require(((CompoundTag) value).contains("Id", Tag.TAG_STRING), "Missing dimension identifier");
        }
        ListTag trains = list(data, "Trains", Tag.TAG_COMPOUND);
        Set<UUID> graphIds = new HashSet<>();
        for (Tag value : graphs) {
            CompoundTag graph = (CompoundTag) value;
            require(graph.hasUUID("Id"), "Invalid rail graph UUID");
            require(graphIds.add(graph.getUUID("Id")), "Duplicate rail graph UUID");
        }
        Set<UUID> trainIds = new HashSet<>();
        Set<UUID> carriageIds = new HashSet<>();
        for (Tag value : trains) {
            CompoundTag train = (CompoundTag) value;
            require(train.hasUUID("Id"), "Invalid train UUID");
            UUID id = train.getUUID("Id");
            require(trainIds.add(id), "Duplicate train UUID: " + id);
            if (train.contains("Graph")) {
                require(train.hasUUID("Graph") && graphIds.contains(train.getUUID("Graph")),
                        "Train references a missing/invalid graph: " + id);
            }
            ListTag carriages = list(train, "Carriages", Tag.TAG_COMPOUND);
            for (int i = 0; i < carriages.size(); i++) {
                CompoundTag carriage = carriages.getCompound(i);
                require(carriage.contains("Entity", Tag.TAG_COMPOUND), "Missing carriage entity: " + id);
                CompoundTag entity = carriage.getCompound("Entity");
                require(entity.hasUUID("UUID") && carriageIds.add(entity.getUUID("UUID")),
                        "Missing or duplicate carriage UUID: " + id);
                require(entity.hasUUID("TrainId") && id.equals(entity.getUUID("TrainId")),
                        "Carriage belongs to a different train: " + id);
                require(entity.contains("CarriageIndex", Tag.TAG_ANY_NUMERIC) && entity.getInt("CarriageIndex") == i,
                        "Invalid carriage index: " + id);
            }
        }
    }

    private static ListTag list(CompoundTag tag, String name, int elementType) throws IOException {
        require(tag.contains(name, Tag.TAG_LIST), "Missing list: " + name);
        ListTag list = (ListTag) tag.get(name);
        require(list.isEmpty() || list.getElementType() == elementType, "Wrong list element type: " + name);
        return list;
    }

    private static void require(boolean condition, String reason) throws IOException {
        if (!condition) throw new IOException(reason);
    }
}
