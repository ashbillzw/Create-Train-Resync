package com.ashbill.trainresync.mixin.create;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Navigation.class, remap = false)
public interface NavigationDebugAccessor {
    @Accessor("currentPath") List<Couple<TrackNode>> trainresync$path();
    @Accessor("waitingForChainedGroups") Map<UUID, Pair<SignalBoundary, Boolean>> trainresync$waitingGroups();
}
