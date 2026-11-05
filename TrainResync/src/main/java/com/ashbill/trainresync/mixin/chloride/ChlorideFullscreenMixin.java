package com.ashbill.trainresync.mixin.chloride;

import me.srrapero720.chloride.ChlorideConfig;
import me.srrapero720.chloride.impl.Borderless;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Persist both settings after Chloride finishes applying a window-mode change. */
@Mixin(value = Borderless.class, remap = false)
public abstract class ChlorideFullscreenMixin {
    @Inject(method = "setFullScreenMode", at = @At("RETURN"))
    private static void trainresync$saveFullscreenMode(CallbackInfo ci) {
        ChlorideConfig.write();
        Minecraft.getInstance().options.save();
    }
}
