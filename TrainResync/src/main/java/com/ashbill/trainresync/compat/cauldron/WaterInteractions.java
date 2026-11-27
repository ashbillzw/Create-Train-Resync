package com.ashbill.trainresync.compat.cauldron;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.item.DyeItem;
import net.mehvahdjukaar.amendments.events.behaviors.CauldronDyeWater;

/** Reuses registered water interactions without replacing the live block with a vanilla cauldron. */
public final class WaterInteractions {
    private static boolean washesItem(CauldronInteraction callback) {
        return callback == CauldronInteraction.SHULKER_BOX || callback == CauldronInteraction.BANNER
                || callback == CauldronInteraction.DYED_ITEM;
    }

    public static void playWashSound(CauldronInteraction callback, InteractionResult result, Level world, BlockPos pos) {
        // Supplementaries shares the filled-map washing callback with supported atlases.
        if (!world.isClientSide && result.consumesAction()
                && (washesItem(callback) || callback == CauldronInteraction.WATER.get(Items.FILLED_MAP)))
            world.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.65F, 1.0F);
    }

    public static InteractionResult interact(PlayerInteractEvent.RightClickBlock event, MilkAccess tank) {
        if (tank == null || !tank.trainresync$content().equals("water")) return InteractionResult.PASS;
        var callback = CauldronInteraction.WATER.get(event.getItemStack().getItem());
        // SHULKER_BOX, BANNER, DYED_ITEM consume one 250 mB portion; other conversions require a full pot.
        boolean cleaning = washesItem(callback);
        if (cleaning ? tank.trainresync$milkAmount() < 250 : tank.trainresync$milkAmount() != 1000) return InteractionResult.PASS;
        var view = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, cleaning ? 1 : 3);
        if (event.getItemStack().getItem() instanceof DyeItem) {
            var dye = new CauldronDyeWater();
            if (dye.isEnabled()) return dye.tryPerformingAction(view, event.getPos(), event.getLevel(),
                    event.getEntity(), event.getHand(), event.getItemStack(), event.getHitVec());
        }
        if (callback == null) return InteractionResult.PASS;
        var result = callback.interact(view, event.getLevel(), event.getPos(), event.getEntity(),
                event.getHand(), event.getItemStack());
        playWashSound(callback, result, event.getLevel(), event.getPos());
        return result;
    }
}
