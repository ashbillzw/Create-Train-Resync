package com.ashbill.trainresync.mixin.search.modernfix;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.client.CreativeModeTabSearchRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeSearchPrefixMixin {
    @Shadow private static CreativeModeTab selectedTab;

    @ModifyExpressionValue(method = "refreshSearchResults", at = @At(value = "INVOKE",
            target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z", remap = false))
    private boolean trainresync$useVanillaTagSearch(boolean startsWithHash) {
        if (!startsWithHash) return false;
        var key = CreativeModeTabSearchRegistry.getNameSearchKey(selectedTab);
        SearchTree<?> tree = Minecraft.getInstance().getSearchTree(key);
        if (tree instanceof LazySearchTreeAccessor lazy) tree = lazy.trainresync$getRealTree();
        // ModernFix's JEI name tree accepts the whole query, including JEI's tooltip prefix.
        return !tree.getClass().getName().equals("org.embeddedt.modernfix.searchtree.JEIBackedSearchTree");
    }
}
