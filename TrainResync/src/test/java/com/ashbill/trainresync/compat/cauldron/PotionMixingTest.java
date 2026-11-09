package com.ashbill.trainresync.compat.cauldron;

public final class PotionMixingTest {
    private static int checks;
    private static void expect(int before, boolean addsNew, int after, int limit, int reaction) {
        int value = new PotionMixingAccess.Mix(before, addsNew).effectsForReaction(after, limit);
        int actual = Integer.compare(value, limit); // -1 ordinary; 0 warning; 1 explosion
        if (actual != reaction) throw new AssertionError(before + " -> " + after + ", new=" + addsNew);
        checks++;
    }

    public static void main(String[] args) {
        for (int limit : new int[]{4, 8, 12}) {
            expect(limit - 1, false, limit - 1, limit, -1);
            expect(limit, false, limit, limit, 0); // effect-free dilution at threshold
            expect(limit + 1, false, limit + 1, limit, 0); // repeat subset / strength / duration
            expect(limit - 1, true, limit, limit, 0);
            expect(limit - 1, true, limit + 3, limit, 0); // first crossing still only warns
            expect(limit, true, limit + 1, limit, 1);
            expect(limit + 1, true, limit + 2, limit, 1);
            expect(limit, false, limit - 1, limit, -1); // dilution can round a short effect away
        }
        System.out.println("Potion mixing reactions: " + checks + " checks passed");
    }
}
