package com.ashbill.trainresync.compat.localization;

import java.util.regex.Pattern;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Translate existing error-screen controls without modifying KubeJS or its diagnostics. */
@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.CLIENT)
public final class KubeJsErrorButtons {
    private static final Pattern COUNTER = Pattern.compile("View (Warnings|Errors) \\[(\\d+)\\]");

    private KubeJsErrorButtons() {}

    @SubscribeEvent
    public static void afterInit(ScreenEvent.Init.Post event) {
        // A name check avoids a hard dependency on this optional mod.
        if (!event.getScreen().getClass().getName().equals("dev.latvian.mods.kubejs.client.KubeJSErrorScreen")) return;
        for (var listener : event.getListenersList()) {
            if (!(listener instanceof Button button)) continue;
            Component message = button.getMessage();
            if (!(message.getContents() instanceof LiteralContents literal) || !message.getSiblings().isEmpty()) continue;
            MutableComponent translated = translate(literal.text());
            if (translated != null) button.setMessage(translated.setStyle(message.getStyle()));
        }
    }

    private static MutableComponent translate(String text) {
        String key = switch (text) {
            case "Open Log File" -> "open_log";
            case "Quit" -> "quit";
            case "Close" -> "close";
            case "Report" -> "report";
            default -> null;
        };
        if (key != null) return Component.translatable("trainresync.kubejs." + key);
        var match = COUNTER.matcher(text);
        if (!match.matches()) return null;
        return Component.translatable("trainresync.kubejs.view_" +
                (match.group(1).equals("Warnings") ? "warnings" : "errors"), match.group(2));
    }
}
