package com.ashbill.trainresync.client;

import com.ashbill.trainresync.compat.jei.DeliveryRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.regex.Pattern;

import static com.ashbill.trainresync.meituan.MeituanService.DISH_LINK;

@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.CLIENT)
public final class DeliveryChatDetails {
    private static final Pattern OFFER = Pattern.compile("/美团 接单 [1-9][0-9]* [a-f0-9-]{36}/[0-9]+");
    private static DeliveryChatDetails open;

    private final Style link;
    private final List<FormattedCharSequence> lines;
    private final int x, y, width, visibleLines, height, lineStep;
    private final Button accept;
    private boolean pinned;
    private int scroll;

    private DeliveryChatDetails(ChatScreen chat, Style style, int mouseX, int mouseY) {
        link = style;
        var mc = Minecraft.getInstance();
        Font font = mc.font;
        Component detail = style.getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT);
        // The panel touches one edge of the link: the pointer can enter it or click the link again.
        int left = mouseX, right = mouseX + 1;
        while (left > 0 && link.equals(styleAt(left - 1, mouseY))) left--;
        while (right < chat.width && link.equals(styleAt(right, mouseY))) right++;
        int rightSpace = chat.width - right - 5, leftSpace = left - 5;
        boolean toRight = rightSpace >= 218 || rightSpace >= leftSpace;
        int limit = Math.max(12, Math.min(218, toRight ? rightSpace : leftSpace));
        lines = font.split(detail, limit - 8);
        width = Math.min(limit, Math.max(72, lines.stream().mapToInt(font::width).max().orElse(0)) + 8);
        lineStep = font.lineHeight + 2;
        visibleLines = Math.min(lines.size(), Math.max(1, (chat.height - 45) / lineStep));
        height = visibleLines * lineStep - 2 + 24;
        x = toRight ? right : left - width;
        y = Mth.clamp(mouseY - 5, 4, chat.height - height - 4);
        accept = Button.builder(Component.literal("接单"), button -> {
            if (mc.player != null) mc.player.connection.sendCommand(link.getClickEvent().getValue().substring(1));
            open = null;
        }).bounds(x + 2, y + height - 18, width - 4, 16).build();
    }

    private static Style styleAt(double x, double y) {
        return Minecraft.getInstance().gui.getChat().getClickedComponentStyleAt(x, y);
    }

    private static boolean isOffer(Style style) {
        if (style == null || style.getClickEvent() == null || style.getHoverEvent() == null) return false;
        var click = style.getClickEvent();
        return click.getAction() == ClickEvent.Action.SUGGEST_COMMAND
                && style.getHoverEvent().getAction() == HoverEvent.Action.SHOW_TEXT && OFFER.matcher(click.getValue()).matches();
    }

    private boolean contains(double mx, double my) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    @SubscribeEvent public static void tooltip(RenderTooltipEvent.Pre event) {
        if (Minecraft.getInstance().screen instanceof ChatScreen &&
                ((open != null && open.contains(event.getX(), event.getY())) || isOffer(styleAt(event.getX(), event.getY())))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ChatScreen chat)) return;
        int mx = event.getMouseX(), my = event.getMouseY();
        if (open == null || (!open.pinned && !open.contains(mx, my))) {
            Style style = styleAt(mx, my);
            if (!isOffer(style)) open = null;
            else if (open == null || !open.link.equals(style)) open = new DeliveryChatDetails(chat, style, mx, my);
        }
        if (open != null) open.draw(event);
    }

    private void draw(ScreenEvent.Render.Post event) {
        var graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);
        TooltipRenderUtil.renderTooltipBackground(graphics, x, y, width, height, 0);
        if (pinned) graphics.fill(x, y - 2, x + width, y - 1, 0xffffbd46);
        for (int i = 0; i < visibleLines; i++) {
            graphics.drawString(font, lines.get(scroll + i), x + 4, y + 2 + i * lineStep, 0xffffff);
        }
        accept.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        graphics.pose().popPose();
    }

    private static boolean openRecipe(Style style) {
        if (style == null || !DISH_LINK.equals(style.getInsertion()) || style.getHoverEvent() == null) return false;
        var item = style.getHoverEvent().getValue(HoverEvent.Action.SHOW_ITEM);
        if (item == null || !ModList.get().isLoaded("jei")) return false;
        DeliveryRecipes.show(item.getItemStack());
        return true;
    }

    @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen chat) || event.getButton() != 0) return;
        double mx = event.getMouseX(), my = event.getMouseY();
        if (open != null && open.contains(mx, my)) {
            event.setCanceled(true);
            if (open.accept.mouseClicked(mx, my, 0)) return;
            Font font = Minecraft.getInstance().font;
            int dy = Mth.floor(my - open.y - 2);
            int line = dy / open.lineStep;
            if (dy >= 0 && line < open.visibleLines && dy % open.lineStep < font.lineHeight && mx >= open.x + 4)
                openRecipe(font.getSplitter().componentStyleAtWidth(open.lines.get(open.scroll + line), (int) mx - open.x - 4));
            return;
        }
        Style style = styleAt(mx, my);
        if (isOffer(style)) {
            if (open == null || !open.link.equals(style)) open = new DeliveryChatDetails(chat, style, (int) mx, (int) my);
            open.pinned = !open.pinned;
            event.setCanceled(true);
        } else if (openRecipe(style)) event.setCanceled(true);
    }

    @SubscribeEvent public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (open != null && open.contains(event.getMouseX(), event.getMouseY())) {
            open.scroll = Mth.clamp(open.scroll - (int) Math.signum(event.getScrollDelta()), 0, open.lines.size() - open.visibleLines);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent public static void reset(ScreenEvent.Init.Pre event) { if (event.getScreen() instanceof ChatScreen) open = null; }
    @SubscribeEvent public static void close(ScreenEvent.Closing event) { if (event.getScreen() instanceof ChatScreen) open = null; }
}
