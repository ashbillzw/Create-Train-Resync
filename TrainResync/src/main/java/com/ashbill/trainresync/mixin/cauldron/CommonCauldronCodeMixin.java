package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.CauldronCrafting;
import com.ashbill.trainresync.compat.cauldron.HeatSources;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ashbill.trainresync.compat.cauldron.CauldronArrows;
import com.ashbill.trainresync.compat.cauldron.MilkIntegration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.mehvahdjukaar.amendments.common.block.CommonCauldronCode;
import net.mehvahdjukaar.amendments.common.recipe.FluidAndItemCraftResult;
import net.mehvahdjukaar.amendments.common.recipe.FluidAndItemsCraftResult;
import net.mehvahdjukaar.amendments.common.recipe.CauldronRecipeUtils;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.mehvahdjukaar.amendments.events.behaviors.CauldronConversion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CommonCauldronCode.class, remap = false)
public abstract class CommonCauldronCodeMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private static void trainresync$checkContentContact(BlockState state, Level world, BlockPos pos, Entity entity,
            Supplier<Double> height, CallbackInfo ci) {
        if (!(entity instanceof ItemEntity) && !((AbstractCauldronBlockMixin)state.getBlock()).trainresync$inside(state, pos, entity))
            ci.cancel();
    }

    @Inject(method = "shouldBoil", at = @At("HEAD"), cancellable = true)
    private static void trainresync$activeHeat(BlockState source, SoftFluidStack fluid, LevelAccessor world,
            BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!HeatSources.active(source, world, pos)) cir.setReturnValue(false);
    }

    @Unique private static final String trainresync$lastCraft = "shader_switch_debug:last_cauldron_craft";
    @Unique private static final ThreadLocal<List<ItemEntity>> trainresync$inputs = new ThreadLocal<>();
    @Unique private static final ThreadLocal<BlockPos> trainresync$position = new ThreadLocal<>();
    @Unique private static final ThreadLocal<Integer> trainresync$arrowRemaining = new ThreadLocal<>();

    @Unique private static boolean trainresync$cooling(Entity entity, Level world) {
        var data = entity.getPersistentData();
        return data.contains(trainresync$lastCraft) && world.getGameTime() - data.getLong(trainresync$lastCraft) < 15;
    }

    @Inject(method = "attemptInWorldCrafting", at = @At("HEAD"), cancellable = true)
    private static void trainresync$cooldown(BlockState state, Level world, BlockPos pos, Entity entity,
            Supplier<Double> height, CallbackInfo ci) {
        trainresync$inputs.remove();
        trainresync$arrowRemaining.remove();
        trainresync$position.set(pos);
        if (trainresync$cooling(entity, world)) ci.cancel();
    }

    @Redirect(method = "attemptInWorldCrafting", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_45976_(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private static List<ItemEntity> trainresync$eligibleItems(Level world, Class<ItemEntity> type, AABB box) {
        var items = new ArrayList<>(world.getEntitiesOfClass(type, box));
        items.removeIf(item -> trainresync$cooling(item, world));
        trainresync$inputs.set(items);
        return items;
    }

    @Redirect(method = "attemptInWorldCrafting", at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/amendments/common/recipe/CauldronRecipeUtils;craft(Lnet/minecraft/world/level/Level;ZILnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;Ljava/util/List;)Lnet/mehvahdjukaar/amendments/common/recipe/FluidAndItemCraftResult;"))
    private static FluidAndItemCraftResult trainresync$craft(Level world, boolean boiling, int capacity,
            SoftFluidStack fluid, List<ItemStack> items) {
        int[] counts = items.stream().mapToInt(ItemStack::getCount).toArray();
        var result = trainresync$tryCraft(world, boiling, capacity, fluid, items);
        if (result == null && items.size() > 1) {
            for (var item : items) {
                if (item.isEmpty()) continue;
                result = trainresync$tryCraft(world, boiling, capacity, fluid, List.of(item));
                if (result != null) break;
            }
        }
        if (result != null) for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getCount() < counts[i])
                trainresync$inputs.get().get(i).getPersistentData().putLong(trainresync$lastCraft, world.getGameTime());
        }
        return result;
    }

    @Unique private static FluidAndItemCraftResult trainresync$tryCraft(Level world, boolean boiling, int capacity,
            SoftFluidStack fluid, List<ItemStack> items) {
        var tank = MilkIntegration.tank(world, trainresync$position.get());
        if (tank != null && tank.trainresync$milkAmount() < 250 && CauldronCrafting.canWash(fluid, items)) return null;
        FluidAndItemCraftResult result;
        if (CauldronArrows.matches(fluid, items)) {
            var arrows = CauldronArrows.craft(world, trainresync$position.get(), items, false);
            if (arrows == null) return null;
            trainresync$arrowRemaining.set(arrows.remaining());
            result = arrows.result();
        } else result = CauldronCrafting.craft(world, boiling, capacity, fluid, items);
        return result;
    }

    @Redirect(method = "attemptInWorldCrafting", at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/amendments/events/behaviors/CauldronConversion;setCorrectCauldronStateAndTile(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;)V"))
    private static void trainresync$keepExactWater(BlockState state, Level world, BlockPos pos, SoftFluidStack result) {
        if (trainresync$arrowRemaining.get() != null) {
            int amount = trainresync$arrowRemaining.get();
            trainresync$arrowRemaining.remove();
            CauldronArrows.setAmount(world, pos, amount);
            return;
        }
        var tank = MilkIntegration.tank(world, pos);
        if (tank != null && tank.trainresync$content().equals("water")
                && (result.isEmpty() || result.is(BuiltInSoftFluids.WATER))) {
            int oldPortions = (tank.trainresync$milkAmount() + 249) / 250;
            tank.trainresync$setMilkAmount(tank.trainresync$milkAmount() - (oldPortions - result.getCount()) * 250);
        } else CauldronConversion.setCorrectCauldronStateAndTile(state, world, pos, result);
    }

    @Redirect(method = "attemptPlayerCrafting", at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/amendments/events/behaviors/CauldronConversion;setCorrectCauldronStateAndTile(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;)V"))
    private static void trainresync$handArrowAmount(BlockState state, Level world, BlockPos pos, SoftFluidStack result) {
        if (trainresync$arrowRemaining.get() != null) trainresync$keepExactWater(state, world, pos, result);
        else CauldronConversion.setCorrectCauldronStateAndTile(state, world, pos, result);
    }

    @Redirect(method = "attemptPlayerCrafting", at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/amendments/common/recipe/CauldronRecipeUtils;craftMultiple(Lnet/minecraft/world/level/Level;ZILnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;Ljava/util/List;)Lnet/mehvahdjukaar/amendments/common/recipe/FluidAndItemsCraftResult;"))
    private static FluidAndItemsCraftResult trainresync$handArrows(Level world, boolean boiling, int capacity,
            SoftFluidStack fluid, List<ItemStack> items, BlockState state, Level callerWorld,
            BlockPos pos, Player player, InteractionHand hand, int callerCapacity, SoftFluidStack callerFluid) {
        trainresync$arrowRemaining.remove();
        if (!CauldronArrows.matches(fluid, items))
            return CauldronRecipeUtils.craftMultiple(world, boiling, capacity, fluid, items);
        var arrows = CauldronArrows.craft(world, pos, items, true);
        if (arrows == null) return null;
        trainresync$arrowRemaining.set(arrows.remaining());
        return FluidAndItemsCraftResult.of(List.of(arrows.result().craftedItem()), arrows.result().resultFluid());
    }

    @Redirect(method = "spawnResultItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean trainresync$stampResult(Level world, Entity result) {
        // Native crafting replaces the input entity; the replacement must retain the cooldown.
        result.getPersistentData().putLong(trainresync$lastCraft, world.getGameTime());
        return world.addFreshEntity(result);
    }

    @Inject(method = "attemptInWorldCrafting", at = @At("RETURN"))
    private static void trainresync$clearInputs(BlockState state, Level world, BlockPos pos, Entity entity,
            Supplier<Double> height, CallbackInfo ci) {
        trainresync$inputs.remove();
        trainresync$position.remove();
        trainresync$arrowRemaining.remove();
    }
}
