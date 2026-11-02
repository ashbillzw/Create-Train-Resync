package com.ashbill.trainresync.compat.jei;

import com.ashbill.trainresync.compat.cauldron.CauldronCompatibility;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class CauldronJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() {
        return new ResourceLocation("trainresync", "cauldron");
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        if (CauldronCompatibility.available()) CauldronDisplays.register(registration);
    }
}
