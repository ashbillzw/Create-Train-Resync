package com.ashbill.trainresync.compat.jade;

import com.ashbill.trainresync.compat.cauldron.MilkAccess;
import com.ashbill.trainresync.compat.cauldron.CauldronContents;
import com.ashbill.trainresync.compat.cauldron.CauldronCompatibility;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.mehvahdjukaar.amendments.common.block.LiquidCauldronBlock;
import net.mehvahdjukaar.amendments.common.block.DyeCauldronBlock;
import net.mehvahdjukaar.amendments.common.tile.LiquidCauldronBlockTile;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Display-only integration: a soft milkshake is not an extractable Forge fluid. */
@WailaPlugin
public final class CauldronJadePlugin implements IWailaPlugin {
    private static final ResourceLocation UID = new ResourceLocation("shader_switch_debug", "milkshake_contents");
    @Override public void register(IWailaCommonRegistration registration) {
        if (!CauldronCompatibility.available()) return;
        registration.registerBlockDataProvider(new ContentsData(), LiquidCauldronBlockTile.class);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        if (!CauldronCompatibility.available()) return;
        var tooltip = new ContentsTooltip();
        registration.registerBlockComponent(tooltip, LiquidCauldronBlock.class);
        registration.registerBlockComponent(tooltip, DyeCauldronBlock.class);
    }
    private static final class ContentsData implements IServerDataProvider<BlockAccessor> {
        @Override public ResourceLocation getUid() { return UID; }
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof MilkAccess access)) return;
            String type = access.trainresync$content();
            if (!CauldronContents.shake(type) || access.trainresync$milkAmount() <= 0) return;
            data.putString("sd_shake_flavor", type.substring(6));
            data.putInt("sd_shake_servings", access.trainresync$milkAmount() / 250);
            data.putInt("sd_shake_capacity", access.trainresync$capacity() / 250);
        }
    }

    private static final class ContentsTooltip implements IBlockComponentProvider {
        @Override public ResourceLocation getUid() { return UID; }
        @Override public int getDefaultPriority() { return 2000; }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlock() instanceof DyeCauldronBlock) {
                tooltip.remove(Identifiers.UNIVERSAL_FLUID_STORAGE);
                return;
            }
            CompoundTag data = accessor.getServerData();
            String flavor = data.getString("sd_shake_flavor");
            if (!CauldronContents.FLAVORS.contains(flavor)) return;
            int servings = data.getInt("sd_shake_servings");
            if (servings <= 0) return;
            tooltip.remove(Identifiers.UNIVERSAL_FLUID_STORAGE);
            tooltip.add(Component.translatable("tooltip.shader_switch_debug.milkshake_contents",
                Component.translatable(CauldronContents.milkshakeTranslation(flavor)),
                servings, data.getInt("sd_shake_capacity")));
        }
    }
}
