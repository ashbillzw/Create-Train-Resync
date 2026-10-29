package com.ashbill.trainresync.mixin.create;

import com.ashbill.trainresync.safety.AtomicRailwayFile;
import com.ashbill.trainresync.safety.RailwayDataGuard;
import com.simibubi.create.content.trains.RailwaySavedData;
import java.io.File;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.saveddata.SavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SavedData.class)
public abstract class RailwaySavedDataMixin {
    @Shadow public abstract boolean isDirty();
    @Shadow public abstract void setDirty(boolean dirty);
    @Shadow public abstract CompoundTag save(CompoundTag tag);

    @Inject(method = "save(Ljava/io/File;)V", at = @At("HEAD"), cancellable = true)
    private void trainresync$saveAtomically(File file, CallbackInfo ci) {
        if (!((Object) this instanceof RailwaySavedData) || !file.getName().equals("create_tracks.dat")) return;
        ci.cancel();
        Path target = file.toPath();
        // A shutdown triggered by a load/save failure must not overwrite the evidence.
        if (RailwayDataGuard.isBlocked(target) || !isDirty()) return;
        try {
            AtomicRailwayFile.write(target, output -> {
                CompoundTag root = new CompoundTag();
                root.put("data", save(new CompoundTag()));
                NbtUtils.addCurrentDataVersion(root);
                NbtIo.writeCompressed(root, output);
            });
            setDirty(false);
        } catch (Exception failure) {
            // Keep dirty and the temporary file. Never continue running with a silently failed save.
            RailwayDataGuard.savingFailed(target, failure);
        }
    }
}
