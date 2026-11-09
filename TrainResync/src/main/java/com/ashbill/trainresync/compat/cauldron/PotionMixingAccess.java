package com.ashbill.trainresync.compat.cauldron;

public interface PotionMixingAccess {
    Mix trainresync$takePotionMix();

    record Mix(int effectsBefore, boolean addsNewEffect) {
        // Amendments warns at the limit and explodes above it.
        public int effectsForReaction(int effectsAfter, int limit) {
            return addsNewEffect && effectsBefore >= limit ? limit + 1 : Math.min(effectsAfter, limit);
        }
    }
}
