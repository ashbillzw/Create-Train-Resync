package com.ashbill.trainresync.client;

import com.cinemamod.mcef.MCEF;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;

/** Loaded only for the Windows FancyMenu/MCEF opening screen. */
final class StartupVideoFrame {
    private static final String PAGE = "https://mc1.ashbill.com:25572/assets/bg-video-loader.html";
    private static final String V3_PAGE = "https://mc1.ashbill.com:25572/v3/opening.html";
    private static volatile boolean presented;

    static boolean presented() {
        return presented;
    }

    static void install() {
        if (MCEF.isInitialized()) register();
        else MCEF.scheduleForInit(ok -> { if (ok) register(); });
    }

    private static void register() {
        var client = MCEF.getClient();
        var config = new CefMessageRouter.CefMessageRouterConfig("niijimaVideoFrame", "niijimaCancelVideoFrame");
        client.getHandle().addMessageRouter(CefMessageRouter.create(config, new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long id, String request,
                                   boolean persistent, CefQueryCallback callback) {
                if (!(PAGE.equals(frame.getURL()) || V3_PAGE.equals(frame.getURL())) || !"presented".equals(request)) return false;
                presented = true;
                callback.success("");
                return true;
            }
        }));
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int status) {
                if (presented || !(PAGE.equals(frame.getURL()) || V3_PAGE.equals(frame.getURL())) || status != 200) return;
                frame.executeJavaScript("""
                        document.querySelector('video')?.requestVideoFrameCallback(() => {
                            window.niijimaVideoFrame({request: 'presented'});
                        });
                        """, frame.getURL(), 1);
            }
        });
    }
}
