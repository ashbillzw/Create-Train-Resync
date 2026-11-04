package com.ashbill.trainresync;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;


@Mod("trainresync")
public class TrainResyncMod {
    private static void addCauldronData(net.minecraftforge.event.AddPackFindersEvent event) {
        if (event.getPackType() != net.minecraft.server.packs.PackType.SERVER_DATA) return;
        var file = net.minecraftforge.fml.ModList.get().getModFileById("trainresync");
        var root = file.getFile().findResource("niijima_cauldron");
        event.addRepositorySource(sink -> {
            var pack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate("trainresync:cauldron",
                    net.minecraft.network.chat.Component.literal(file.getMods().get(0).getDisplayName()), true,
                    id -> new net.minecraft.server.packs.PathPackResources(id, root, true), event.getPackType(),
                    net.minecraft.server.packs.repository.Pack.Position.BOTTOM, net.minecraft.server.packs.repository.PackSource.BUILT_IN);
            if (pack != null) sink.accept(pack);
        });
    }

    public TrainResyncMod() {
        if (com.ashbill.trainresync.compat.cauldron.CauldronCompatibility.available()) {
            net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus()
                    .addListener(TrainResyncMod::addCauldronData);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(com.ashbill.trainresync.compat.cauldron.MilkIntegration.class);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(com.ashbill.trainresync.compat.cauldron.HeatSources.class);
        }
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientInit::init);
    }
}
