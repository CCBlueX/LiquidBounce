package net.ccbluex.liquidbounce.api.minecraft.client.renderer

interface IGlStateManager
{
    fun bindTexture(textureID: Int)
    fun resetColor()
    fun tryBlendFuncSeparate(glSrcAlpha: Int, glOneMinusSrcAlpha: Int, glOne: Int, glZero: Int)
    fun pushMatrix()
    fun pushAttrib()
    fun popMatrix()
    fun popAttrib()
    fun doPolygonOffset(factor: Float, units: Float)

    // Disable
    fun disableTexture2D()
    fun disableBlend()
    fun disableLighting()
    fun disableRescaleNormal()
    fun disablePolygonOffset()
    fun disableDepth()
    fun disableCull()
    fun disableAlpha()

    // Enable
    fun enableTexture2D()
    fun enableBlend()
    fun enableLighting()
    fun enableRescaleNormal()
    fun enablePolygonOffset()
    fun enableDepth()
    fun enableColorMaterial()
    fun enableAlpha()
}
