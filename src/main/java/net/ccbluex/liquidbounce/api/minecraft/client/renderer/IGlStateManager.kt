package net.ccbluex.liquidbounce.api.minecraft.client.renderer

interface IGlStateManager {
    fun bindTexture(textureID: Int)
    fun resetColor()
    fun enableTexture2D()
    fun enableBlend()
    fun tryBlendFuncSeparate(glSrcAlpha: Int, glOneMinusSrcAlpha: Int, glOne: Int, glZero: Int)
    fun disableTexture2D()
    fun disableBlend()
    fun enableAlpha()
    fun disableLighting()
    fun disableCull()
    fun enableColorMaterial()
    fun disableRescaleNormal()
    fun pushMatrix()
    fun pushAttrib()
    fun popMatrix()
    fun popAttrib()
}