package com.ashbill.trainresync.time;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod.EventBusSubscriber(modid = "trainresync")
public final class WorldTimeCommands {
    private static final int DAY = 24000;
    private static final SimpleCommandExceptionType NUMERIC_SET = new SimpleCommandExceptionType(Component.literal(
            "【AshBill】请勿直接用数值设时。调整昼夜请用 /time set <day|noon|night|midnight>；指定绝对时间请用 /time force-set。"));

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var set = literal("set").then(argument("time", TimeArgument.time()).executes(ctx -> { throw NUMERIC_SET.create(); }));
        addPreset(set, "day", "白天", 1000);
        addPreset(set, "noon", "正午", 6000);
        addPreset(set, "night", "夜晚", 13000);
        addPreset(set, "midnight", "午夜", 18000);
        dispatcher.register(literal("time").requires(s -> s.hasPermission(2)).then(set)
                .then(literal("force-set").then(argument("ticks", LongArgumentType.longArg(0, Long.MAX_VALUE - DAY))
                        .executes(ctx -> apply(ctx.getSource(), LongArgumentType.getLong(ctx, "ticks"), null)))));
    }

    private static void addPreset(LiteralArgumentBuilder<CommandSourceStack> set,
                                  String literalName, String label, int phase) {
        set.then(literal(literalName).executes(ctx -> {
            var source = ctx.getSource();
            return apply(source, nextTime(source.getLevel().getDayTime(), phase), label);
        }));
    }

    static long nextTime(long now, int phase) {
        int delta = Math.floorMod(phase - Math.floorMod(now, DAY), DAY);
        return Math.addExact(now, delta == 0 ? DAY : delta);
    }

    private static int apply(CommandSourceStack source, long ticks, String label) {
        for (ServerLevel level : source.getServer().getAllLevels()) level.setDayTime(ticks);
        String message = label == null ? "【AshBill】已将世界时间设为 " + ticks + " tick"
                : "【AshBill】时间已推进至下一个" + label;
        source.sendSuccess(() -> Component.literal(message + "（第 " + ticks / DAY + " 天）。"), true);
        return Math.floorMod(ticks, DAY);
    }
}
