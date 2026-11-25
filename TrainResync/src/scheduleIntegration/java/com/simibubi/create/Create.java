package com.simibubi.create;

import java.util.Random;
import net.minecraft.resources.ResourceLocation;

/** Only the namespace and RNG used by Create's real schedule and train constructors. */
public final class Create {
    public static final Random RANDOM = new Random(0);

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation("create", path);
    }
}
