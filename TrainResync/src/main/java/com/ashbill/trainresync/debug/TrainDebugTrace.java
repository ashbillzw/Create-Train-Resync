package com.ashbill.trainresync.debug;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.mojang.logging.LogUtils;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.foundation.utility.Couple;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = "trainresync")
public final class TrainDebugTrace {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static TrainDebugTrace active;
    private final CommandSourceStack source;
    private final Train train;
    private final Logger logger;
    private final long deadline;
    private long nextSample;
    private final Set<String> scoutMismatches = new HashSet<>();

    private TrainDebugTrace(CommandSourceStack source, Train train, int seconds, Logger logger) {
        this.source = source;
        this.train = train;
        this.logger = logger;
        long now = System.nanoTime();
        deadline = now + TimeUnit.SECONDS.toNanos(seconds);
        nextSample = now + TimeUnit.SECONDS.toNanos(1);
    }

    static int start(CommandSourceStack source, Train train, int seconds, boolean fileOnly) {
        Logger logger = fileOnly ? TrainDebugFileLog.logger() : LOGGER;
        if (active != null) finish("切换目标");
        active = new TrainDebugTrace(source, train, seconds, logger);
        TrainDebugCommands.reply(source, "跟踪：" + train.name.getString() + "（" + seconds + " 秒"
                + (fileOnly ? "；文件：" + TrainDebugFileLog.PATH : "") + "）");
        TrainDebugReport.snapshot(train, active::log);
        return 1;
    }

    static int status(CommandSourceStack source) {
        if (active == null) {
            TrainDebugCommands.reply(source, "未在跟踪");
            return 0;
        }
        long seconds = Math.max(0, (active.deadline - System.nanoTime() + 999_999_999L) / 1_000_000_000L);
        TrainDebugCommands.reply(source, "正在跟踪：" + active.train.name.getString() + "（剩余 " + seconds + " 秒"
                + (active.logger != LOGGER ? "；文件" : "") + "）");
        return 1;
    }

    static int stop(CommandSourceStack source) {
        if (active == null) return status(source);
        var owner = active.source;
        String message = finish("手动");
        if (source.getEntity() != owner.getEntity()) TrainDebugCommands.reply(source, message);
        return 1;
    }

    private static String finish(String reason) {
        TrainDebugTrace trace = active;
        active = null;
        TrainDebugReport.snapshot(trace.train, trace::log);
        String message = "跟踪结束：" + trace.train.name.getString() + "（" + reason + "）";
        trace.log(message);
        TrainDebugCommands.reply(trace.source, message);
        return message;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active == null) return;
        if (Create.RAILWAYS.trains.get(active.train.id) != active.train) {
            finish("列车移除");
            return;
        }
        long now = System.nanoTime();
        if (now >= active.deadline) finish("到期");
        else if (now >= active.nextSample) {
            active.log("STATE tick=" + active.source.getServer().getTickCount() + " " + TrainDebugReport.state(active.train));
            active.nextSample = now + TimeUnit.SECONDS.toNanos(1);
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        if (active != null) finish("服务器停止");
    }

    public static boolean watching(Train train) {
        return active != null && active.train == train && System.nanoTime() < active.deadline;
    }

    public static String navigationState(Navigation nav) {
        return "destination=" + TrainDebugReport.destination(nav) + " path="
                + TrainDebugReport.access(nav).trainresync$path().stream().map(TrainDebugReport::edge).toList();
    }

    public static void navigation(Train train, String event, String before, double result) {
        active.scoutMismatches.clear();
        active.log("NAV tick=" + active.source.getServer().getTickCount() + " train=" + train.id + " event=" + event
                + " before={" + before + "} after={" + navigationState(train.navigation) + "} result=" + result);
    }

    public static void fork(Train train, boolean scout, Couple<TrackNode> planned, boolean available,
            List<Map.Entry<TrackNode, TrackEdge>> options, Map.Entry<TrackNode, TrackEdge> selected, int consumed) {
        String reason = planned == null ? "empty_path" : available ? "planned" : "planned_edge_unavailable";
        if (scout && !reason.equals("planned_edge_unavailable")) return;
        String decision = "train=" + train.id + " graph=" + (train.graph == null ? "none" : train.graph.id)
                + " at=" + TrainDebugReport.node(selected.getValue().node1) + " planned=" + TrainDebugReport.edge(planned)
                + " options=" + options.stream().map(e -> TrainDebugReport.node(e.getKey())).toList()
                + " selected=" + TrainDebugReport.node(selected.getKey()) + " reason=" + reason + " consumed=" + consumed;
        if (scout && !active.scoutMismatches.add(decision)) return;
        active.log((scout ? "SCOUT_MISMATCH" : "FORK") + " tick=" + active.source.getServer().getTickCount() + " " + decision);
    }

    private void log(String line) {
        logger.info("[TrainDebug] {}", line);
    }
}
