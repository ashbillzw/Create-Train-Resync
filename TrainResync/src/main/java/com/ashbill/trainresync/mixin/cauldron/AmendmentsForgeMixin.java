package com.ashbill.trainresync.mixin.cauldron;

import net.mehvahdjukaar.amendments.forge.AmendmentsForge;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = AmendmentsForge.class, remap = false)
public abstract class AmendmentsForgeMixin {
    @ModifyArg(method = "onRegisterPOI", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;of([Ljava/lang/Object;)Ljava/util/stream/Stream;"), index = 0)
    private static Object[] trainresync$allCauldrons(Object[] original) {
        return ForgeRegistries.BLOCKS.getValues().stream()
                .filter(block -> block instanceof AbstractCauldronBlock).toArray();
    }
}
