package com.ashbill.trainresync.mixin.create;

import com.ashbill.trainresync.safety.RailwayDataGuard;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DimensionDataStorage.class)
public abstract class RailwayDataStorageMixin {
    @Shadow private File getDataFile(String name) { throw new AssertionError(); }
    @Shadow public abstract CompoundTag readTagFromDisk(String name, int version) throws IOException;

    @WrapMethod(method = "readSavedData")
    private <T extends SavedData> T trainresync$loadStrictly(Function<CompoundTag, T> loader,
            String name, Operation<T> original) {
        if (!RailwayDataGuard.DATA_NAME.equals(name)) return original.call(loader, name);
        Path target = getDataFile(name).toPath();
        if (!RailwayDataGuard.isEnabled(target)) return original.call(loader, name);
        try {
            RailwayDataGuard.checkLoad(target);
            // Only preflight-approved new worlds may take vanilla's initialization path.
            if (!Files.exists(target)) return original.call(loader, name);
            RailwayDataGuard.validateGzip(target);
            CompoundTag root = readTagFromDisk(name,
                    SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            RailwayDataGuard.validateRoot(root);
            T result = loader.apply(root.getCompound("data"));
            if (result == null) throw new IOException("Railway decoder returned no saved data");
            return result;
        } catch (Exception failure) {
            // Outside vanilla's readSavedData catch: do not return null and create empty railway data.
            throw RailwayDataGuard.loadingFailed(target, failure);
        }
    }
}
