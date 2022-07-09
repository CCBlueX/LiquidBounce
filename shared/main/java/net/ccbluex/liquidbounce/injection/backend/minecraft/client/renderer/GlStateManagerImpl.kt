package net.ccbluex.liquidbounce.injection.backend.minecraft.client.renderer

import net.ccbluex.liquidbounce.api.minecraft.client.renderer.IGlStateManager
import net.minecraft.client.renderer.GlStateManager

object GlStateManagerImpl : IGlStateManager
{
    override fun bindTexture(textureID: Int) = GlStateManager.bindTexture(textureID)
    override fun resetColor() = GlStateManager.resetColor()

    override fun enableTexture2D() = GlStateManager.enableTexture2D()

    override fun enableBlend() = GlStateManager.enableBlend()

    override fun tryBlendFuncSeparate(glSrcAlpha: Int, glOneMinusSrcAlpha: Int, glOne: Int, glZero: Int) = GlStateManager.tryBlendFuncSeparate(glSrcAlpha, glOneMinusSrcAlpha, glOne, glZero)

    override fun disableTexture2D() = GlStateManager.disableTexture2D()

    override fun disableBlend() = GlStateManager.disableBlend()

    override fun enableAlpha() = GlStateManager.enableAlpha()

    override fun disableLighting() = GlStateManager.disableLighting()

    override fun disableCull() = GlStateManager.disableCull()

    override fun disableAlpha() = GlStateManager.disableAlpha()

    override fun enableColorMaterial() = GlStateManager.enableColorMaterial()

    override fun enableRescaleNormal() = GlStateManager.enableRescaleNormal()

    override fun disableRescaleNormal() = GlStateManager.disableRescaleNormal()

    override fun pushMatrix() = GlStateManager.pushMatrix()

    override fun pushAttrib() = GlStateManager.pushAttrib()

    override fun popMatrix() = GlStateManager.popMatrix()

    override fun popAttrib() = GlStateManager.popAttrib()

    override fun enablePolygonOffset() = GlStateManager.enablePolygonOffset()

    override fun doPolygonOffset(factor: Float, units: Float) = GlStateManager.doPolygonOffset(factor, units)

    override fun disablePolygonOffset() = GlStateManager.disablePolygonOffset()

    override fun disableDepth() = GlStateManager.disableDepth()

    override fun enableDepth() = GlStateManager.enableDepth()

    override fun enableLighting() = GlStateManager.enableLighting()
}
