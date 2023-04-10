/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;

import java.awt.Color;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUseProgram;

/**
 * @author TheSlowly, Navex
 */
public abstract class FramebufferShader extends Shader {

    private static Framebuffer framebuffer;

    protected float red, green, blue, alpha = 1F;
    protected int radius = 5;
    protected int fade = 10;
    protected float renderScale = 1f;
    protected float targetAlpha = 0f;

    private boolean entityShadows;

    public FramebufferShader(String fragmentShader) {
        super(fragmentShader);
    }

    public void startDraw(float partialTicks, float renderScale) {
        this.renderScale = renderScale;

        pushMatrix();
        enableAlpha();
        pushAttrib();

        framebuffer = setupFrameBuffer(framebuffer, renderScale);
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(true);
        entityShadows = mc.gameSettings.entityShadows;
        mc.gameSettings.entityShadows = false;
        mc.entityRenderer.setupCameraTransform(partialTicks, 0);
    }

    public void stopDraw(Color color, int radius, int fade, float targetAlpha) {
        mc.gameSettings.entityShadows = entityShadows;
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        mc.getFramebuffer().bindFramebuffer(true);

        red = color.getRed() / 255F;
        green = color.getGreen() / 255F;
        blue = color.getBlue() / 255F;
        alpha = color.getAlpha() / 255F;
        this.radius = radius;
        this.fade = fade;
        this.targetAlpha = targetAlpha;

        mc.entityRenderer.disableLightmap();
        RenderHelper.disableStandardItemLighting();

        startShader();
        mc.entityRenderer.setupOverlayRendering();
        drawFramebuffer(framebuffer);
        stopShader();

        mc.entityRenderer.disableLightmap();

        popMatrix();
        popAttrib();
    }

    /**
     * @author TheSlowly, Navex
     */
    public Framebuffer setupFrameBuffer(Framebuffer frameBuffer, float renderScale) {
        if(frameBuffer != null)
            frameBuffer.deleteFramebuffer();

        frameBuffer = new Framebuffer((int)(mc.displayWidth * renderScale), (int)(mc.displayHeight * renderScale), true);

        return frameBuffer;
    }

    /**
     * @author Navex
     */
    public void drawFramebuffer(Framebuffer framebuffer) {
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        double scaledWidth = scaledResolution.getScaledWidth_double();
        double scaledHeight = scaledResolution.getScaledHeight_double();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();

        glBindTexture(GL_TEXTURE_2D, framebuffer.framebufferTexture);
        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0.0, 0.0, 1.0).tex(0.0, 1.0).endVertex();
        buffer.pos(0.0, scaledHeight, 1.0).tex(0.0, 0.0).endVertex();
        buffer.pos(scaledWidth, scaledHeight, 1.0).tex(1.0, 0.0).endVertex();
        buffer.pos(scaledWidth, 0.0, 0.0).tex(1.0, 1.0).endVertex();
        tessellator.draw();
        glUseProgram(0);
    }
}
