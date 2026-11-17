package com.ashbill.trainresync.config;

import java.nio.file.Files;

public final class GameMenuConfigTest {
    public static void main(String[] args) throws Exception {
        var file = Files.createTempFile("trainresync-menu", ".json");
        try {
            Files.writeString(file, "{\"meituan\":true}");
            var old = TrainResyncConfig.load(file);
            if (!old.meituan() || !old.gameMenu().equals(TrainResyncConfig.GameMenu.DEFAULT))
                throw new AssertionError("Existing configurations must keep their switches and use default menu timings");
            Files.writeString(file, "{\"gameMenu\":{\"probeIntervalMs\":600,\"probeTimeoutMs\":7000,\"inactiveTimeoutMs\":9000}}");
            if (!TrainResyncConfig.load(file).gameMenu().equals(new TrainResyncConfig.GameMenu(600, 7000, 9000)))
                throw new AssertionError("Configured timings must be loaded");
            Files.writeString(file, "{\"gameMenu\":{\"probeIntervalMs\":0,\"probeTimeoutMs\":1.5,\"inactiveTimeoutMs\":6000}}");
            if (!TrainResyncConfig.load(file).gameMenu().equals(new TrainResyncConfig.GameMenu(1000, 5000, 6000)))
                throw new AssertionError("Invalid values must fall back independently");
            Files.writeString(file, "{\"gameMenu\":false}");
            if (!TrainResyncConfig.load(file).gameMenu().equals(TrainResyncConfig.GameMenu.DEFAULT))
                throw new AssertionError("Invalid menu object must fall back");
            System.out.println("Game menu configuration: 4 cases passed");
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
