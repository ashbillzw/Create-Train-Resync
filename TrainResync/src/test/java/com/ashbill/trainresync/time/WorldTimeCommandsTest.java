package com.ashbill.trainresync.time;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class WorldTimeCommandsTest {
    public static void main(String[] args) throws Exception {
        var commands = new CommandDispatcher<CommandSourceStack>();
        TimeCommand.register(commands);
        var originalAdd = commands.findNode(java.util.List.of("time", "add", "time")).getCommand();
        var originalQuery = commands.findNode(java.util.List.of("time", "query", "day")).getCommand();
        WorldTimeCommands.register(commands);
        var op = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 2, "test",
                Component.literal("test"), null, null);
        for (String value : new String[]{"6000", "1d", "0.5s", "1488435594"}) {
            try {
                commands.execute("time set " + value, op);
                throw new AssertionError("Numeric set succeeded");
            } catch (CommandSyntaxException expected) {
                if (!expected.getRawMessage().getString().startsWith("【AshBill】请勿直接用数值设时。"))
                    throw expected;
            }
        }
        String force = "time force-set 13907527830";
        var ctx = commands.parse(force, op).getContext().build(force);
        if (LongArgumentType.getLong(ctx, "ticks") != 13907527830L) throw new AssertionError("Lost precision");
        if (commands.parse(force, op.withPermission(0)).getContext().getCommand() != null)
            throw new AssertionError("Missing permission check");
        if (commands.findNode(java.util.List.of("time", "add", "time")).getCommand() != originalAdd ||
                commands.findNode(java.util.List.of("time", "query", "day")).getCommand() != originalQuery)
            throw new AssertionError("Unrelated time subcommand changed");
        for (int phase : new int[]{1000, 6000, 13000, 18000}) {
            long target = 24_000L * 579_480 + phase;
            if (WorldTimeCommands.nextTime(target - 1, phase) != target ||
                    WorldTimeCommands.nextTime(target, phase) != target + 24000 ||
                    WorldTimeCommands.nextTime(target + 1, phase) != target + 24000)
                throw new AssertionError("Wrong next occurrence for " + phase);
        }
    }
}
