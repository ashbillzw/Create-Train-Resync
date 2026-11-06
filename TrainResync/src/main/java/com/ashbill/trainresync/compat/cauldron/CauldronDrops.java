package com.ashbill.trainresync.compat.cauldron;

import com.ashbill.trainresync.mixin.cauldron.AbstractCauldronBlockMixin;
import dev.xkmc.fruitsdelight.content.cauldrons.FDCauldronInteraction;
import dev.xkmc.fruitsdelight.mixin.AbstractCauldronBlockAccessor;
import net.mehvahdjukaar.amendments.common.LiquidMixer;
import net.mehvahdjukaar.amendments.common.block.DyeCauldronBlock;
import net.mehvahdjukaar.amendments.common.item.DyeBottleItem;
import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.mehvahdjukaar.amendments.configs.CommonConfigs;
import net.mehvahdjukaar.amendments.reg.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/** Material-only drop adapter: use registered actions without a simulated player. */
public final class CauldronDrops {
    public static final String LAST_CRAFT = "shader_switch_debug:last_cauldron_craft";
    public static boolean isOnCooldown(Entity entity, Level world) {
        var tag = entity.getPersistentData();
        return tag.contains(LAST_CRAFT) && world.getGameTime() - tag.getLong(LAST_CRAFT) < 15;
    }
    public static boolean accept(BlockState state, Level world, BlockPos pos, ItemEntity entity) {
        if (world.isClientSide || !CommonConfigs.CAULDRON_IN_WORLD_CRAFTING.get()
                || !(state.getBlock() instanceof AbstractCauldronBlock pot)
                || !((AbstractCauldronBlockMixin)pot).trainresync$inside(state, pos, entity)) return false;
        // Skip our crafting only; retain native lava damage and other entity contact behavior.
        if (isOnCooldown(entity, world)) return false;
        var held = entity.getItem();
        var access = MilkIntegration.tank(world, pos);
        ItemStack output = MilkConversions.apply(world, pos, held);
        if (output == null && held.getItem() instanceof DyeItem dye && CommonConfigs.DYE_WATER.get()) {
            if (access != null && access.trainresync$content().equals("water") && access.trainresync$milkAmount() == 1000) {
                if (!world.setBlock(pos, ModRegistry.DYE_CAULDRON.get().defaultBlockState()
                        .setValue(DyeCauldronBlock.LEVEL, 3), 3)) return false;
                var tile = (LiquidCauldronBlockTile)world.getBlockEntity(pos);
                tile.getSoftFluidTank().setFluid(DyeBottleItem.toFluidStack(dye.getDyeColor(), 3));
                tile.setChanged();
                DyeCauldronBlock.playDyeSoundAndConsume(state, pos, world, null, ItemStack.EMPTY);
                output = ItemStack.EMPTY;
            } else if (state.getBlock() instanceof DyeCauldronBlock
                    && world.getBlockEntity(pos) instanceof LiquidCauldronBlockTile tile) {
                var fluid = tile.getSoftFluidTank().getFluid();
                var preview = fluid.copyWithCount(Math.min(2, fluid.getCount()));
                LiquidMixer.mixDye(preview, DyeBottleItem.toFluidStack(dye.getDyeColor(), 1));
                preview.setCount(fluid.getCount());
                if (preview.isFluidEqual(fluid)) return true;
                // Native interactWithItem owns the dye mixing and sound rules.
                var result = tile.getSoftFluidTank().interactWithItem(held.copyWithCount(1), world, pos, false);
                if (result != null) { tile.setChanged(); output = result; }
            }
        }
        if (output == null && !(held.getItem() instanceof BucketItem)
                && !held.is(Items.GLASS_BOTTLE)
                && !held.is(Items.BOWL)) {
            var view = state;
            var interactions = ((AbstractCauldronBlockAccessor)pot).getInteractions();
            if (access != null && access.trainresync$content().equals("water") && access.trainresync$milkAmount() == 1000) {
                view = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
                interactions = CauldronInteraction.WATER;
            }
            if (interactions.get(held.getItem()) instanceof FDCauldronInteraction recipe) {
                var next = recipe.action().perform(view);
                if (next == null || next == view) return false;
                if (recipe.perform(view, world, pos, held)) {
                    var remainder = held.getCraftingRemainingItem();
                    if (!remainder.isEmpty()) spawn(world, pos, remainder);
                    output = recipe.result().copy();
                }
            }
        }
        if (output == null) return false;
        held.shrink(1);
        entity.getPersistentData().putLong(LAST_CRAFT, world.getGameTime());
        if (held.isEmpty()) entity.discard();
        if (!output.isEmpty()) spawn(world, pos, output);
        world.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        return true;
    }
    private static void spawn(Level world, BlockPos pos, ItemStack stack) {
        var item = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.25,
                pos.getZ() + 0.5, stack);
        item.setDefaultPickUpDelay();
        item.setDeltaMovement(world.random.nextGaussian() * 0.01, 0.375, world.random.nextGaussian() * 0.01);
        item.getPersistentData().putLong(LAST_CRAFT, world.getGameTime());
        world.addFreshEntity(item);
    }
}
