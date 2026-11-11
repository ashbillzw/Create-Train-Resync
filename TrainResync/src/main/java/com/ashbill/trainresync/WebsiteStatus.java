package com.ashbill.trainresync;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.DEDICATED_SERVER)
public final class WebsiteStatus {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final long INTERVAL = TimeUnit.SECONDS.toNanos(5);
    private record Snapshot(long sampledAt, boolean running, int onlinePlayers, float mspt, long totalLogins) {}

    private final Path file;
    private long totalLogins;
    private long lastSample = System.nanoTime() - INTERVAL;
    private CompletableFuture<Void> writing = CompletableFuture.completedFuture(null);

    private WebsiteStatus(Path file, long seed) {
        this.file = file;
        totalLogins = seed;
    }

    @SubscribeEvent
    public static void start(ServerStartingEvent event) {
        String name = System.getProperty("trainresync.statusFile");
        if (name == null) return;
        try {
            Path file = Path.of(name);
            long seed = JsonParser.parseString(Files.readString(file)).getAsJsonObject()
                    .get("totalLogins").getAsBigDecimal().longValueExact();
            if (seed < 0) throw new IllegalArgumentException("negative totalLogins");
            MinecraftForge.EVENT_BUS.register(new WebsiteStatus(file, seed));
        } catch (IOException | RuntimeException ex) {
            LOG.error("Website status disabled: could not load login counter from {}", name, ex);
        }
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        totalLogins++;
        lastSample = System.nanoTime() - INTERVAL;
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !writing.isDone()) return;
        long now = System.nanoTime();
        if (now - lastSample < INTERVAL) return;
        lastSample = now;
        MinecraftServer server = event.getServer();
        var snapshot = new Snapshot(System.currentTimeMillis(), true, server.getPlayerCount(),
                server.getAverageTickTime(), totalLogins);
        writing = CompletableFuture.runAsync(() -> write(snapshot));
    }

    @SubscribeEvent
    public void stop(ServerStoppedEvent event) {
        MinecraftForge.EVENT_BUS.unregister(this);
        writing.join();
        write(new Snapshot(System.currentTimeMillis(), false, 0, 0, totalLogins));
    }

    private void write(Snapshot snapshot) {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(snapshot));
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            LOG.error("Could not publish website status to {}", file, ex);
        }
    }
}
