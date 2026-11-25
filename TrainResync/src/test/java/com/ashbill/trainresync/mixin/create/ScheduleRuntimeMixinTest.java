package com.ashbill.trainresync.mixin.create;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class ScheduleRuntimeMixinTest extends ScheduleRuntimeMixin {
    public static void main(String[] args) {
        var runtime = new ScheduleRuntimeMixinTest();
        runtime.conditionProgress = new ArrayList<>(List.of(3, 0));
        var waiting = new CompoundTag();
        waiting.putInt("Time", 147);
        var secondGroup = new CompoundTag();
        secondGroup.putInt("StatusVersion", 5);
        var stale = new CompoundTag();
        stale.putInt("Time", 999);
        var saved = new ListTag();
        saved.add(waiting);
        saved.add(secondGroup);
        saved.add(stale);

        ListTag active = runtime.trainresync$readCurrentStop(saved);
        if (active.size() != 2 || active.get(0) != waiting || active.get(1) != secondGroup
                || !runtime.conditionProgress.equals(List.of(3, 0)) || saved.size() != 3)
            throw new AssertionError("Legacy save lost active state or kept the unused tail");
        if (runtime.trainresync$readCurrentStop(active) != active)
            throw new AssertionError("A second load changed a normalized save");
        var shortSave = new ListTag();
        shortSave.add(waiting);
        ListTag padded = runtime.trainresync$readCurrentStop(shortSave);
        if (padded.size() != 2 || padded.get(0) != waiting || !padded.getCompound(1).isEmpty())
            throw new AssertionError("Missing wait group was not initialized");

        runtime.conditionProgress.clear();
        if (!runtime.trainresync$readCurrentStop(saved).isEmpty())
            throw new AssertionError("An inactive schedule retained wait groups");
    }
}
