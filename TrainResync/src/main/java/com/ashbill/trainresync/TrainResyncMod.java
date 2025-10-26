package com.ashbill.trainresync;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;


@Mod("trainresync")
public class TrainResyncMod {
    public TrainResyncMod() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientInit::init);
    }
}
