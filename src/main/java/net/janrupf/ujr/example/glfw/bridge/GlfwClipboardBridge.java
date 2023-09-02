package net.janrupf.ujr.example.glfw.bridge;

import net.janrupf.ujr.api.clipboard.UltralightClipboard;
import org.lwjgl.glfw.GLFW;

/**
 * Clipboard implementation using GLFW to access the clipboard.
 */
public class GlfwClipboardBridge implements UltralightClipboard {
    @Override
    public void clear() {
        GLFW.glfwSetClipboardString(0, "");
    }

    @Override
    public String readPlainText() {
        return GLFW.glfwGetClipboardString(0);
    }

    @Override
    public void writePlainText(String text) {
        GLFW.glfwSetClipboardString(0, text);
    }
}
