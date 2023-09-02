package net.janrupf.ujr.example.glfw.surface;

import net.janrupf.ujr.api.math.IntRect;
import net.janrupf.ujr.api.surface.UltralightSurface;
import net.janrupf.ujr.api.util.UltralightBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL44;

import java.nio.ByteBuffer;

/**
 * This is a custom implementation of the UltralightSurface interface.
 * <p>
 * It uses a texture to store the pixel data and a ByteBuffer to store the
 * pixel data in system memory.
 */
public class GlfwSurface implements UltralightSurface {
    private static final Logger LOGGER = LogManager.getLogger(GlfwSurface.class);

    private int texture;
    private ByteBuffer pixelBuffer;

    private long width;
    private long height;

    private IntRect dirtyBounds;

    public GlfwSurface(long width, long height) {
        resize(width, height);
        LOGGER.debug("Created surface with texture {}", texture);
    }

    @Override
    public long width() {
        return width;
    }

    @Override
    public long height() {
        return height;
    }

    @Override
    public long rowBytes() {
        return width * 4;
    }

    @Override
    public long size() {
        return width * height * 4;
    }

    @Override
    public UltralightBuffer lockPixels() {
        LOGGER.trace("Locked surface with texture {}", texture);
        return new GlfwSurfaceBuffer(pixelBuffer, this);
    }

    @Override
    public void resize(long width, long height) {
        this.width = width;
        this.height = height;

        if (this.texture != 0) {
            // We need to delete the old texture as we set the size using
            // immutable storage.
            GL30.glDeleteTextures(this.texture);
        }

        this.texture = GL30.glGenTextures();

        // Configure the texture
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, this.texture);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
        GL44.glTexStorage2D(GL30.GL_TEXTURE_2D, 1, GL30.GL_RGBA8, (int) width, (int) height);

        LOGGER.debug("Resized surface with texture {} to {}x{}", texture, width, height);

        // Make sure to have a fitting pixel buffer
        this.pixelBuffer = ByteBuffer.allocateDirect((int) (width * height * 4));
    }

    @Override
    public void setDirtyBounds(IntRect bounds) {
        this.dirtyBounds = bounds;
    }

    @Override
    public IntRect dirtyBounds() {
        if (dirtyBounds != null) {
            return dirtyBounds;
        } else {
            // We don't want to return null into the native code
            return new IntRect(0, 0, 0, 0);
        }
    }

    @Override
    public void clearDirtyBounds() {
        this.dirtyBounds = null;
    }

    public void destroy() {
        GL30.glDeleteTextures(this.texture);

        LOGGER.debug("Destroyed surface with texture {}", texture);
    }

    public int getTexture() {
        return texture;
    }
}
