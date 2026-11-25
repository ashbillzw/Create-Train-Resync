package com.ashbill.trainresync.mixin.create;

import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScheduleRuntime.class, remap = false)
public abstract class ScheduleRuntimeMixin {
    @Shadow List<Integer> conditionProgress;
    @Shadow List<CompoundTag> conditionContext;

    @Inject(method = "destinationReached", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V"))
    void trainresync$clearPreviousStop(CallbackInfo ci) {
        conditionContext.clear();
    }

    @ModifyArg(method = "read", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/foundation/utility/NBTHelper;iterateCompoundList(Lnet/minecraft/nbt/ListTag;Ljava/util/function/Consumer;)V"), index = 0)
    ListTag trainresync$readCurrentStop(ListTag saved) {
        int slots = conditionProgress.size();
        if (saved.size() == slots) return saved;
        // Create indexes the active groups from zero. Old saves can have millions of trailing tags.
        ListTag active = new ListTag();
        for (int i = 0; i < slots; i++) {
            active.add(i < saved.size() ? saved.getCompound(i) : new CompoundTag());
        }
        return active;
    }
}
