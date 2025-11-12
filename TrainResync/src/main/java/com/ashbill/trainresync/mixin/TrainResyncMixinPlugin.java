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
        if (mixinClassName.contains("ContraptionSeatMixin")) return true;
        if (mixinClassName.contains("LiquidEngineUpgradeMixin"))
            return lm.getModFileById("simpleplanes") != null && lm.getModFileById("supplementaries") != null;
        if (mixinClassName.contains("HeadTailLightMovementBehaviourMixin"))
            return lm.getModFileById("ctl") != null;
        return lm.getModFileById("pantographsandwires") != null;
    }
}
