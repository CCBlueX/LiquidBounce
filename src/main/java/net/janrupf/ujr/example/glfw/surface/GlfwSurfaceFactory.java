package net.janrupf.ujr.example.glfw.surface;

import net.janrupf.ujr.api.surface.UltralightSurface;
import net.janrupf.ujr.api.surface.UltralightSurfaceFactory;

/**
 * This replaces the default bitmap surface factory so that Ultralight renders into GL textures.
 */
public class GlfwSurfaceFactory implements UltralightSurfaceFactory {
    @Override
    public UltralightSurface createSurface(long width, long height) {
        return new GlfwSurface(width, height);
    }

    @Override
    public void destroySurface(UltralightSurface surface) {
        ((GlfwSurface) surface).destroy();
    }
}
