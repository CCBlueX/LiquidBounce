package net.janrupf.ujr.example.glfw.surface;

import net.janrupf.ujr.api.math.IntRect;
import net.janrupf.ujr.api.util.NioUltralightBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

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
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, surface.getTexture());
        if (dirtyBounds.width() == surface.width()) {
            // We can copy using a rectangle
            GL30.glTexSubImage2D(
                    GL30.GL_TEXTURE_2D,
                    0,
                    dirtyBounds.getLeft(),
                    dirtyBounds.getTop(),
                    dirtyBounds.width(),
                    dirtyBounds.height(),
                    GL30.GL_RGBA,
                    GL30.GL_UNSIGNED_BYTE,
                    bufferPtr + (surface.rowBytes() * dirtyBounds.getTop() + (dirtyBounds.getLeft() * 4L))
            );
        } else {
            // We need to manually copy multiple scanlines
            for (int y = dirtyBounds.getTop(); y < dirtyBounds.getBottom(); y++) {
                GL30.glTexSubImage2D(
                        GL30.GL_TEXTURE_2D,
                        0,
                        dirtyBounds.getLeft(),
                        y,
                        dirtyBounds.width(),
                        1,
                        GL30.GL_RGBA,
                        GL30.GL_UNSIGNED_BYTE,
                        bufferPtr + (surface.rowBytes() * y + (dirtyBounds.getLeft() * 4L))
                );
            }
        }
        GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D);

        // As we have uploaded the data to the texture, we can clear the dirty bounds
        surface.clearDirtyBounds();

        LOGGER.trace("Updated texture {} (updated {}x{})", surface.getTexture(), dirtyBounds.width(), dirtyBounds.height());

        super.close();
    }
}
