package com.ashbill.trainresync.mixin.create;

import java.util.List;
import java.util.Map;
import com.ashbill.trainresync.debug.TrainDebugTrace;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.foundation.utility.Couple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Navigation.class, remap = false)
public abstract class NavigationDebugMixin {
    @Shadow public Train train;
    @Shadow List<Couple<TrackNode>> currentPath;

    @WrapMethod(method = "navigateOptions")
    private Map.Entry<TrackNode, TrackEdge> trainresync$observeFork(List<Couple<TrackNode>> path,
            TrackGraph graph, List<Map.Entry<TrackNode, TrackEdge>> options,
            Operation<Map.Entry<TrackNode, TrackEdge>> original) {
        if (!TrainDebugTrace.watching(train)) return original.call(path, graph, options);
        Couple<TrackNode> planned = path.isEmpty() ? null : path.get(0);
        TrackEdge edge = planned == null ? null : graph.getConnection(planned);
        boolean available = edge != null && options.stream().anyMatch(option -> option.getValue() == edge);
        boolean scout = path != currentPath;
        int before = path.size();
        var selected = original.call(path, graph, options);
        TrainDebugTrace.fork(train, scout, planned, available, options, selected, before - path.size());
        return selected;
    }

    @WrapMethod(method = "startNavigation")
    private double trainresync$observeStart(DiscoveredPath path, Operation<Double> original) {
        if (!TrainDebugTrace.watching(train)) return original.call(path);
        String before = TrainDebugTrace.navigationState((Navigation) (Object) this);
        double result = original.call(path);
        TrainDebugTrace.navigation(train, result < 0 ? "failed" : "start", before, result);
        return result;
    }

    @WrapMethod(method = "cancelNavigation")
    private void trainresync$observeCancel(Operation<Void> original) {
        if (!TrainDebugTrace.watching(train)) {
            original.call();
            return;
        }
        String before = TrainDebugTrace.navigationState((Navigation) (Object) this);
        original.call();
        TrainDebugTrace.navigation(train, "cancel", before, 0);
    }
}
