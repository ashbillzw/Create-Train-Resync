package com.ashbill.trainresync.mixin.shaders;

import com.ashbill.trainresync.compat.FlywheelShaderFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Flywheel 0.6: synchronize reused builders before either begin selects a vertex layout. */
@Pseudo
@Mixin(targets = "com.jozufozu.flywheel.core.model.ModelUtil$ThreadLocalObjects", remap = false)
public abstract class FlywheelShaderReloadMixin {
    @Inject(method = "begin()V", at = @At("HEAD"), remap = false, require = 1, allow = 1)
    private void trainresync$synchronizeShaderFormat(CallbackInfo ci) {
        FlywheelShaderFormats.synchronize(this);
    }
}
