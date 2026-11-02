package com.ashbill.trainresync.mixin.trainlights;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.adeptstack.ctl.blocks.lights.HeadTailLightBlockBase;
import net.adeptstack.ctl.blocks.lights.LightBlockBase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class LightBlockChangeMixin {
    @WrapOperation(method = "handleBlockChange", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/contraptions/Contraption;invalidateColliders()V"))
    private void trainresync$skipLightOnlyChange(Contraption contraption, Operation<Void> original,
            @Local(argsOnly = true) BlockState next, @Local StructureBlockInfo previous) {
        BlockState before = previous.state();
        if (next.getBlock() instanceof LightBlockBase && before.getBlock() == next.getBlock()) {
            boolean lightOnly = true;
            for (Property<?> property : next.getProperties()) {
                if (property != LightBlockBase.LIT && property != HeadTailLightBlockBase.LIGHT_MODE
                        && !before.getValue(property).equals(next.getValue(property))) {
                    lightOnly = false;
                    break;
                }
            }
            if (lightOnly) return;
        }
        original.call(contraption);
    }
}
