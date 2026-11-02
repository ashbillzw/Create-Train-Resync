package com.ashbill.trainresync;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;


@Mod("trainresync")
public class TrainResyncMod {
    public TrainResyncMod() {
        if (com.ashbill.trainresync.compat.cauldron.CauldronCompatibility.available()) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(com.ashbill.trainresync.compat.cauldron.MilkIntegration.class);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(com.ashbill.trainresync.compat.cauldron.HeatSources.class);
        }
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientInit::init);
    }
}
