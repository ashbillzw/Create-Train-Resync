package com.ashbill.trainresync.currency;

import dev.ithundxr.createnumismatics.content.coins.CoinItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

public final class CurrencyHud {
    private static final ForgeConfigSpec.Builder CONFIG = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue VISIBLE = CONFIG.define("showCurrencyHud", true);
    private static ItemStack spur, terminal;
    private static LocalPlayer lastPlayer;
    private static int lastTick;
    private static long cash;

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CONFIG.build(), "trainresync-currency-client.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CurrencyHud::register);
        MinecraftForge.EVENT_BUS.addListener(CurrencyHud::logout);
    }

    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        CurrencyBalance.clientBalance = 0;
        lastPlayer = null;
    }

    private static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("currency", (gui, graphics, partialTick, width, height) -> render(graphics, width));
    }

    private static void render(GuiGraphics graphics, int width) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.options.hideGui || mc.options.renderDebug || !VISIBLE.get()) return;
        if (spur == null) {
            spur = icon("spur");
            terminal = icon("bank_terminal");
        }
        if (player != lastPlayer || player.tickCount != lastTick) {
            cash = 0;
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                var stack = inventory.getItem(i);
                if (stack.getItem() instanceof CoinItem item) cash += (long) item.coin.value * stack.getCount();
            }
            lastPlayer = player;
            lastTick = player.tickCount;
        }
        int right = width - 4;
        if (CurrencyBalance.clientBalance != 0) {
            int left = drawAmount(graphics, terminal, CurrencyBalance.clientBalance, right);
            right = left - 3 - mc.font.width("+");
            graphics.drawString(mc.font, "+", right, 8, 0xffffff);
            right -= 3;
        }
        drawAmount(graphics, spur, cash, right);
    }

    private static int drawAmount(GuiGraphics graphics, ItemStack icon, long value, int right) {
        var font = Minecraft.getInstance().font;
        String text = Long.toString(value);
        int x = right - font.width(text);
        graphics.drawString(font, text, x, 8, 0xffffff);
        graphics.renderItem(icon, x - 18, 4);
        return x - 18;
    }

    private static ItemStack icon(String id) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("numismatics", id)));
    }
}
