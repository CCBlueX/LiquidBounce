package net.janrupf.ujr.example.glfw.web.listener;

import net.janrupf.ujr.api.UltralightView;
import net.janrupf.ujr.api.listener.UltralightLoadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebViewLoadListener implements UltralightLoadListener {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onBeginLoading(UltralightView view, long frameId, boolean isMainFrame, String url) {
        LOGGER.info("Started to load {} in frame {} (is main = {})", url, frameId, isMainFrame);
    }

    @Override
    public void onFinishLoading(UltralightView view, long frameId, boolean isMainFrame, String url) {
        LOGGER.info("Finished loading {} in frame {} (is main = {})", url, frameId, isMainFrame);

    }

    @Override
    public void onFailLoading(UltralightView view, long frameId, boolean isMainFrame, String url, String description, String errorDomain, int errorCode) {
        LOGGER.warn("Failed to load {} in frame {} (is main = {}): {} ({}) [{}]", url, frameId, isMainFrame, description, errorDomain, errorCode);
    }
}
