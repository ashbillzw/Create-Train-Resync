package com.ashbill.trainresync.safety;

import com.mojang.logging.LogUtils;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import org.slf4j.Logger;

public final class RailwayMemoryDiagnostics {
    private static final Logger LOG = LogUtils.getLogger();
    private static Recording recording;

    public static void start(Path logs) {
        try {
            var profile = new Recording(Configuration.getConfiguration("profile"));
            recording = profile;
            profile.setToDisk(true);
            profile.setMaxAge(Duration.ofHours(2));
            profile.setMaxSize(256L * 1024 * 1024);
            profile.setDestination(logs.resolve("railway-" + Instant.now().toEpochMilli() + ".jfr"));
            profile.setDumpOnExit(true);
            profile.start();
        } catch (Exception e) {
            close();
            LOG.warn("Could not start railway JFR", e);
        }
    }

    public static void close() {
        if (recording == null) return;
        try {
            if (recording.getState() == jdk.jfr.RecordingState.RUNNING) recording.stop();
        } finally {
            recording.close();
            recording = null;
        }
    }

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
