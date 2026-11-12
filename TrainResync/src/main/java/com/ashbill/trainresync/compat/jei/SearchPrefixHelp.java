package com.ashbill.trainresync.compat.jei;

import com.ashbill.trainresync.mixin.search.CreativeSearchBoxAccessor;
import com.ashbill.trainresync.mixin.search.JeiSearchFieldAccessor;
import mezz.jei.api.runtime.IIngredientListOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector2i;

import java.util.List;

public final class SearchPrefixHelp {
    private final IIngredientListOverlay overlay;

    SearchPrefixHelp(IIngredientListOverlay overlay) {
        this.overlay = overlay;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void draw(ScreenEvent.Render.Post event) {
        EditBox field;
        if (overlay.hasKeyboardFocus() && overlay instanceof JeiSearchFieldAccessor jei) {
            field = jei.trainresync$getSearchField();
        } else if (event.getScreen() instanceof CreativeSearchBoxAccessor creative) {
            field = creative.trainresync$getSearchBox();
        } else {
            return;
        }
        if (!field.isVisible() || !field.isFocused() || !field.getValue().equals("?")) return;

        var lines = List.of(Component.translatable("trainresync.search.help.mods").getVisualOrderText(),
                Component.translatable("trainresync.search.help.tags").getVisualOrderText(),
                Component.translatable("trainresync.search.help.ids").getVisualOrderText());
        event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, lines, (width, height, x, y, w, h) -> {
            int below = y + field.getHeight() + 5;
            return new Vector2i(Math.max(4, Math.min(x, width - w - 4)),
                    below + h + 4 <= height ? below : Math.max(4, y - h - 5));
        }, field.getX(), field.getY());
    }
}
