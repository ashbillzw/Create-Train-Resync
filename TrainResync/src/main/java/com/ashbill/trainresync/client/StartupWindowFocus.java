package com.ashbill.trainresync.client;

import com.mojang.logging.LogUtils;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinUser;
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
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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

    private static boolean waitForVideoFrame;
    private static boolean visitedVideo;

    private StartupWindowFocus() {}

    @Mod.EventBusSubscriber(modid = "trainresync", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            if (Platform.get() == Platform.WINDOWS && ModList.get().isLoaded("fancymenu") && ModList.get().isLoaded("mcef")) {
                event.enqueueWork(() -> {
                    StartupVideoFrame.install();
                    waitForVideoFrame = true;
                });
            }
        }
    }

    @SubscribeEvent
    public static void afterClientTick(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean targetScreen = ModList.get().isLoaded("fancymenu")
                ? FancyMenuScreen.isVideo(minecraft.screen) : minecraft.screen instanceof TitleScreen;
        if (waitForVideoFrame) {
            if (!targetScreen) {
                if (visitedVideo) finished = true;
                return;
            }
            visitedVideo = true;
            if (minecraft.getOverlay() == null && StartupVideoFrame.presented()) {
                WindowsFocus.onFirstFrame(minecraft.getWindow().getWindow());
                finished = true;
            }
            return;
        }
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
        private static final int POSITION_FLAGS = WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE
                | WinUser.SWP_NOACTIVATE | WinUser.SWP_NOOWNERZORDER;
        private static final HWND HWND_TOP = null; // Win32 defines HWND_TOP as (HWND)0.
        private static final int WS_EX_TOPMOST = 0x00000008;

        private static void onFirstFrame(long window) {
            HWND game = new HWND(new Pointer(GLFWNativeWin32.glfwGetWin32Window(window)));
            User32 api = User32.INSTANCE;
            if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE)
                api.ShowWindow(game, WinUser.SW_SHOWNOACTIVATE);
            // Chloride sets Minecraft's fullscreen flag for both borderless and exclusive fullscreen.
            if (!Minecraft.getInstance().getWindow().isFullscreen()
                    && GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_MAXIMIZED) != GLFW.GLFW_TRUE) {
                GLFW.glfwMaximizeWindow(window);
                LogUtils.getLogger().info("Maximized windowed game for the opening video");
            }
            HWND previous = api.GetForegroundWindow();
            if (game.equals(previous)) {
                LogUtils.getLogger().info("Opening video frame presented; game already in foreground");
                return;
            }
            if ((api.GetWindowLong(game, WinUser.GWL_EXSTYLE) & WS_EX_TOPMOST) != 0) {
                LogUtils.getLogger().info("Opening video frame presented; game already topmost (foreground={})", previous);
                return;
            }
            if (api.SetWindowPos(game, HWND_TOP, 0, 0, 0, 0, POSITION_FLAGS)) {
                LogUtils.getLogger().info("Opening video frame presented; requested one-time raise for game={} (foreground={})", game, previous);
            } else {
                LogUtils.getLogger().warn("Could not raise opening video: Win32 error {}", com.sun.jna.Native.getLastError());
            }
        }

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
