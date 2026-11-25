package com.ashbill.trainresync.client;

import com.ashbill.trainresync.meituan.MeituanService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.CLIENT)
public final class DeliveryChatTable {
    private static final Style SPACING = Style.EMPTY.withFont(new ResourceLocation("trainresync", "table_spacing"));

    @SubscribeEvent
    public static void alignOffers(ClientChatReceivedEvent.System event) {
        var parts = event.getMessage().getSiblings();
        if (event.isOverlay() || parts.isEmpty() || !isRow(parts.get(0))) return;
        var mc = Minecraft.getInstance();
        var chat = mc.gui.getChat();
        Component aligned = layout(parts, mc.font.getSplitter(), (int) Math.floor(chat.getWidth() / chat.getScale()));
        if (aligned != null) event.setMessage(aligned);
    }

    static Component layout(List<Component> parts, StringSplitter font, int available) {
        var rows = new ArrayList<Component[]>();
        float[] widest = new float[4];
        for (int i = 0; i < parts.size(); i += 2) {
            if (!isRow(parts.get(i))) return null;
            Object[] args = ((TranslatableContents) parts.get(i).getContents()).getArgs();
            if (args.length != 5) return null;
            Component[] cells = new Component[args.length];
            for (int c = 0; c < cells.length; c++) {
                cells[c] = args[c] instanceof Component text ? text : Component.literal(args[c].toString());
                if (c < widest.length) widest[c] = Math.max(widest[c], font.stringWidth(cells[c]));
            }
            rows.add(cells);
        }
        var table = Component.empty();
        for (int i = 0; i < rows.size(); i++) {
            var row = Component.empty().setStyle(parts.get(i * 2).getStyle());
            var cells = rows.get(i);
            for (int c = 0; c < cells.length; c++) {
                if (c >= 2 && (c < 4 || i > 0)) row.append(" | ");
                if (c == 2 || c == 3) row.append(padding(widest[c] - font.stringWidth(cells[c])));
                row.append(cells[c]);
                if (c < 2) row.append(padding(widest[c] - font.stringWidth(cells[c])));
            }
            // A narrow chat keeps the compact vanilla layout and its usual word wrapping.
            if (font.stringWidth(row) > available) return null;
            if (i > 0) table.append("\n");
            table.append(row);
        }
        return table;
    }

    private static boolean isRow(Component component) {
        return component.getContents() instanceof TranslatableContents text && MeituanService.OFFER_ROW.equals(text.getKey());
    }

    private static Component padding(float width) {
        int halves = Math.round(width * 2);
        String spaces = "\u2002".repeat(halves / 16) + " ".repeat(halves % 16 / 2) + "\u200a".repeat(halves % 2);
        return Component.literal(spaces).setStyle(SPACING);
    }
}
