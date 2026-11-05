package com.ashbill.trainresync.debug;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.ashbill.trainresync.mixin.create.NavigationDebugAccessor;
import com.ashbill.trainresync.mixin.create.TrackTargetDebugAccessor;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.utility.Couple;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public final class TrainDebugReport {
    static NavigationDebugAccessor access(Navigation navigation) {
        return (NavigationDebugAccessor) navigation;
    }

    public static String destination(Navigation navigation) {
        return navigation.destination == null ? "none"
                : navigation.destination.id + ":" + quote(navigation.destination.name);
    }

    static String quote(String value) {
        return new com.google.gson.JsonPrimitive(value).toString();
    }

    static String node(TrackNode node) {
        return node == null ? "none" : location(node.getLocation());
    }

    static String location(TrackNodeLocation location) {
        return location.dimension.location() + ":" + vector(location.getLocation());
    }

    static String vector(Vec3 position) {
        return String.format(java.util.Locale.ROOT, "%.3f,%.3f,%.3f", position.x, position.y, position.z);
    }

    public static String edge(Couple<TrackNode> edge) {
        return edge == null ? "none" : node(edge.getFirst()) + "->" + node(edge.getSecond());
    }

    static boolean hasEdge(TrackGraph graph, Couple<TrackNode> edge) {
        return graph != null && edge != null && edge.getFirst() != null && edge.getSecond() != null
                && graph.getConnectionsFrom(edge.getFirst()).containsKey(edge.getSecond());
    }

    private static TravellingPoint end(Train train, boolean front) {
        if (train.carriages.isEmpty()) return null;
        return front ? train.carriages.get(0).getLeadingPoint()
                : train.carriages.get(train.carriages.size() - 1).getTrailingPoint();
    }

    private static String point(Train train, TravellingPoint point) {
        if (point == null) return "none";
        String text = node(point.node1) + "->" + node(point.node2) + " offset=" + point.position;
        if (train.graph != null && point.node1 != null && point.node2 != null && point.edge != null)
            text += " pos=" + point.node1.getLocation().dimension.location() + ":" + vector(point.getPosition(train.graph));
        return text;
    }

    static String state(Train train) {
        Navigation nav = train.navigation;
        return "id=" + train.id + " graph=" + (train.graph == null ? "none" : train.graph.id)
                + " graphObject=" + Integer.toHexString(System.identityHashCode(train.graph))
                + " state=" + train.runtime.state + " paused=" + train.runtime.paused
                + " entry=" + train.runtime.currentEntry + " destination=" + destination(nav)
                + " remaining=" + nav.distanceToDestination + " speed=" + train.speed
                + " targetSpeed=" + train.targetSpeed + " fuelTicks=" + train.fuelTicks
                + " manual=" + train.manualTick + " derailed=" + train.derailed
                + " waitingChunks=" + train.carriageWaitingForChunks + " behind=" + nav.destinationBehindTrain
                + " pathSize=" + access(nav).trainresync$path().size()
                + " occupied=" + train.occupiedSignalBlocks.keySet() + " reserved=" + train.reservedSignalBlocks
                + " blocking=" + nav.waitingForSignal + " signalDistance=" + nav.distanceToSignal
                + " signalWaitTicks=" + nav.ticksWaitingForSignal + " front={" + point(train, end(train, true)) + "}";
    }

    public static void info(Train train, Consumer<String> out) {
        out.accept("INFO name=" + quote(train.name.getString()) + " " + state(train));
        out.accept("INFO carriages=" + train.carriages.size() + " backwards=" + train.currentlyBackwards
                + " throttle=" + train.throttle + " completed=" + train.runtime.completed
                + " hasSchedule=" + (train.runtime.getSchedule() != null)
                + " frontConductor=" + train.hasForwardConductor() + " backConductor=" + train.hasBackwardConductor()
                + " back={" + point(train, end(train, false)) + "}");
    }

    public static void path(Train train, Consumer<String> out) {
        List<Couple<TrackNode>> path = access(train.navigation).trainresync$path();
        out.accept("PATH train=" + train.id + " count=" + path.size() + " front={" + point(train, end(train, true))
                + "} back={" + point(train, end(train, false)) + "}");
        for (int i = 0; i < path.size(); i++)
            out.accept("PATH index=" + i + " edge=" + edge(path.get(i)) + " exists=" + hasEdge(train.graph, path.get(i)));
    }

    public static void signals(Train train, Consumer<String> out) {
        Navigation nav = train.navigation;
        var waiting = access(nav).trainresync$waitingGroups();
        out.accept("SIGNAL train=" + train.id + " occupied=" + train.occupiedSignalBlocks
                + " reserved=" + train.reservedSignalBlocks + " blocking=" + nav.waitingForSignal
                + " distance=" + nav.distanceToSignal + " waitTicks=" + nav.ticksWaitingForSignal
                + " chained=" + waiting.entrySet().stream().map(e -> e.getKey() + ":"
                        + e.getValue().getFirst().id + ":" + e.getValue().getSecond()).toList());
        var groups = new LinkedHashSet<>(train.occupiedSignalBlocks.keySet());
        groups.addAll(train.reservedSignalBlocks);
        groups.addAll(waiting.keySet());
        if (nav.waitingForSignal != null && train.graph != null) {
            SignalBoundary signal = train.graph.getPoint(EdgePointType.SIGNAL, nav.waitingForSignal.getFirst());
            if (signal != null) {
                boundary(signal, out);
                signal.groups.forEach(groups::add);
            } else out.accept("SIGNAL missing=" + nav.waitingForSignal.getFirst());
        }
        for (UUID group : groups) if (group != null) group(group, out);
    }

    private static void boundary(SignalBoundary signal, Consumer<String> out) {
        for (boolean side : new boolean[]{true, false})
            out.accept("SIGNAL id=" + signal.id + " side=" + side + " group=" + signal.groups.get(side)
                    + " type=" + signal.types.get(side) + " state=" + signal.cachedStates.get(side)
                    + " forcedRed=" + signal.isForcedRed(side) + " blocks=" + signal.blockEntities.get(side).keySet()
                    + " edge=" + signal.edgeLocation.stream().map(TrainDebugReport::location).toList());
    }

    private static void group(UUID id, Consumer<String> out) {
        var group = Create.RAILWAYS.signalEdgeGroups.get(id);
        if (group == null) {
            out.accept("SIGNAL group=" + id + " exists=false");
            return;
        }
        out.accept("SIGNAL group=" + id + " occupied=" + group.trains.stream().map(t -> t.id + ":" + quote(t.name.getString())).toList()
                + " reservedBySignal=" + (group.reserved == null ? "none" : group.reserved.id)
                + " conflicts=" + group.intersecting.values()
                + " resolvedConflicts=" + (group.intersectingResolved == null ? "unknown" : group.intersectingResolved.stream()
                        .map(g -> g.id + ":occupied=" + g.trains.stream().map(t -> t.id).toList()
                                + ":reservedBySignal=" + (g.reserved == null ? "none" : g.reserved.id)).toList()));
    }

    public static int check(Train train, Consumer<String> out) {
        var findings = new ArrayList<String>();
        TrackGraph graph = train.graph;
        if (graph == null) {
            if (!train.derailed) findings.add("kind=missing_graph");
        } else {
            if (Create.RAILWAYS.trackNetworks.get(graph.id) != graph) findings.add("kind=unregistered_graph detail=" + graph.id);
            List<Couple<TrackNode>> path = access(train.navigation).trainresync$path();
            for (int i = 0; i < path.size(); i++)
                if (!hasEdge(graph, path.get(i))) findings.add("kind=missing_path_edge detail=" + i + ":" + edge(path.get(i)));
            for (int i = 0; i < train.carriages.size(); i++)
                for (var bogey : train.carriages.get(i).bogeys)
                    if (bogey != null) for (var point : List.of(bogey.leading(), bogey.trailing()))
                        if (point.node1 == null || point.node2 == null
                                || graph.getConnectionsFrom(point.node1).get(point.node2) != point.edge
                                || point.edge == null)
                            findings.add("kind=invalid_bogey_edge detail=carriage:" + i);
            var destination = train.navigation.destination;
            if (destination != null && graph.getPoint(EdgePointType.STATION, destination.id) != destination)
                findings.add("kind=missing_destination detail=" + destination.id);
            var blocking = train.navigation.waitingForSignal;
            if (blocking != null && graph.getPoint(EdgePointType.SIGNAL, blocking.getFirst()) == null)
                findings.add("kind=missing_blocking_signal detail=" + blocking.getFirst());
        }
        var groups = new LinkedHashSet<>(train.occupiedSignalBlocks.keySet());
        groups.addAll(train.reservedSignalBlocks);
        groups.addAll(access(train.navigation).trainresync$waitingGroups().keySet());
        for (UUID id : groups)
            if (!Create.RAILWAYS.signalEdgeGroups.containsKey(id)) findings.add("kind=missing_signal_group detail=" + id);
        findings.forEach(line -> out.accept("FINDING train=" + train.id + " " + line));
        return findings.size();
    }

    public static void at(ServerLevel level, BlockPos pos, Consumer<String> out) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        out.accept("AT dimension=" + level.dimension().location() + " pos=" + pos.toShortString()
                + " loaded=" + (chunk != null) + " block=" + (chunk == null ? "unknown" : chunk.getBlockState(pos)));
        if (chunk != null) {
            var be = chunk.getBlockEntities().get(pos);
            if (be instanceof TrackBlockEntity track)
                out.accept("AT curves=" + track.getConnections().keySet());
            if (be instanceof SignalBlockEntity signal) binding(signal.edgePoint, EdgePointType.SIGNAL, out);
            if (be instanceof StationBlockEntity station) binding(station.edgePoint, EdgePointType.STATION, out);
        }
        for (TrackGraph graph : Create.RAILWAYS.trackNetworks.values())
            for (TrackNodeLocation location : graph.getNodes())
                if (location.dimension.equals(level.dimension()) && location.getLocation().distanceToSqr(Vec3.atCenterOf(pos)) <= 64) {
                    TrackNode node = graph.locateNode(location);
                    out.accept("AT graph=" + graph.id + " node=" + location(location) + " connections="
                            + graph.getConnectionsFrom(node).keySet().stream().map(TrainDebugReport::node).toList());
                }
    }

    private static void binding(TrackTargetingBehaviour<?> target, EdgePointType<?> type, Consumer<String> out) {
        UUID id = ((TrackTargetDebugAccessor) target).trainresync$targetId();
        var cached = target.getEdgePoint();
        out.accept("AT binding=" + id + " targetTrack=" + target.getGlobalPosition().toShortString()
                + " direction=" + target.getTargetDirection() + " curve=" + target.getTargetBezier()
                + " cached=" + (cached == null ? "none" : cached.id));
        for (TrackGraph graph : Create.RAILWAYS.trackNetworks.values()) {
            var point = graph.getPoint(type, id);
            if (point == null) continue;
            out.accept("AT bindingGraph=" + graph.id + " cachedMatches=" + (point == cached)
                    + " edge=" + point.edgeLocation.stream().map(TrainDebugReport::location).toList());
            if (point instanceof SignalBoundary signal) {
                boundary(signal, out);
                for (UUID group : signal.groups) if (group != null) group(group, out);
            }
        }
    }

    static void snapshot(Train train, Consumer<String> out) {
        info(train, out);
        path(train, out);
        signals(train, out);
        out.accept("CHECK trains=1 findings=" + check(train, out));
    }
}
