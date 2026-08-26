package com.ashbill.trainresync.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;

import net.minecraftforge.fml.loading.LoadingModList;


public class TrainResyncMixinPlugin implements IMixinConfigPlugin {
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) {}

    @Override public void onLoad(String mixinPackage) {
        MixinExtrasBootstrap.init();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        LoadingModList lm = LoadingModList.get();

        if (mixinClassName.contains("ContraptionSeatMixin") ||
            mixinClassName.contains("TrainStatusMixin")
        )
            return lm.getModFileById("create") != null;


        if (mixinClassName.contains("PantographBlockEntityAccessor") ||
            mixinClassName.contains("CarriageMixin") ||
            mixinClassName.contains("ContraptionPantographMixin") ||
            mixinClassName.contains("PantographBlockEntityMixin") ||
            mixinClassName.contains("TrainMixin")
        )
            return lm.getModFileById("pantographsandwires") != null;


        if (mixinClassName.contains("AbstractMaidContainerMixin") ||
            mixinClassName.contains("EntityMaidMixin") ||
            mixinClassName.contains("HandleBackpackEventMixin") ||
            mixinClassName.contains("MaidConfigMixin") ||
            mixinClassName.contains("OpenMaidGuiMessageMixin") ||
            mixinClassName.contains("SetAttackListMessageMixin")
        )
            return lm.getModFileById("touhou_little_maid") != null;


        if (mixinClassName.contains("HeadTailLightMovementBehaviourMixin"))
            return lm.getModFileById("ctl") != null;

        if (mixinClassName.contains("LiquidEngineUpgradeMixin"))
            return lm.getModFileById("simpleplanes") != null && lm.getModFileById("supplementaries") != null;

        if (mixinClassName.contains("EntityMixin"))
            return true;

        return true;
    }
}
