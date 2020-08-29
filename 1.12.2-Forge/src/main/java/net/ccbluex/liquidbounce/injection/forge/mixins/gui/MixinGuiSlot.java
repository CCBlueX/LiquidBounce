/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.injection.implementations.IMixinGuiSlot;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiSlot.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiSlot implements IMixinGuiSlot {
    @Shadow
    public int left;
    @Shadow
    public int top;
    @Shadow
    public int width;
    @Shadow
    public int right;
    @Shadow
    public int bottom;
    @Shadow
    public int height;
    @Shadow
    protected int mouseX;
    @Shadow
    protected int mouseY;
    @Shadow
    protected float amountScrolled;
    @Shadow
    protected boolean hasListHeader;
    @Shadow
    @Final
    protected Minecraft mc;
    @Shadow
    protected boolean visible;
    private int listWidth = 220;
    private boolean enableScissor = false;

    @Shadow
    protected abstract void drawBackground();

    @Shadow
    protected abstract void bindAmountScrolled();

    @Shadow
    protected abstract void drawListHeader(int p_148129_1_, int p_148129_2_, Tessellator p_148129_3_);

    @Shadow
    protected abstract int getContentHeight();

    @Shadow
    protected abstract void drawSelectionBox(int p_drawSelectionBox_1_, int p_drawSelectionBox_2_, int p_drawSelectionBox_3_, int p_drawSelectionBox_4_, float p_drawSelectionBox_5_);

    @Shadow
    protected abstract void overlayBackground(int p_overlayBackground_1_, int p_overlayBackground_2_, int p_overlayBackground_3_, int p_overlayBackground_4_);

    @Shadow
    public abstract int getMaxScroll();

    @Shadow
    protected abstract void drawContainerBackground(Tessellator p_drawContainerBackground_1_);

    @Shadow
    protected abstract void renderDecorations(int p_renderDecorations_1_, int p_renderDecorations_2_);

    /**
     * @author CCBlueX
     */
    @Overwrite
    public void drawScreen(int mouseXIn, int mouseYIn, float partialTicks) {
        if (this.visible) {
            this.mouseX = mouseXIn;
            this.mouseY = mouseYIn;
            this.drawBackground();
            int i = this.getScrollBarX();
            int j = i + 6;
            this.bindAmountScrolled();
            GlStateManager.disableLighting();
            GlStateManager.disableFog();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            // Forge: background rendering moved into separate method.
//            this.drawContainerBackground(tessellator);
            int k = this.left + this.width / 2 - this.getListWidth() / 2 + 2;
            int l = this.top + 4 - (int) this.amountScrolled;

            if (this.hasListHeader) {
                this.drawListHeader(k, l, tessellator);
            }

            RenderUtils.makeScissorBox(left, top, right, bottom);

            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            this.drawSelectionBox(k, l, mouseXIn, mouseYIn, partialTicks);

            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            ScaledResolution scaledResolution = new ScaledResolution(mc);

            GlStateManager.disableDepth();
            Gui.drawRect(0, 0, scaledResolution.getScaledWidth(), this.top, Integer.MIN_VALUE);
            Gui.drawRect(0, this.bottom, scaledResolution.getScaledWidth(), this.height, Integer.MIN_VALUE);

            GL11.glEnable(GL11.GL_BLEND);
            GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
            GlStateManager.disableAlpha();
            GlStateManager.shadeModel(7425);
            GlStateManager.disableTexture2D();
            int i1 = 4;
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
            bufferbuilder.pos(this.left, this.top + i1, 0.0D).color(0, 0, 0, 0).endVertex();
            bufferbuilder.pos(this.right, this.top + i1, 0.0D).color(0, 0, 0, 0).endVertex();
            bufferbuilder.pos(this.right, this.top, 0.0D).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos(this.left, this.top, 0.0D).color(0, 0, 0, 255).endVertex();
            tessellator.draw();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
            bufferbuilder.pos(this.left, this.bottom, 0.0D).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos(this.right, this.bottom, 0.0D).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos(this.right, this.bottom - i1, 0.0D).color(0, 0, 0, 0).endVertex();
            bufferbuilder.pos(this.left, this.bottom - i1, 0.0D).color(0, 0, 0, 0).endVertex();
            tessellator.draw();
            int j1 = this.getMaxScroll();

            if (j1 > 0) {
                int k1 = (this.bottom - this.top) * (this.bottom - this.top) / this.getContentHeight();
                k1 = MathHelper.clamp(k1, 32, this.bottom - this.top - 8);
                int l1 = (int) this.amountScrolled * (this.bottom - this.top - k1) / j1 + this.top;

                if (l1 < this.top) {
                    l1 = this.top;
                }

                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos(i, this.bottom, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(j, this.bottom, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(j, this.top, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(i, this.top, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 255).endVertex();
                tessellator.draw();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos(i, l1 + k1, 0.0D).tex(0.0D, 1.0D).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos(j, l1 + k1, 0.0D).tex(1.0D, 1.0D).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos(j, l1, 0.0D).tex(1.0D, 0.0D).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos(i, l1, 0.0D).tex(0.0D, 0.0D).color(128, 128, 128, 255).endVertex();
                tessellator.draw();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos(i, l1 + k1 - 1, 0.0D).tex(0.0D, 1.0D).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos(j - 1, l1 + k1 - 1, 0.0D).tex(1.0D, 1.0D).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos(j - 1, l1, 0.0D).tex(1.0D, 0.0D).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos(i, l1, 0.0D).tex(0.0D, 0.0D).color(192, 192, 192, 255).endVertex();
                tessellator.draw();
            }

            this.renderDecorations(mouseXIn, mouseYIn);
            GlStateManager.enableTexture2D();
            GlStateManager.shadeModel(7424);
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
        }
    }
    //    @Overwrite
//    public void drawScreen(int mouseXIn, int mouseYIn, float p_148128_3_) {
//        if (this.visible) {
//            this.mouseX = mouseXIn;
//            this.mouseY = mouseYIn;
//            this.drawBackground();
//            int i = this.getScrollBarX();
//            int j = i + 6;
//            this.bindAmountScrolled();
//            GlStateManager.disableLighting();
//            GlStateManager.disableFog();
//            Tessellator tessellator = Tessellator.getInstance();
//            BufferBuilder worldrenderer = tessellator.getBuffer();
//            this.drawContainerBackground(tessellator);
//            int k = this.left + this.width / 2 - this.getListWidth() / 2 + 2;
//            int l = this.top + 4 - (int) this.amountScrolled;
//            if (this.hasListHeader) {
//                this.drawListHeader(k, l, tessellator);
//            }
//
//            boolean enableScissor = this.enableScissor;
//
//            if (enableScissor) {
//                RenderUtils.makeScissorBox(left, top, right, bottom);
//
//                GL11.glEnable(GL11.GL_SCISSOR_TEST);
//            }
//
//            this.drawSelectionBox(k, l, mouseXIn, mouseYIn, p_148128_3_);
//
//            if (enableScissor)
//                GL11.glDisable(GL11.GL_SCISSOR_TEST);
//
//            GlStateManager.disableDepth();
//            ScaledResolution scaledResolution = new ScaledResolution(mc);
//            Gui.drawRect(0, 0, scaledResolution.getScaledWidth(), this.top, Integer.MIN_VALUE);
//            Gui.drawRect(0, this.bottom, scaledResolution.getScaledWidth(), this.height, Integer.MIN_VALUE);
//
//            GL11.glEnable(GL11.GL_BLEND);
//            GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
//            GlStateManager.disableAlpha();
//            GlStateManager.shadeModel(7425);
//            GlStateManager.disableTexture2D();
//
//            int i1 = 1;
//
//            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
//            worldrenderer.pos(this.left, this.top + i1, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 0).endVertex();
//            worldrenderer.pos(this.right, this.top + i1, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 0).endVertex();
//            worldrenderer.pos(this.right, this.top, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 255).endVertex();
//            worldrenderer.pos(this.left, this.top, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 255).endVertex();
//            tessellator.draw();
//            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
//            worldrenderer.pos(this.left, this.bottom, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 255).endVertex();
//            worldrenderer.pos(this.right, this.bottom, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
//            worldrenderer.pos(this.right, this.bottom - i1, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 0).endVertex();
//            worldrenderer.pos(this.left, this.bottom - i1, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 0).endVertex();
//            tessellator.draw();
//            int j1 = this.getMaxScroll();
//            if (j1 > 0) {
//                int k1 = (this.bottom - this.top) * (this.bottom - this.top) / this.getContentHeight();
//                k1 = MathHelper.clamp(k1, 32, this.bottom - this.top - 8);
//                int l1 = (int) this.amountScrolled * (this.bottom - this.top - k1) / j1 + this.top;
//                if (l1 < this.top) {
//                    l1 = this.top;
//                }
//
//                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
//                worldrenderer.pos(i, this.bottom, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 255).endVertex();
//                worldrenderer.pos(j, this.bottom, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
//                worldrenderer.pos(j, this.top, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 255).endVertex();
//                worldrenderer.pos(i, this.top, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 255).endVertex();
//                tessellator.draw();
//                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
//                worldrenderer.pos(i, l1 + k1, 0.0D).tex(0.0D, 1.0D).color(128, 128, 128, 255).endVertex();
//                worldrenderer.pos(j, l1 + k1, 0.0D).tex(1.0D, 1.0D).color(128, 128, 128, 255).endVertex();
//                worldrenderer.pos(j, l1, 0.0D).tex(1.0D, 0.0D).color(128, 128, 128, 255).endVertex();
//                worldrenderer.pos(i, l1, 0.0D).tex(0.0D, 0.0D).color(128, 128, 128, 255).endVertex();
//                tessellator.draw();
//                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
//                worldrenderer.pos(i, l1 + k1 - 1, 0.0D).tex(0.0D, 1.0D).color(192, 192, 192, 255).endVertex();
//                worldrenderer.pos(j - 1, l1 + k1 - 1, 0.0D).tex(1.0D, 1.0D).color(192, 192, 192, 255).endVertex();
//                worldrenderer.pos(j - 1, l1, 0.0D).tex(1.0D, 0.0D).color(192, 192, 192, 255).endVertex();
//                worldrenderer.pos(i, l1, 0.0D).tex(0.0D, 0.0D).color(192, 192, 192, 255).endVertex();
//                tessellator.draw();
//            }
//
//            this.renderDecorations(mouseXIn, mouseYIn);
//
//            GlStateManager.enableTexture2D();
//            GlStateManager.shadeModel(7424);
//            GlStateManager.enableAlpha();
//            GlStateManager.disableBlend();
//        }
//    }

    /**
     * @author CCBlueX
     */
    @Overwrite
    protected int getScrollBarX() {
        return this.width - 5;
    }

    @Override
    public void setEnableScissor(boolean enableScissor) {
        this.enableScissor = enableScissor;
    }

    /**
     * @author CCBlueX (superblaubeere27)
     */
    @Overwrite
    public int getListWidth() {
        return this.listWidth;
    }

    @Override
    public void setListWidth(int listWidth) {
        this.listWidth = listWidth;
    }

}