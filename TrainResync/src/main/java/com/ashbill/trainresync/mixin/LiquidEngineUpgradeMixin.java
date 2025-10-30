package com.ashbill.trainresync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;

import xyz.przemyk.simpleplanes.entities.PlaneEntity;
import xyz.przemyk.simpleplanes.upgrades.Upgrade;
import xyz.przemyk.simpleplanes.upgrades.UpgradeType;
import xyz.przemyk.simpleplanes.upgrades.engines.liquid.LiquidEngineUpgrade;


@Mixin(value = LiquidEngineUpgrade.class, remap = false)
public abstract class LiquidEngineUpgradeMixin extends Upgrade {
    @Shadow public ItemStackHandler itemStackHandler;
    @Shadow public FluidTank fluidTank;

    public LiquidEngineUpgradeMixin(UpgradeType type, PlaneEntity planeEntity) {
        super(type, planeEntity);
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getCapability(Lnet/minecraftforge/common/capabilities/Capability;)Lnet/minecraftforge/common/util/LazyOptional;",
            remap = true
        ),
        cancellable = true
    )
    private void trainresync$lumiseneIsAFuel(CallbackInfo ci, @Local ItemStack itemStack) {
        ResourceLocation item = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if (item != null && item.equals(new ResourceLocation("supplementaries", "lumisene_bucket"))) {
            Fluid lumisene = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("supplementaries", "lumisene"));
            if (lumisene != null && lumisene != Fluids.EMPTY) {
                int filled = fluidTank.fill(new FluidStack(lumisene, 1000), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    itemStackHandler.setStackInSlot(0, ItemStack.EMPTY);
                    itemStackHandler.setStackInSlot(1, new ItemStack(Items.BUCKET));
                    updateClient();
                    ci.cancel();
                }
            }                
        }
    }
}
