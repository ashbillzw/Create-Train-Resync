package com.ashbill.trainresync.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional IrisFlw bridge; no client or optional-mod types are linked on a dedicated server. */
public final class FlywheelShaderFormats {
    private FlywheelShaderFormats() {}

    public static void synchronize(Object holder) {
        try {
            boolean forceBlock = !(Boolean) Class.forName("top.leonx.irisflw.IrisFlw")
                    .getMethod("isUsingExtendedVertexFormat").invoke(null);
            Object shaded = builder(holder, "shadedBuilder");
            Object unshaded = builder(holder, "unshadedBuilder");
            // Resolve both compat setters before writing. Do not modify the vertex buffers/layouts.
            Method shadedSetter = shaded.getClass().getMethod("irisflw$setForceBlockFormat", boolean.class);
            Method unshadedSetter = unshaded.getClass().getMethod("irisflw$setForceBlockFormat", boolean.class);
            shadedSetter.invoke(shaded, forceBlock);
            unshadedSetter.invoke(unshaded, forceBlock);
        } catch (ReflectiveOperationException ex) {
            // Continuing after failure can feed plain vertices into an extended unsafe reader.
            throw new IllegalStateException("TrainResync: failed to synchronize Flywheel shader vertex format", ex);
        }
    }

    private static Object builder(Object holder, String name) throws ReflectiveOperationException {
        Field field = holder.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(holder);
    }
}
