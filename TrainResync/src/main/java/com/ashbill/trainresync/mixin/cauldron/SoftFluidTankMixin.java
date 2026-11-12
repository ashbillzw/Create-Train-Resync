package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.PotionMixingAccess;
import com.ashbill.trainresync.compat.cauldron.CauldronGlow;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.fluids.FluidContainerList;
import net.mehvahdjukaar.moonlight.api.util.PotionNBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidTank;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SoftFluidTank.class, remap = false)
public abstract class SoftFluidTankMixin implements PotionMixingAccess {
    @Shadow public abstract SoftFluidStack getFluid();
    @Unique private PotionMixingAccess.Mix trainresync$pendingMix;

    // Moonlight maps plain water from every potion bottle to WATER, losing the bottle type.
    @ModifyExpressionValue(method = "drainItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ZZ)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;fromItem(Lnet/minecraft/world/item/ItemStack;)Lcom/mojang/datafixers/util/Pair;"))
    private Pair<SoftFluidStack, FluidContainerList.Category> trainresync$dilutionBottleType(
            Pair<SoftFluidStack, FluidContainerList.Category> mapped, ItemStack held, Level world, BlockPos pos) {
        if (mapped == null || !mapped.getFirst().is(BuiltInSoftFluids.WATER) || !getFluid().is(BuiltInSoftFluids.POTION)
                || pos == null || !(world.getBlockEntity(pos) instanceof LiquidCauldronBlockTile tile)
                || tile.getSoftFluidTank() != (Object)this) return mapped;
        var type = PotionNBTHelper.getPotionType(held.getItem());
        if (type == null || type == PotionNBTHelper.Type.REGULAR) return mapped;
        var tag = new CompoundTag();
        tag.putString("Potion", "minecraft:water");
        type.applyToTag(tag);
        return Pair.of(SoftFluidStack.of(BuiltInSoftFluids.POTION, mapped.getFirst().getCount(), tag), mapped.getSecond());
    }

    @Override public PotionMixingAccess.Mix trainresync$takePotionMix() {
        var previous = trainresync$pendingMix;
        trainresync$pendingMix = null;
        return previous;
    }

    @WrapMethod(method = "fillItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ZZ)Lnet/minecraft/world/InteractionResultHolder;")
    private InteractionResultHolder<ItemStack> trainresync$glowingBottle(ItemStack empty, Level world, BlockPos pos,
            boolean simulate, boolean sound, Operation<InteractionResultHolder<ItemStack>> original) {
        // Capture before the native fill consumes the last portion or clears the cauldron.
        boolean glowing = pos != null && world.getBlockEntity(pos) instanceof LiquidCauldronBlockTile tile
                && tile.getSoftFluidTank() == (Object)this && tile.isGlowing()
                && (getFluid().is(BuiltInSoftFluids.POTION) || getFluid().is(BuiltInSoftFluids.WATER));
        var result = original.call(empty, world, pos, simulate, sound);
        if (glowing && result.getResult().consumesAction()) CauldronGlow.bottle(result.getObject());
        return result;
    }

    @WrapMethod(method = "addFluid(Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;Z)I")
    private int trainresync$rememberPotionMix(SoftFluidStack input, boolean simulate, Operation<Integer> original) {
        if (simulate) return original.call(input, true);
        var stored = getFluid();
        PotionMixingAccess.Mix previous = null;
        if (!stored.isEmpty() && stored.is(BuiltInSoftFluids.POTION) && input.is(BuiltInSoftFluids.POTION)) {
            var effects = PotionUtils.getAllEffects(stored.getTag());
            boolean addsNewEffect = PotionUtils.getAllEffects(input.getTag()).stream().anyMatch(added ->
                    added.getEffect() != MobEffects.GLOWING
                    && effects.stream().noneMatch(existing -> existing.getEffect() == added.getEffect()));
            previous = new PotionMixingAccess.Mix(CauldronGlow.mixingCount(effects), addsNewEffect);
        }
        int added = original.call(input, false);
        trainresync$pendingMix = added > 0 ? previous : null;
        return added;
    }
}
