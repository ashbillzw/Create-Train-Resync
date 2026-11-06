package com.ashbill.trainresync.meituan;

import com.google.gson.Gson;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import java.nio.charset.StandardCharsets;

final class DeliveryData extends SavedData {
    private static final Gson GSON = new Gson();
    DeliveryBook book = new DeliveryBook();
    RuntimeException readError;
    private CompoundTag unreadable;

    static DeliveryData load(CompoundTag tag) {
        DeliveryData data = new DeliveryData();
        try {
            if (tag.getInt("version") != 1 || !tag.contains("orders")) throw new IllegalArgumentException("Unknown Meituan save format");
            data.book = GSON.fromJson(new String(tag.getByteArray("orders"), StandardCharsets.UTF_8), DeliveryBook.class);
            if (data.book == null || data.book.requests == null || data.book.couriers == null || data.book.lastBroadcastDay == null)
                throw new IllegalArgumentException("Incomplete Meituan save");
        } catch (RuntimeException error) {
            // Do not turn an unreadable save into a new empty order pool.
            data.readError = error;
            data.unreadable = tag.copy();
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        if (unreadable != null) return unreadable.copy();
        tag.putInt("version", 1);
        tag.putByteArray("orders", GSON.toJson(book).getBytes(StandardCharsets.UTF_8));
        return tag;
    }
}
