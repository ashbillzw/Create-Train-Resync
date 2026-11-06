package com.ashbill.trainresync.client;

import com.mojang.logging.LogUtils;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.Platform;

/** Bring the initial video page forward, with a bounded retry window. */
@Mod.EventBusSubscriber(modid = "trainresync", value = Dist.CLIENT)
public final class StartupWindowFocus {
    private static final int MAX_ATTEMPTS = 3;
    private static boolean finished;
    private static int attempts;
    private static long nextAttemptAt;

    private StartupWindowFocus() {}

    @SubscribeEvent
    public static void afterClientTick(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean targetScreen = ModList.get().isLoaded("fancymenu")
                ? FancyMenuScreen.isVideo(minecraft.screen) : minecraft.screen instanceof TitleScreen;
        if (!targetScreen) {
            if (attempts > 0) {
                finished = true;
                LogUtils.getLogger().info("Stopped startup window-focus retries after leaving the screen");
            }
            return;
        }
        if (minecraft.getOverlay() != null) return;

        long window = minecraft.getWindow().getWindow();
        if (isForeground(window)) {
            finished = true;
            LogUtils.getLogger().info("Startup game window is foreground (attempts={})", attempts);
            return;
        }
        long now = Util.getMillis();
        if (now < nextAttemptAt) return;
        if (attempts == MAX_ATTEMPTS) {
            finished = true;
            LogUtils.getLogger().warn("Game window did not become foreground after {} startup attempts", attempts);
            return;
        }
        attempts++;
        nextAttemptAt = now + 1000;
        if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE)
            GLFW.glfwRestoreWindow(window);
        GLFW.glfwFocusWindow(window);
        LogUtils.getLogger().info("Requested startup game-window focus (attempt={}/{}, screen={})",
                attempts, MAX_ATTEMPTS, minecraft.screen.getClass().getSimpleName());
    }

    private static boolean isForeground(long window) {
        return Platform.get() == Platform.WINDOWS ? WindowsFocus.isForeground(window)
                : GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }

    private static final class WindowsFocus {
        private static boolean isForeground(long window) {
            HWND foreground = User32.INSTANCE.GetForegroundWindow();
            return foreground != null
                    && Pointer.nativeValue(foreground.getPointer()) == GLFWNativeWin32.glfwGetWin32Window(window);
        }
    }

    // Keep the optional FancyMenu class out of the vanilla-only execution path.
    private static final class FancyMenuScreen {
        private static boolean isVideo(Screen screen) {
            return screen instanceof CustomGuiBaseScreen custom && "random_video".equals(custom.getIdentifier());
        }
    }
}
