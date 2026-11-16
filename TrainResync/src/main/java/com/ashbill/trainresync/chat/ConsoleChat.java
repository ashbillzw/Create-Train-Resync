package com.ashbill.trainresync.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.DEDICATED_SERVER)
public final class ConsoleChat {
    private static final ArrayDeque<JsonObject> history = new ArrayDeque<>();
    private static String epoch;
    private static long latest;

    @SubscribeEvent
    public static void start(ServerStartingEvent event) {
        history.clear();
        latest = 0;
        epoch = UUID.randomUUID().toString();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void chat(ServerChatEvent event) {
        JsonObject message = new JsonObject();
        message.addProperty("id", ++latest);
        message.addProperty("time", System.currentTimeMillis());
        message.addProperty("uuid", event.getPlayer().getUUID().toString());
        message.addProperty("name", event.getPlayer().getGameProfile().getName());
        message.addProperty("text", event.getRawText());
        if (history.size() == 256) history.removeFirst();
        history.addLast(message);
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("niijima-chat")
                .requires(s -> s.source == s.getServer() || s.source instanceof RconConsoleSource)
                .then(literal("since").then(argument("cursor", LongArgumentType.longArg(0))
                        .executes(c -> page(c.getSource(), LongArgumentType.getLong(c, "cursor"), null))
                        .then(argument("epoch", StringArgumentType.word()).executes(c -> page(c.getSource(),
                                LongArgumentType.getLong(c, "cursor"), StringArgumentType.getString(c, "epoch")))))));
    }

    private static int page(CommandSourceStack source, long cursor, String previousEpoch) {
        boolean restarted = previousEpoch != null && !previousEpoch.equals(epoch);
        long oldest = history.isEmpty() ? latest + 1 : history.getFirst().get("id").getAsLong();
        long next = restarted ? 0 : Math.min(cursor, latest);
        boolean gap = next < oldest - 1 || restarted || cursor > latest;
        JsonArray messages = new JsonArray();
        int bytes = 0;
        for (JsonObject message : history) {
            long id = message.get("id").getAsLong();
            if (id <= next) continue;
            bytes += message.toString().getBytes(StandardCharsets.UTF_8).length + 1;
            if (bytes > 3200 && !messages.isEmpty()) break;
            messages.add(message);
            next = id;
        }
        JsonObject result = new JsonObject();
        result.addProperty("epoch", epoch);
        result.addProperty("gap", gap);
        result.addProperty("next", next);
        result.addProperty("latest", latest);
        result.add("messages", messages);
        source.sendSuccess(() -> Component.literal(result.toString()), false);
        return messages.size();
    }
}
