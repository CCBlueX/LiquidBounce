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

package net.ccbluex.liquidbounce.ultralight.surface;

import net.janrupf.ujr.api.math.IntRect;
import net.janrupf.ujr.api.surface.UltralightSurface;
import net.janrupf.ujr.api.util.UltralightBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL42.*;

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
            glDeleteTextures(this.texture);
        }

        this.texture = glGenTextures();

        // Configure the texture
        glBindTexture(GL_TEXTURE_2D, this.texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, (int) width, (int) height);

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
        glDeleteTextures(this.texture);

        LOGGER.debug("Destroyed surface with texture {}", texture);
    }

    public int getTexture() {
        return texture;
    }
}
