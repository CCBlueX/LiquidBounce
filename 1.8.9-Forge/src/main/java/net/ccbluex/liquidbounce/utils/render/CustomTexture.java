/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.utils.render;

import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;

public class CustomTexture {
    private final BufferedImage image;
    private boolean unloaded;
    private int textureId = -1;

    public CustomTexture(BufferedImage image) {
        this.image = image;
    }

    /**
     * @return ID of this texture loaded into memory
     * @throws IllegalStateException If the texture was unloaded via {@link #unload()}
     */
    public int getTextureId() {
        if (unloaded)
            throw new IllegalStateException("Texture unloaded");

        if (textureId == -1)
            textureId = TextureUtil.uploadTextureImageAllocate(TextureUtil.glGenTextures(), image, true, true);

        return textureId;
    }

    public void unload() {
        if (!unloaded) {
            GL11.glDeleteTextures(textureId);
            unloaded = true;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        unload();
    }
}
