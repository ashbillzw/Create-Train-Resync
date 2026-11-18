package com.ashbill.trainresync.sponsorship;

import com.ashbill.trainresync.config.TrainResyncConfig;
import java.util.function.BooleanSupplier;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod.EventBusSubscriber(modid = "trainresync")
public final class PlayerSponsorship {
    private static final int HOUR = 20 * 60 * 60;
    private static final int NEWCOMER_TICKS = HOUR / 2;
    private static final String KEY = "trainresync:sponsorship";
    // Only online newcomers appear here. A null sponsor means they're waiting.
    private static final Map<MinecraftServer, Map<UUID, UUID>> newcomers = new HashMap<>();

    @SubscribeEvent
    public static void start(ServerAboutToStartEvent event) {
        try {
            if (TrainResyncConfig.load(TrainResyncConfig.file(FMLPaths.CONFIGDIR.get(), "trainresync.json")).playerSponsorship()) {
                newcomers.put(event.getServer(), new HashMap<>());
            }
        } catch (IOException error) {
            LogUtils.getLogger().warn("Cannot load player sponsorship configuration", error);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        newcomers.remove(event.getServer());
    }

    private static CompoundTag data(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY);
    }

    @SubscribeEvent
    public static void join(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var online = newcomers.get(player.server);
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag record = persisted.getCompound(KEY);
        persisted.put(KEY, record);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        SponsorshipTime clock = SponsorshipTime.get(player.server);
        CompoundTag time = clock.onLogin(player.getStringUUID());
        if (online == null || time.getLong("playedTicks") >= NEWCOMER_TICKS) {
            // Restore players who were forced into spectator mode under the previous rules.
            if (record.contains("previousMode")) {
                if (player.isSpectator()) player.setGameMode(GameType.byId(record.getInt("previousMode")));
                record.remove("previousMode");
            }
            return;
        }
        online.put(player.getUUID(), null);
        waitForSponsor(player, record);
    }

    private static void waitForSponsor(ServerPlayer player, CompoundTag record) {
        if (!player.isSpectator()) {
            record.putInt("previousMode", player.gameMode.getGameModeForPlayer().getId());
            player.setGameMode(GameType.SPECTATOR);
        }
    }

    @SubscribeEvent
    public static void leave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var online = newcomers.get(player.server);
        if (online == null) return;
        UUID id = player.getUUID();
        online.remove(id);
        for (var entry : online.entrySet()) {
            if (!id.equals(entry.getValue())) continue;
            ServerPlayer newcomer = player.server.getPlayerList().getPlayer(entry.getKey());
            entry.setValue(null);
            if (newcomer != null) waitForSponsor(newcomer, data(newcomer));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        var online = newcomers.get(player.server);
        UUID id = player.getUUID();
        if (online != null && online.containsKey(id) && (online.get(id) == null || player.isSpectator())) return;
        SponsorshipTime clock = SponsorshipTime.get(player.server);
        CompoundTag record = clock.find(player.getStringUUID());
        if (record == null) return; // Only count players initialized by a real login.
        long ticks = record.getLong("playedTicks") + 1;
        record.putLong("playedTicks", ticks);
        clock.setDirty();
        if (online != null && ticks == NEWCOMER_TICKS) online.remove(id);
    }

    /** Vanilla's permission-checked /gamemode command, including the server console. */
    public static boolean adminGameMode(ServerPlayer player, GameType mode, BooleanSupplier change) {
        if (mode != GameType.SURVIVAL && mode != GameType.CREATIVE) return change.getAsBoolean();
        SponsorshipTime clock = SponsorshipTime.get(player.server);
        UUID id = player.getUUID();
        CompoundTag record = clock.find(player.getStringUUID());
        if (record == null || record.getLong("playedTicks") >= NEWCOMER_TICKS) return change.getAsBoolean();
        var online = newcomers.get(player.server);
        boolean restricted = online != null && online.containsKey(id);
        UUID sponsor = restricted ? online.remove(id) : null;
        boolean changed = false;
        try {
            changed = change.getAsBoolean();
            if (changed) {
                record.putLong("playedTicks", record.getLong("playedTicks") + HOUR);
                clock.setDirty();
                data(player).remove("previousMode");
            }
            return changed;
        } finally {
            if (!changed && restricted) online.put(id, sponsor);
        }
    }

    @SubscribeEvent
    public static void changeMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getNewGameMode() == GameType.SPECTATOR || !(event.getEntity() instanceof ServerPlayer player)) return;
        var online = newcomers.get(player.server);
        if (online != null && online.containsKey(player.getUUID()) && online.get(player.getUUID()) == null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        for (String name : new String[]{"老带新", "担保", "danbao", "niijima-sponsor"}) {
            event.getDispatcher().register(Commands.literal(name)
                    .requires(source -> newcomers.containsKey(source.getServer()))
                    .then(Commands.argument("player", EntityArgument.player()).executes(context ->
                            sponsor(context.getSource(), context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
    }

    private static int sponsor(CommandSourceStack source, ServerPlayer veteran, ServerPlayer target) {
        CompoundTag veteranData = SponsorshipTime.get(veteran.server).find(veteran.getStringUUID());
        if (veteranData == null || veteranData.getLong("playedTicks") <= HOUR ||
                !Instant.ofEpochMilli(veteranData.getLong("firstSeen")).atZone(ZoneId.systemDefault())
                        .plusMonths(1).toInstant().isBefore(Instant.now())) {
            source.sendFailure(Component.literal("需要由老玩家担保"));
            return 0;
        }
        var online = newcomers.get(veteran.server);
        if (!online.containsKey(target.getUUID())) {
            source.sendFailure(Component.literal("该玩家无需担保"));
            return 0;
        }
        target.teleportTo(veteran.serverLevel(), veteran.getX(), veteran.getY(), veteran.getZ(), veteran.getYRot(), veteran.getXRot());
        if (target.level() != veteran.level() || target.distanceToSqr(veteran) > 0.0001) return 0;
        UUID previous = online.put(target.getUUID(), veteran.getUUID());
        if (target.gameMode.getGameModeForPlayer() != GameType.SURVIVAL && !target.setGameMode(GameType.SURVIVAL)) {
            online.put(target.getUUID(), previous);
            return 0;
        }
        data(target).remove("previousMode");
        source.sendSuccess(() -> Component.literal("已担保 ").append(target.getName()), false);
        return 1;
    }
}
