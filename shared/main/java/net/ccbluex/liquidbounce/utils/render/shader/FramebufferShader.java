/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader;

import net.ccbluex.liquidbounce.api.minecraft.util.IScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUseProgram;

/**
 * @author TheSlowly
 */
public abstract class FramebufferShader extends Shader {

    private static Framebuffer framebuffer;

    protected float red, green, blue, alpha = 1F;
    protected float radius = 2F;
    protected float quality = 1F;

    private boolean entityShadows;

    public FramebufferShader(final String fragmentShader) {
        super(fragmentShader);
    }

    public void startDraw(final float partialTicks) {
        classProvider.getGlStateManager().enableAlpha();

        classProvider.getGlStateManager().pushMatrix();
        classProvider.getGlStateManager().pushAttrib();

        framebuffer = setupFrameBuffer(framebuffer);
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(true);
        entityShadows = mc.getGameSettings().getEntityShadows();
        mc.getGameSettings().setEntityShadows(false);
        mc.getEntityRenderer().setupCameraTransform(partialTicks, 0);
    }

    public void stopDraw(final Color color, final float radius, final float quality) {
        mc.getGameSettings().setEntityShadows(entityShadows);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        mc.getFramebuffer().bindFramebuffer(true);

        red = color.getRed() / 255F;
        green = color.getGreen() / 255F;
        blue = color.getBlue() / 255F;
        alpha = color.getAlpha() / 255F;
        this.radius = radius;
        this.quality = quality;

        mc.getEntityRenderer().disableLightmap();
        RenderHelper.disableStandardItemLighting();

        startShader();
        mc.getEntityRenderer().setupOverlayRendering();
        drawFramebuffer(framebuffer);
        stopShader();

        mc.getEntityRenderer().disableLightmap();

        classProvider.getGlStateManager().popMatrix();
        classProvider.getGlStateManager().popAttrib();
    }

    /**
     * @param frameBuffer
     * @return frameBuffer
     * @author TheSlowly
     */
    public Framebuffer setupFrameBuffer(Framebuffer frameBuffer) {
        if(frameBuffer != null)
            frameBuffer.deleteFramebuffer();

        frameBuffer = new Framebuffer(mc.getDisplayWidth(), mc.getDisplayHeight(), true);

        return frameBuffer;
    }

    /**
     * @author TheSlowly
     */
    public void drawFramebuffer(final Framebuffer framebuffer) {
        final IScaledResolution scaledResolution = classProvider.createScaledResolution(mc);

        glBindTexture(GL_TEXTURE_2D, framebuffer.framebufferTexture);
        glBegin(GL_QUADS);
        glTexCoord2d(0, 1);
        glVertex2d(0, 0);
        glTexCoord2d(0, 0);
        glVertex2d(0, scaledResolution.getScaledHeight());
        glTexCoord2d(1, 0);
        glVertex2d(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
        glTexCoord2d(1, 1);
        glVertex2d(scaledResolution.getScaledWidth(), 0);
        glEnd();
        glUseProgram(0);
    }
}
