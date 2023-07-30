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
import net.janrupf.ujr.api.util.NioUltralightBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL42.*;

/**
 * Extension of a {@link NioUltralightBuffer} that is used to render the content of a {@link GlfwSurface} to a texture.
 */
public class GlfwSurfaceBuffer extends NioUltralightBuffer {
    private final static Logger LOGGER = LogManager.getLogger(GlfwSurfaceBuffer.class);

    private final ByteBuffer pixelBuffer;
    private final GlfwSurface surface;

    public GlfwSurfaceBuffer(ByteBuffer pixelBuffer, GlfwSurface surface) {
        super(pixelBuffer);
        this.pixelBuffer = pixelBuffer;
        this.surface = surface;
    }

    @Override
    public void close() {
        IntRect dirtyBounds = surface.dirtyBounds();

        long bufferPtr = MemoryUtil.memAddress(pixelBuffer);

        // Copy the pixel data from the PBO to the texture
        glBindTexture(GL_TEXTURE_2D, surface.getTexture());
        if (dirtyBounds.width() == surface.width()) {
            // We can copy using a rectangle
            glTexSubImage2D(
                    GL_TEXTURE_2D,
                    0,
                    dirtyBounds.getLeft(),
                    dirtyBounds.getTop(),
                    dirtyBounds.width(),
                    dirtyBounds.height(),
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    bufferPtr + (surface.rowBytes() * dirtyBounds.getTop() + (dirtyBounds.getLeft() * 4L))
            );
        } else {
            // We need to manually copy multiple scanlines
            for (int y = dirtyBounds.getTop(); y < dirtyBounds.getBottom(); y++) {
                glTexSubImage2D(
                        GL_TEXTURE_2D,
                        0,
                        dirtyBounds.getLeft(),
                        y,
                        dirtyBounds.width(),
                        1,
                        GL_RGBA,
                        GL_UNSIGNED_BYTE,
                        bufferPtr + (surface.rowBytes() * y + (dirtyBounds.getLeft() * 4L))
                );
            }
        }
        glGenerateMipmap(GL_TEXTURE_2D);

        // As we have uploaded the data to the texture, we can clear the dirty bounds
        surface.clearDirtyBounds();

        LOGGER.trace("Updated texture {} (updated {}x{})", surface.getTexture(), dirtyBounds.width(), dirtyBounds.height());

        super.close();
    }
}
