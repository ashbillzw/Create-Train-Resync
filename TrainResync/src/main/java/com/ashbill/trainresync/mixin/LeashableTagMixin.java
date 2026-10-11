package com.ashbill.trainresync.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// Include vanilla overrides so they cannot bypass the additive tag rule.
@Mixin({Mob.class, TamableAnimal.class, AmbientCreature.class, Dolphin.class,
        Panda.class, Squid.class, Turtle.class, WaterAnimal.class, Wolf.class,
        Axolotl.class, Zoglin.class, Hoglin.class, AbstractVillager.class})
public abstract class LeashableTagMixin {
    @Unique
    private static final TagKey<EntityType<?>> trainresync$CAN_BE_LEASHED =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("trainresync", "can_be_leashed"));

    @ModifyReturnValue(
            method = "canBeLeashed(Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("RETURN")
    )
    private boolean trainresync$allowTaggedMobs(boolean original) {
        Mob mob = (Mob) (Object) this;
        return original || (!mob.isLeashed() && mob.getType().is(trainresync$CAN_BE_LEASHED));
    }
}
