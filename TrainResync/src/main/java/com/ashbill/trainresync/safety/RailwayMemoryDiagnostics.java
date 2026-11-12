package com.ashbill.trainresync.safety;

import com.mojang.logging.LogUtils;
import java.lang.management.ManagementFactory;
import org.slf4j.Logger;

public final class RailwayMemoryDiagnostics {
    private static final Logger LOG = LogUtils.getLogger();
    public record Sample(long time, long heap, long gcCount, long gcMillis) {}

    public static Sample sample() {
        long count = 0, millis = 0;
        for (var collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            count += Math.max(0, collector.getCollectionCount());
            millis += Math.max(0, collector.getCollectionTime());
        }
        return new Sample(System.nanoTime(), ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(), count, millis);
    }

    public static void report(Sample before, boolean committed) {
        var after = sample();
        LOG.info("Railway DAT committed={} elapsedMs={} heapMiB={}->{} gcCount={} gcMs={}", committed,
                (after.time - before.time) / 1_000_000, before.heap >> 20, after.heap >> 20,
                after.gcCount - before.gcCount, after.gcMillis - before.gcMillis);
    }
}
