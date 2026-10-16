package com.ashbill.trainresync.mixin;

import java.util.List;
import java.util.Locale;
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
        LoadingModList mods = LoadingModList.get();
        String prefix = "com.ashbill.trainresync.mixin.";

        if (mixinClassName.startsWith(prefix + "vanilla.")) return true;
        if (mixinClassName.startsWith(prefix + "create.")) return hasMods(mods, "create");
        if (mixinClassName.startsWith(prefix + "pantographs.")) return hasMods(mods, "pantographsandwires");
        if (mixinClassName.startsWith(prefix + "maid.")) return hasMods(mods, "touhou_little_maid");
        if (mixinClassName.startsWith(prefix + "trainlights.")) return hasMods(mods, "ctl");
        if (mixinClassName.startsWith(prefix + "simpleplanes.")) return hasMods(mods, "simpleplanes", "supplementaries");
        if (mixinClassName.startsWith(prefix + "shaders.")) return hasMods(mods, "oculus", "irisflw", "flywheel");
        if (mixinClassName.startsWith(prefix + "mcef.")) {
            return hasMods(mods, "mcef")
                    && System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
        }
        // Preserve the existing fallback for mixins outside these optional feature groups.
        return true;
    }

    private static boolean hasMods(LoadingModList mods, String... ids) {
        for (String id : ids) {
            if (mods.getModFileById(id) == null) return false;
        }
        return true;
    }
}
