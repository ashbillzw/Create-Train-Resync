package com.ashbill.trainresync.mixin.accessor;

import java.util.Set;
import org.joml.Vector3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.BlockPos;

import de.mrjulsen.paw.blockentity.PantographBlockEntity;


@Mixin(value = PantographBlockEntity.class, remap = false)
public interface PantographBlockEntityAccessor {

    @Invoker("findIntersectingBlocks")
    static Set<BlockPos> trainresync$callFindIntersectingBlocks(Vector3d a, Vector3d b, Vector3d v) {
        throw new AssertionError();
    }

    @Invoker("checkWireIntersection")
    static Vector3d trainresync$callCheckWireIntersection(Vector3d c, Vector3d d, Vector3d a, Vector3d b, Vector3d up) {
        throw new AssertionError();
    }

}
