package com.ashbill.trainresync.mixin.search.modernfix;

import net.minecraft.client.searchtree.RefreshableSearchTree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "org.embeddedt.modernfix.searchtree.LazySearchTree", remap = false)
public interface LazySearchTreeAccessor {
    @Invoker("getRealTree") RefreshableSearchTree<?> trainresync$getRealTree();
}
