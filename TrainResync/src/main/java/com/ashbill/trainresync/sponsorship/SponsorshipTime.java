package com.ashbill.trainresync.sponsorship;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** One play clock per UUID for all dimensions of a world. */
final class SponsorshipTime extends SavedData {
    private final CompoundTag players;

    private SponsorshipTime(CompoundTag players) {
        this.players = players;
    }

    static SponsorshipTime get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                root -> new SponsorshipTime(root.getCompound("players")),
                () -> new SponsorshipTime(new CompoundTag()), "trainresync_sponsorship");
    }

    CompoundTag find(String uuid) {
        return players.contains(uuid) ? players.getCompound(uuid) : null;
    }

    CompoundTag onLogin(String uuid) {
        CompoundTag record = find(uuid);
        if (record == null) {
            record = new CompoundTag();
            record.putLong("firstSeen", System.currentTimeMillis());
            record.putLong("playedTicks", 0L);
            players.put(uuid, record);
            setDirty();
        }
        return record;
    }

    @Override public CompoundTag save(CompoundTag root) {
        root.put("players", players);
        return root;
    }
}
