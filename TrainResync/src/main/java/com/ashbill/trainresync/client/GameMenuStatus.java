package com.ashbill.trainresync.client;

import com.cinemamod.mcef.MCEF;
import com.ashbill.trainresync.config.TrainResyncConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Client-side status pings leased by the visible game-entry panel. */
@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GameMenuStatus {
    private static final List<Node> NODES = List.of(
            new Node("mc1", 25591), new Node("mc5", 25591),
            new Node("mc2", 25590), new Node("mc6", 25590));
    private static volatile long lastRead = Long.MIN_VALUE;
    private static ScheduledExecutorService probes;
    private static TrainResyncConfig.GameMenu settings = TrainResyncConfig.GameMenu.DEFAULT;

    private GameMenuStatus() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded("mcef") || !ModList.get().isLoaded("fancymenu")) return;
        event.enqueueWork(() -> {
            try {
                settings = TrainResyncConfig.load(TrainResyncConfig.file(FMLPaths.CONFIGDIR.get(), "trainresync.json")).gameMenu();
            } catch (java.io.IOException failure) {
                com.mojang.logging.LogUtils.getLogger().warn("Using default game menu timings", failure);
            }
            Bridge.install();
        });
    }

    private static boolean active() {
        return lastRead != Long.MIN_VALUE && Util.getMillis() - lastRead < settings.inactiveTimeoutMs()
                && Minecraft.getInstance().level == null;
    }

    // Keep optional MCEF linkage out of setup when the mod is absent.
    private static final class Bridge {
        private static void install() {
            if (MCEF.isInitialized()) register();
            else MCEF.scheduleForInit(ok -> { if (ok) register(); });
        }

        private static void register() {
            probes = Executors.newScheduledThreadPool(4, task -> {
                Thread thread = new Thread(task, "Niijima menu ping");
                thread.setDaemon(true);
                return thread;
            });
            for (Node node : NODES) probes.execute(node::probe);
            var config = new CefMessageRouter.CefMessageRouterConfig("niijimaServerStatus", "niijimaCancelServerStatus");
            MCEF.getClient().getHandle().addMessageRouter(CefMessageRouter.create(config, new CefMessageRouterHandlerAdapter() {
                @Override
                public boolean onQuery(CefBrowser browser, CefFrame frame, long id, String request,
                                       boolean persistent, CefQueryCallback callback) {
                    if (!frame.isMain() || !trusted(frame.getURL(), "click".equals(request))) return false;
                    if ("click".equals(request)) {
                        Minecraft client = Minecraft.getInstance();
                        client.execute(() -> client.getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)));
                        callback.success("{}");
                        return true;
                    }
                    if (!"read".equals(request)) return false;
                    lastRead = Util.getMillis();
                    JsonObject result = new JsonObject();
                    JsonObject nodes = new JsonObject();
                    for (Node node : NODES) {
                        JsonObject sample = node.snapshot;
                        boolean fresh = sample.has("sampledAt") && System.currentTimeMillis() - sample.get("sampledAt").getAsLong()
                                <= (long) settings.probeIntervalMs() + settings.probeTimeoutMs();
                        nodes.add(node.id, fresh ? sample : new JsonObject());
                    }
                    result.add("nodes", nodes);
                    JsonObject timings = new JsonObject();
                    timings.addProperty("probeIntervalMs", settings.probeIntervalMs());
                    timings.addProperty("probeTimeoutMs", settings.probeTimeoutMs());
                    timings.addProperty("inactiveTimeoutMs", settings.inactiveTimeoutMs());
                    result.add("gameMenu", timings);
                    callback.success(result.toString());
                    return true;
                }
            }));
        }
    }

    private static boolean trusted(String address, boolean click) {
        try {
            URI uri = URI.create(address);
            if ("https".equals(uri.getScheme()) && "mc1.ashbill.com".equals(uri.getHost()) && uri.getPort() == 25572)
                return (click && uri.getPath().startsWith("/v3/")) || List.of("/v3/", "/v3/index.html", "/index-preview.html").contains(uri.getPath());
            return "file".equals(uri.getScheme()) && uri.getPath() != null
                    && uri.getPath().endsWith("/config/fancymenu/assets/niijima/base.html");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static final class Node {
        private final String id;
        private final int port;
        private volatile JsonObject snapshot = new JsonObject();

        private Node(String id, int port) {
            this.id = id;
            this.port = port;
        }

        private void probe() {
            long started = Util.getMillis();
            try {
                if (!active()) return;
                ServerStatusPinger pinger = new ServerStatusPinger();
                ServerData data = new ServerData(id, id + ".ashbill.com:" + port, false);
                data.ping = -1;
                try {
                    pinger.pingServer(data, () -> {});
                    while (active() && Util.getMillis() - started < settings.probeTimeoutMs() && data.ping < 0) {
                        pinger.tick();
                        Thread.sleep(50);
                    }
                    JsonObject next = new JsonObject();
                    if (active() && data.ping >= 0 && Util.getMillis() - started < settings.probeTimeoutMs()) {
                        next.addProperty("ms", data.ping);
                        if (data.players != null) {
                            next.addProperty("players", data.players.online());
                            JsonArray names = new JsonArray();
                            data.players.sample().stream().limit(3).forEach(player -> names.add(player.getName()));
                            next.add("names", names);
                        }
                    }
                    next.addProperty("sampledAt", System.currentTimeMillis());
                    snapshot = next;
                } finally {
                    pinger.removeAll();
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (Exception failure) {
                snapshot = new JsonObject();
            } finally {
                if (!Thread.currentThread().isInterrupted())
                    probes.schedule(this::probe, Math.max(50, settings.probeIntervalMs() - (Util.getMillis() - started)), TimeUnit.MILLISECONDS);
            }
        }
    }
}
