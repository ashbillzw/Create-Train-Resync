package com.ashbill.trainresync.compat.cauldron;

/** Exact millibucket accounting, shared by the runtime bridge and regression tests. */
public final class MilkAmounts {
    public static final int CAPACITY = 1000;
    private MilkAmounts() {}
    public static int comparatorSignal(int amount, int capacity) {
        return capacity <= 0 ? 0 : (int)((long)Math.max(0, Math.min(capacity, amount)) * 15 / capacity);
    }
    public static int displayLayers(int amount, int capacity) {
        if (capacity <= 0) return 0;
        int bounded = Math.max(0, Math.min(capacity, amount));
        return (int)(((long)bounded * 4 + capacity - 1) / capacity);
    }
    public static int fill(int stored, int requested) {
        return Math.min(Math.max(0, requested), CAPACITY - clamp(stored));
    }
    public static int drain(int stored, int requested) {
        return Math.min(clamp(stored), Math.max(0, requested));
    }
    public static int clamp(int amount) { return Math.max(0, Math.min(CAPACITY, amount)); }
    public static int layers(int amount) { return (clamp(amount) + 249) / 250; }
    public static int reconcileLayers(int amount, int layersNow) {
        return clamp(amount + (layersNow - layers(amount)) * 250);
    }
    public static boolean wholeContainerFits(int stored, int amount, boolean filling) {
        return amount > 0 && (filling ? fill(stored, amount) : drain(stored, amount)) == amount;
    }
}
