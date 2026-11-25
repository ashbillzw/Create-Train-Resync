package com.ashbill.trainresync.test;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

final class ScheduleEntryRegression {
    static void run() {
        var train = new Train(new UUID(0, 1), null, null, List.of(), List.of(), false);
        var runtime = train.runtime;
        var schedule = new Schedule();
        var stop = new ScheduleEntry();
        stop.instruction = new DestinationInstruction();
        stop.conditions.add(List.of(new ScheduledDelay(), new ScheduledDelay()));
        stop.conditions.add(List.of(new ScheduledDelay()));
        schedule.entries.add(stop);
        runtime.setSchedule(schedule, false);
        runtime.state = ScheduleRuntime.State.IN_TRANSIT;
        runtime.destinationReached();

        var legacy = runtime.write();
        var contexts = legacy.getList("ConditionContext", Tag.TAG_COMPOUND);
        if (contexts.size() != 2) throw new AssertionError("First arrival did not initialize two groups");
        contexts.getCompound(0).putInt("Time", 147);
        contexts.getCompound(1).putInt("StatusVersion", 5);
        legacy.putIntArray("ConditionProgress", new int[]{1, 0});
        var expected = legacy.copy();
        contexts.add(new CompoundTag());
        runtime.read(legacy);
        if (!runtime.write().equals(expected))
            throw new AssertionError("Legacy read failed to trim the unused context or preserve active waits");

        runtime.destinationReached();
        if (!runtime.write().equals(expected))
            throw new AssertionError("Duplicate arrival cleared or changed an active wait");
        runtime.read(runtime.write());
        if (!runtime.write().equals(expected))
            throw new AssertionError("Saving and reloading changed the current stop");

        for (int i = 0; i < 2; i++) {
            runtime.state = ScheduleRuntime.State.IN_TRANSIT;
            runtime.destinationReached();
            var arrived = runtime.write();
            var groups = arrived.getList("ConditionContext", Tag.TAG_COMPOUND);
            if (!Arrays.equals(arrived.getIntArray("ConditionProgress"), new int[]{0, 0})
                    || groups.size() != 2 || !groups.getCompound(0).isEmpty() || !groups.getCompound(1).isEmpty())
                throw new AssertionError("A new stop inherited old waits or accumulated extra slots");
        }
    }
}
