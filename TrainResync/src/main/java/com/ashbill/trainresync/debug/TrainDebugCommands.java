package com.ashbill.trainresync.debug;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod.EventBusSubscriber(modid = "trainresync")
public final class TrainDebugCommands {
    private static final DynamicCommandExceptionType NOT_FOUND = new DynamicCommandExceptionType(
            value -> Component.literal("列车不存在：" + value));
    private static final DynamicCommandExceptionType AMBIGUOUS = new DynamicCommandExceptionType(
            value -> Component.literal("列车重名，请使用 UUID：" + value));

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("trainresync").then(literal("debug")
                .requires(source -> source.hasPermission(2))
                .then(literal("list").executes(ctx -> list(ctx.getSource(), ""))
                        .then(argument("filter", StringArgumentType.greedyString())
                                .executes(ctx -> list(ctx.getSource(), StringArgumentType.getString(ctx, "filter")))))
                .then(query("info", TrainDebugReport::info))
                .then(query("path", TrainDebugReport::path))
                .then(query("signals", TrainDebugReport::signals))
                .then(literal("check").executes(ctx -> check(ctx.getSource(), trains()))
                        .then(trainArgument().executes(ctx -> check(ctx.getSource(),
                                List.of(find(StringArgumentType.getString(ctx, "train")))))))
                .then(literal("at").then(argument("dimension", DimensionArgument.dimension())
                        .then(argument("pos", BlockPosArgument.blockPos()).executes(ctx -> {
                            TrainDebugReport.at(DimensionArgument.getDimension(ctx, "dimension"),
                                    BlockPosArgument.getBlockPos(ctx, "pos"), output(ctx.getSource()));
                            return 1;
                        }))))
                .then(literal("trace").executes(ctx -> TrainDebugTrace.status(ctx.getSource()))
                        .then(trainArgument().executes(ctx -> trace(ctx, 60, false))
                                .then(argument("seconds", IntegerArgumentType.integer(1, 86400))
                                        .executes(ctx -> trace(ctx, IntegerArgumentType.getInteger(ctx, "seconds"), false))
                                        .then(literal("file").executes(ctx ->
                                                trace(ctx, IntegerArgumentType.getInteger(ctx, "seconds"), true))))))
                .then(literal("stop").executes(ctx -> TrainDebugTrace.stop(ctx.getSource())))));
    }

    private static int trace(CommandContext<CommandSourceStack> ctx, int seconds, boolean fileOnly)
            throws CommandSyntaxException {
        return TrainDebugTrace.start(ctx.getSource(), find(StringArgumentType.getString(ctx, "train")), seconds, fileOnly);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> query(String name,
            BiConsumer<Train, Consumer<String>> report) {
        return literal(name).then(trainArgument().executes(ctx -> {
            report.accept(find(StringArgumentType.getString(ctx, "train")), output(ctx.getSource()));
            return 1;
        }));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> trainArgument() {
        return argument("train", StringArgumentType.string()).suggests((ctx, builder) ->
                SharedSuggestionProvider.suggest(trains().stream().flatMap(train ->
                        Stream.of(train.id.toString(),
                                StringArgumentType.escapeIfRequired(train.name.getString()))), builder));
    }

    static Train find(String selector) throws CommandSyntaxException {
        for (Train train : trains())
            if (train.id.toString().equalsIgnoreCase(selector)) return train;
        List<Train> matches = trains().stream().filter(t -> t.name.getString().equals(selector)).toList();
        if (matches.isEmpty()) throw NOT_FOUND.create(selector);
        if (matches.size() > 1) throw AMBIGUOUS.create(matches.stream().map(t -> t.id.toString()).toList());
        return matches.get(0);
    }

    private static Collection<Train> trains() {
        return Create.RAILWAYS.trains.values();
    }

    private static int list(CommandSourceStack source, String filter) {
        List<Train> matches = trains().stream().filter(t -> t.name.getString().contains(filter))
                .sorted(Comparator.comparing(t -> t.name.getString())).toList();
        if (matches.isEmpty()) reply(source, "无匹配列车");
        for (Train train : matches)
            output(source).accept("LIST id=" + train.id + " name=" + TrainDebugReport.quote(train.name.getString())
                    + " state=" + train.runtime.state + " destination=" + TrainDebugReport.destination(train.navigation));
        return matches.size();
    }

    private static int check(CommandSourceStack source, Collection<Train> trains) {
        var out = output(source);
        int findings = 0;
        for (Train train : trains) findings += TrainDebugReport.check(train, out);
        out.accept("CHECK trains=" + trains.size() + " findings=" + findings);
        return findings;
    }

    static Consumer<String> output(CommandSourceStack source) {
        return line -> reply(source, "[TrainDebug] " + line);
    }

    static void reply(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}
