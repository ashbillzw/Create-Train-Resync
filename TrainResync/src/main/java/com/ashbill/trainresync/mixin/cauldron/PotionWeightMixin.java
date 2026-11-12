package com.ashbill.trainresync.mixin.cauldron;

import com.ashbill.trainresync.compat.cauldron.MilkAccess;
import com.ashbill.trainresync.compat.cauldron.CauldronGlow;
import net.mehvahdjukaar.moonlight.api.fluids.BuiltInSoftFluids;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidTank;
import net.mehvahdjukaar.moonlight.api.util.PotionNBTHelper;
import net.minecraft.nbt.CompoundTag;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile$1", remap = false)
public abstract class PotionWeightMixin extends SoftFluidTank {
    protected PotionWeightMixin(int capacity) { super(capacity); }
    @Unique private MilkAccess trainresync$owner;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void trainresync$owner(LiquidCauldronBlockTile tile, int capacity, CallbackInfo ci) {
        trainresync$owner = (MilkAccess)tile;
    }

    @Override
    public int addFluid(SoftFluidStack input, boolean simulate) {
        var base = CauldronGlow.unpack(input);
        boolean glowing = base != input;
        if (base.is(BuiltInSoftFluids.WATER) && getFluid().is(BuiltInSoftFluids.POTION)) {
            // Present water to the native mixer as a regular bottle with zero effects.
            var tag = new CompoundTag();
            tag.putString("Potion", "minecraft:water");
            PotionNBTHelper.Type.REGULAR.applyToTag(tag);
            base = SoftFluidStack.of(BuiltInSoftFluids.POTION, input.getCount(), tag);
        }
        int accepted;
        if (base.is(BuiltInSoftFluids.WATER) && (isEmpty() || getFluid().is(BuiltInSoftFluids.WATER))) {
            accepted = Math.min(getSpace(), input.getCount());
            if (!simulate && accepted > 0) {
                setFluid(SoftFluidStack.of(BuiltInSoftFluids.WATER, getFluid().getCount() + accepted));
                base.shrink(accepted);
            }
        } else {
            accepted = super.addFluid(base, simulate);
        }
        if (!simulate && accepted > 0) {
            if (base != input) input.shrink(accepted);
            if (glowing) trainresync$owner.trainresync$markGlowing();
        }
        return accepted;
    }

    @WrapOperation(method = "addFluidOntoExisting", at = @At(value = "INVOKE", target = "Lnet/mehvahdjukaar/amendments/common/LiquidMixer;mixPotions(Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;Lnet/mehvahdjukaar/moonlight/api/fluids/SoftFluidStack;)V"))
    private void trainresync$weightByVolume(SoftFluidStack stored, SoftFluidStack input, Operation<Void> original) {
        int amount = trainresync$owner.trainresync$milkAmount();
        var weighted = stored.copyWithCount(amount >= 0 ? amount : stored.getCount() * 250);
        original.call(weighted, input.copyWithCount(input.getCount() * 250));
        stored.setTag(weighted.getTag());
    }
}
