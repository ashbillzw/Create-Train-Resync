package com.ashbill.trainresync.compat.cauldron;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.item.DyeItem;
import net.mehvahdjukaar.amendments.events.behaviors.CauldronDyeWater;

/** Reuses registered water interactions without replacing the live block with a vanilla cauldron. */
public final class WaterInteractions {
    public static InteractionResult interact(PlayerInteractEvent.RightClickBlock event, MilkAccess tank) {
        if (tank == null || !tank.trainresync$content().equals("water")) return InteractionResult.PASS;
        var callback = CauldronInteraction.WATER.get(event.getItemStack().getItem());
        // SHULKER_BOX, BANNER, DYED_ITEM consume one 250 mB portion; other conversions require a full pot.
        boolean cleaning = callback == CauldronInteraction.SHULKER_BOX || callback == CauldronInteraction.BANNER
                || callback == CauldronInteraction.DYED_ITEM;
        if (cleaning ? tank.trainresync$milkAmount() < 250 : tank.trainresync$milkAmount() != 1000) return InteractionResult.PASS;
        var view = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, cleaning ? 1 : 3);
        if (event.getItemStack().getItem() instanceof DyeItem) {
            var dye = new CauldronDyeWater();
            if (dye.isEnabled()) return dye.tryPerformingAction(view, event.getPos(), event.getLevel(),
                    event.getEntity(), event.getHand(), event.getItemStack(), event.getHitVec());
        }
        if (callback == null) return InteractionResult.PASS;
        return callback.interact(view, event.getLevel(), event.getPos(), event.getEntity(),
                event.getHand(), event.getItemStack());
    }
}
