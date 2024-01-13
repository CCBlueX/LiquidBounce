package net.janrupf.ujr.example.glfw.web.listener;

import net.janrupf.ujr.api.UltralightView;
import net.janrupf.ujr.api.cursor.UlCursor;
import net.janrupf.ujr.api.listener.UlMessageLevel;
import net.janrupf.ujr.api.listener.UlMessageSource;
import net.janrupf.ujr.api.listener.UltralightViewListener;
import net.janrupf.ujr.example.glfw.web.CursorTranslator;
import net.janrupf.ujr.example.glfw.web.WebWindow;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebViewListener implements UltralightViewListener {
    private static final Logger LOGGER = LogManager.getLogger(WebViewListener.class);
    private final WebWindow window;

    public WebViewListener(WebWindow window) {
        this.window = window;
    }

    @Override
    public void onChangeCursor(UltralightView view, UlCursor cursor) {
        int cursorId = CursorTranslator.ultralightToGlfwCursor(cursor);
        CursorTranslator.changeCursor(cursorId);
    }

    @Override
    public void onAddConsoleMessage(UltralightView view, UlMessageSource source, UlMessageLevel level, String message, long lineNumber, long columnNumber, String sourceId) {
        LOGGER.log(Level.INFO, message + ":" + lineNumber + "@" + columnNumber + " " + sourceId);
    }


}
