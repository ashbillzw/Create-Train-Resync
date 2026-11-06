package com.ashbill.trainresync.mixin.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskSnow;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = TaskSnow.class, remap = false)
public abstract class TaskSnowMixin {
    @Unique
    private static final ResourceLocation trainresync$ash = new ResourceLocation("supplementaries", "ash");

    @ModifyReturnValue(method = "canHarvest", at = @At("RETURN"))
    private boolean trainresync$allowAsh(boolean original, EntityMaid maid, BlockPos pos, BlockState state) {
        return original || trainresync$ash.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }
}
