/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.ultralight.window;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.NativeResource;

import java.util.Objects;

import static org.lwjgl.opengl.GL42.glViewport;

/**
 * Helper class to hold the state of a GLFW window.
 */
public class Window {
    private final long handle;

    private WindowStateListener stateListener;

    private final WindowController windowController;

    public Window(WindowController windowController, long handle) {
        this.windowController = windowController;
        this.handle = handle;

        disposeResource(GLFW.glfwSetFramebufferSizeCallback(
                handle,
                (window, width, height) -> this.onFramebufferSize(width, height)
        ));

        disposeResource(GLFW.glfwSetCursorPosCallback(
                handle,
                (window, x, y) -> this.onCursorPos(x, y)
        ));

        disposeResource(GLFW.glfwSetMouseButtonCallback(
                handle,
                (window, button, action, mods) -> this.onMouseButton(button, action, mods)
        ));

        disposeResource(GLFW.glfwSetScrollCallback(
                handle,
                (window, x, y) -> this.onScroll(x, y)
        ));

        disposeResource(GLFW.glfwSetCharModsCallback(
                handle,
                (window, codepoint, mods) -> this.onCharMods(codepoint, mods)
        ));

        disposeResource(GLFW.glfwSetKeyCallback(
                handle,
                (window, key, scancode, action, mods) -> this.onKey(key, scancode, action, mods)
        ));

        disposeResource(GLFW.glfwSetWindowCloseCallback(handle, (window) -> this.onClose()));

        disposeResource(GLFW.glfwSetWindowFocusCallback(handle, (window, isFocused) -> this.onFocusChange(isFocused)));
    }

    public void setStateListener(WindowStateListener stateListener) {
        this.stateListener = stateListener;
    }

    public void activateContext() {
        GLFW.glfwMakeContextCurrent(handle);
    }

    public void swapBuffers() {
        GLFW.glfwSwapBuffers(handle);
    }

    private void onFramebufferSize(int width, int height) {
        GLFW.glfwMakeContextCurrent(handle);
        glViewport(0, 0, width, height);

        windowController.activateRootContext();

        if (stateListener != null) {
            stateListener.onFramebufferSizeChange(width, height);
        }
    }

    private void onCursorPos(double x, double y) {
        if (stateListener != null) {
            stateListener.onCursorPos(x, y);
        }
    }

    private void onMouseButton(int button, int action, int mods) {
        if (stateListener != null) {
            stateListener.onMouseButton(button, action, mods);
        }
    }

    private void onScroll(double x, double y) {
        if (stateListener != null) {
            stateListener.onScroll(x, y);
        }
    }

    private void onCharMods(int codepoint, int mods) {
        if (stateListener != null) {
            stateListener.onCharMods(codepoint, mods);
        }
    }

    private void onKey(int key, int scancode, int action, int mods) {
        if (stateListener != null) {
            stateListener.onKey(key, scancode, action, mods);
        }
    }

    private void onFocusChange(boolean isFocused) {
        if (stateListener != null) {
            stateListener.onFocusChange(isFocused);
        }
    }

    private void onClose() {
        // Reset all the callbacks to prevent dangling references
        // and also to free the old ones.
        disposeResource(GLFW.glfwSetFramebufferSizeCallback(handle, null));
        disposeResource(GLFW.glfwSetCursorPosCallback(handle, null));
        disposeResource(GLFW.glfwSetMouseButtonCallback(handle, null));
        disposeResource(GLFW.glfwSetScrollCallback(handle, null));
        disposeResource(GLFW.glfwSetCharModsCallback(handle, null));
        disposeResource(GLFW.glfwSetKeyCallback(handle, null));
        disposeResource(GLFW.glfwSetWindowCloseCallback(handle, null));
        disposeResource(GLFW.glfwSetWindowFocusCallback(handle, null));

        windowController.notifyClose(this);
    }

    public boolean isMouseDown(int button) {
        return GLFW.glfwGetMouseButton(handle, button) == GLFW.GLFW_PRESS;
    }

    public void setCursor(int cursor) {
        if (cursor == 0) {
            GLFW.glfwSetCursor(handle, 0);
        } else {
            GLFW.glfwSetCursor(handle, windowController.getStandardCursor(cursor));
        }
    }

    private void disposeResource(NativeResource resource) {
        if (resource != null) {
            resource.close();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Window)) return false;
        Window window = (Window) o;
        return handle == window.handle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(handle);
    }
}
