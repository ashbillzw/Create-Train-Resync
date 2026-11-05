package com.ashbill.trainresync.client;

import com.mojang.logging.LogUtils;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

/** Request focus once when the initial title screen is ready, after loading overlays. */
@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.CLIENT)
public final class StartupWindowFocus {
    private static boolean requested;

    private StartupWindowFocus() {}

    @SubscribeEvent
    public static void afterClientTick(TickEvent.ClientTickEvent event) {
        if (requested || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getOverlay() != null) return;
        if (!(minecraft.screen instanceof TitleScreen)
                && !(ModList.get().isLoaded("fancymenu") && FancyMenuScreen.isVideo(minecraft.screen))) return;

        requested = true;
        long window = minecraft.getWindow().getWindow();
        if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE)
            GLFW.glfwRestoreWindow(window);
        GLFW.glfwFocusWindow(window);
        LogUtils.getLogger().info("Requested game-window focus at initial title screen (GLFW_FOCUSED={})",
                GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE);
    }

    // Keep the optional FancyMenu class out of the vanilla-only execution path.
    private static final class FancyMenuScreen {
        private static boolean isVideo(Screen screen) {
            return screen instanceof CustomGuiBaseScreen custom && "random_video".equals(custom.getIdentifier());
        }
    }
}
