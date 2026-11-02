package com.ashbill.trainresync.compat.cauldron;
public interface MilkAccess {
    /** -1 for an unmanaged tank; otherwise exact mB (milkshakes use 250 per serving). */
    int trainresync$milkAmount();
    void trainresync$setMilkAmount(int amount);
    String trainresync$content();
    int trainresync$capacity();
    void trainresync$setContent(String content, int amount, int capacity);
    void trainresync$setMappedFluid(net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack fluid, int amount);
}
