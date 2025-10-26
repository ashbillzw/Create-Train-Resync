package com.ashbill.trainresync.mixin;

import java.util.Set;
import java.util.HashSet;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3d;
import org.joml.Quaternionf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.DimensionPalette;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.paw.blockentity.PantographBlockEntity;

import com.ashbill.trainresync.mixin.accessor.PantographBlockEntityAccessor;
import com.ashbill.trainresync.mixin_interfaces.IElectricTrainContraption;
import com.ashbill.trainresync.mixin_interfaces.IElectricTrainCarriage;


@Mixin(value = Carriage.class, remap = false)
public abstract class CarriageMixin implements IElectricTrainCarriage {
    
    @Unique private final Quaternionf trainresync$initialRotation = new Quaternionf();

    @Unique private final Set<BlockPos> trainresync$pantographs = new HashSet<BlockPos>();

    @Shadow public abstract DimensionalCarriageEntity getDimensional(Level level);

    @Inject(method = "setContraption", at = @At("RETURN"))
    private void trainresync$findPantographs(Level level, CarriageContraption contraption, CallbackInfo ci, @Local DimensionalCarriageEntity dimensional) {
        trainresync$setInitialRotation(getCarriageRotation(dimensional));
        trainresync$setPantographs(((IElectricTrainContraption)contraption).trainresync$getPantographs());
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void trainresync$savePantographs(DimensionPalette dimensions, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        Vector3f euler = new Vector3f();
        trainresync$initialRotation.getEulerAnglesYXZ(euler);
        tag.putFloat("Trainresync$InitialRotation", euler.y);
        tag.put("Trainresync$Pantographs", NBTHelper.writeCompoundList(trainresync$pantographs, NbtUtils::writeBlockPos));
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void trainresync$loadPantographs(CompoundTag tag, TrackGraph graph, DimensionPalette dimensions, CallbackInfoReturnable<Carriage> cir) {
        Carriage carriage = cir.getReturnValue();
        ((IElectricTrainCarriage)carriage).trainresync$setInitialRotation(new Quaternionf().rotationYXZ(tag.getFloat("Trainresync$InitialRotation"), 0.0F, 0.0F));
        Set <BlockPos> pantographs = new HashSet<BlockPos>();
        NBTHelper.iterateCompoundList(tag.getList("Trainresync$Pantographs", Tag.TAG_COMPOUND), (c ->
            pantographs.add(NbtUtils.readBlockPos(c))
        ));
        ((IElectricTrainCarriage)carriage).trainresync$setPantographs(pantographs);
    }

    @Unique private static Quaternionf getCarriageRotation(DimensionalCarriageEntity dce) {
        Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), dce.rotationAnchors.get(false).subtract(dce.rotationAnchors.get(true)).toVector3f());

        Vector3f euler = new Vector3f();
        rotation.getEulerAnglesYXZ(euler);
        rotation.rotationYXZ​(euler.y, euler.x, 0.0F);

        return rotation;
    }

    @Override
    public void trainresync$setInitialRotation(Quaternionf initialRotation) {
        trainresync$initialRotation.set(initialRotation);
    }

    @Override
    public void trainresync$setPantographs(Set<BlockPos> pantographs) {
        trainresync$pantographs.clear();
        trainresync$pantographs.addAll(pantographs);
    }

    @Override
    public int trainresync$getPantographCount() {
        return trainresync$pantographs.size();
    }

    @Override
    public boolean trainresync$hasElectricity(Level level) {
        DimensionalCarriageEntity dce = getDimensional(level);

        for (BlockPos pantograph : trainresync$pantographs) {

            Quaternionf rotation = getCarriageRotation(dce);
            Vector3fc location = dce.positionAnchor.toVector3f().add(rotation.transform(trainresync$initialRotation.transformInverse​(Vec3.atLowerCornerOf(pantograph).toVector3f())));

            // DebugPrint.debugPrint("[TR] PANTO LOC: " + new Vector3f(location).toString(new DecimalFormat("0.000")));

            WireGraph wiregraph = WireGraphManager.get(level, WiresApi.PAW_CATENARY_WIRES);

            Vector3fc rightVector = rotation.transform(new Vector3f().set(PantographBlockEntity.BASE_RIGHT_VECTOR));
            for (BlockPos pos : PantographBlockEntityAccessor.trainresync$callFindIntersectingBlocks(
                new Vector3d(location).sub(rightVector),
                new Vector3d(location).add(rightVector),
                rotation.transform(new Vector3d(PantographBlockEntity.BASE_UP_VECTOR))
            )) if (!wiregraph.getCollisionsInBlock(pos).isEmpty()) return true;
        }
        return false;
    }
}
