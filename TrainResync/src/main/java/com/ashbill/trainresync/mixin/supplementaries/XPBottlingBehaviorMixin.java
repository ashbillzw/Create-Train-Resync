package com.ashbill.trainresync.mixin.supplementaries;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.events.overrides.XPBottlingBehavior", remap = false)
public abstract class XPBottlingBehaviorMixin {
    @ModifyArg(method = "tryPerformingAction", index = 1, at = @At(value = "INVOKE", target =
            "Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidTank;interactWithItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/item/ItemStack;"))
    private Level trainresync$useInteractionLevel(Level original, @Local(argsOnly = true) Level level) {
        return level;
    }
}
