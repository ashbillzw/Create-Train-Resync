package com.ashbill.trainresync.mixin.create;

import java.util.UUID;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TrackTargetingBehaviour.class, remap = false)
public interface TrackTargetDebugAccessor {
    @Accessor("id") UUID trainresync$targetId();
}
