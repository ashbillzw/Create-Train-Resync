package com.ashbill.trainresync.client;

import com.mojang.logging.LogUtils;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinBase;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import me.srrapero720.chloride.ChlorideConfig;
import me.srrapero720.chloride.impl.Borderless;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
            if (Platform.get() != Platform.WINDOWS || !ModList.get().isLoaded("fancymenu")) return;
            if (ModList.get().isLoaded("drippyloadingscreen") && ModList.get().isLoaded("chloride"))
                event.enqueueWork(DrippySplash::listen);
            if (ModList.get().isLoaded("mcef")) {
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
                WindowsFocus.present(minecraft.getWindow().getWindow(), true);
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

    private static final class DrippySplash {
        private static boolean pending = true;
        private static final String channel = "Local\\Niijima.StartupFocus." + ProcessHandle.current().pid();
        // Keep the decision visible to a late launcher poll for this JVM's lifetime.
        private static HANDLE decision;

        static void listen() {
            EventHandler.INSTANCE.registerListener(event -> {
                if (!pending) return;
                Minecraft game = Minecraft.getInstance();
                Screen screen = ((RenderScreenEvent.Post) event).getScreen();
                boolean loading = game.getOverlay() != null;
                if (loading && !ScreenIdentifierHandler.isIdentifierOfScreen("drippy_loading_overlay", screen)) return;
                if (claimStartup() && loading && ChlorideConfig.fullScreen == Borderless.Mode.FULLSCREEN && game.getWindow().isFullscreen())
                    WindowsFocus.present(game.getWindow().getWindow(), false);
            }, RenderScreenEvent.Post.class);
        }

        private static boolean claimStartup() {
            Kernel32 api = Kernel32.INSTANCE;
            HANDLE gate = api.CreateMutex(null, false, channel + ".gate");
            if (gate != null) {
                try {
                    int wait = api.WaitForSingleObject(gate, 0);
                    if (wait == com.sun.jna.platform.win32.WinError.WAIT_TIMEOUT) return false; // Retry next frame.
                    if (wait == WinBase.WAIT_OBJECT_0 || wait == WinBase.WAIT_ABANDONED) {
                        try {
                            decision = api.CreateEvent(null, true, false, channel + ".closed");
                            if (decision != null) {
                                int state = api.WaitForSingleObject(decision, 0);
                                if (state == WinBase.WAIT_OBJECT_0) {
                                    pending = false;
                                    LogUtils.getLogger().info("Skipping Drippy focus after confirmed launcher handoff");
                                    return false;
                                }
                                if (state == com.sun.jna.platform.win32.WinError.WAIT_TIMEOUT && api.SetEvent(decision)) {
                                    pending = false;
                                    return true;
                                }
                            }
                        } finally {
                            api.ReleaseMutex(gate);
                        }
                    }
                } finally {
                    api.CloseHandle(gate);
                }
            }
            pending = false;
            LogUtils.getLogger().warn("Cannot coordinate launcher focus; skipping Drippy focus");
            return false;
        }
    }

    private static final class WindowsFocus {
        private static final HWND TOPMOST = new HWND(Pointer.createConstant(-1));
        private static final HWND NOT_TOPMOST = new HWND(Pointer.createConstant(-2));
        private static final int POSITION_FLAGS = WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE;
        private static final int WS_EX_TOPMOST = 0x00000008;

        private static void present(long window, boolean openingVideo) {
            String stage = openingVideo ? "Opening video" : "Drippy splash";
            HWND game = new HWND(new Pointer(GLFWNativeWin32.glfwGetWin32Window(window)));
            User32 api = User32.INSTANCE;
            if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE)
                api.ShowWindow(game, WinUser.SW_SHOWNOACTIVATE);
            // Chloride sets Minecraft's fullscreen flag for both borderless and exclusive fullscreen.
            if (openingVideo && !Minecraft.getInstance().getWindow().isFullscreen()
                    && GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_MAXIMIZED) != GLFW.GLFW_TRUE) {
                GLFW.glfwMaximizeWindow(window);
                LogUtils.getLogger().info("Maximized windowed game for the opening video");
            }
            HWND previous = api.GetForegroundWindow();
            if (game.equals(previous)) {
                LogUtils.getLogger().info("{} frame presented; game already in foreground", stage);
                return;
            }
            // SDL2's force-raise sequence, preserving maximization and any existing topmost setting.
            DWORD currentThread = new DWORD(Kernel32.INSTANCE.GetCurrentThreadId());
            DWORD otherThread = new DWORD(previous == null ? 0 : api.GetWindowThreadProcessId(previous, null));
            boolean wasTopmost = isTopmost(game);
            boolean attached = otherThread.longValue() != 0 && !otherThread.equals(currentThread)
                    && api.AttachThreadInput(otherThread, currentThread, true);
            boolean raised = false;
            boolean lowered = false;
            boolean activated;
            try {
                if (!wasTopmost) {
                    raised = api.SetWindowPos(game, TOPMOST, 0, 0, 0, 0, POSITION_FLAGS);
                    if (raised)
                        lowered = api.SetWindowPos(game, NOT_TOPMOST, 0, 0, 0, 0, POSITION_FLAGS);
                }
                activated = api.SetForegroundWindow(game);
            } finally {
                try {
                    if (raised && !lowered && isTopmost(game))
                        api.SetWindowPos(game, NOT_TOPMOST, 0, 0, 0, 0, POSITION_FLAGS | WinUser.SWP_NOACTIVATE);
                } finally {
                    if (attached && !api.AttachThreadInput(otherThread, currentThread, false))
                        LogUtils.getLogger().warn("Could not detach {} input queues", stage);
                }
            }
            if (game.equals(api.GetForegroundWindow()))
                api.SetFocus(game);
            LogUtils.getLogger().info("{} activation: game={}, before={}, previouslyTopmost={}, "
                    + "attached={}, raised={}, lowered={}, activated={}", stage, game, previous, wasTopmost,
                    attached, raised, lowered, activated);
            logFocus(game, stage, "immediate");
            CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> logFocus(game, stage, "500ms"));
        }

        private static boolean isTopmost(HWND game) {
            return (User32.INSTANCE.GetWindowLong(game, WinUser.GWL_EXSTYLE) & WS_EX_TOPMOST) != 0;
        }

        private static void logFocus(HWND game, String stage, String sample) {
            User32 api = User32.INSTANCE;
            WinUser.GUITHREADINFO info = new WinUser.GUITHREADINFO();
            info.cbSize = info.size();
            boolean valid = api.GetGUIThreadInfo(0, info);
            LogUtils.getLogger().info("{} focus {}: foreground={}, keyboardFocus={}, game={}, topmost={}",
                    stage, sample, api.GetForegroundWindow(), valid ? info.hwndFocus : null, game, isTopmost(game));
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
