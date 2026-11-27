package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.HeatSources;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.xkmc.fruitsdelight.content.cauldrons.FDCauldronInteraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FDCauldronInteraction.class, remap = false)
public abstract class FDCauldronInteractionMixin {
    @Shadow public abstract ItemStack result();
    @Shadow public abstract SoundEvent sound();

    // The upstream serving sound runs on the server only, so include the player who served it.
    @ModifyArg(method = "m_175710_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_6269_(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"), index = 0)
    private Player trainresync$includeCook(Player excluded) {
        return null;
    }

    @Inject(method = "m_175710_", at = @At("RETURN"), cancellable = true)
    private void trainresync$swingOnSuccess(BlockState state, Level world, BlockPos pos, Player player,
            InteractionHand hand, ItemStack held, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() == InteractionResult.CONSUME) {
            if (!world.isClientSide && result().isEmpty())
                world.playSound(null, pos, sound(), SoundSource.BLOCKS, 0.65F, 1.0F);
            cir.setReturnValue(InteractionResult.sidedSuccess(world.isClientSide));
        }
    }

    @Redirect(method = "perform", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState trainresync$activeHeat(Level world, BlockPos pos) {
        return HeatSources.view(world, pos);
    }
}
