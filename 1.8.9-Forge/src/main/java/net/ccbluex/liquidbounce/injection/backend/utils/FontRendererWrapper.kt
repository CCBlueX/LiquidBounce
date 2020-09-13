/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.utils

import net.ccbluex.liquidbounce.api.util.IWrappedFontRenderer
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.util.ResourceLocation

class FontRendererWrapper(val wrapped: IWrappedFontRenderer): FontRenderer(Minecraft.getMinecraft().gameSettings,
        ResourceLocation("textures/font/ascii.png"),
//        if (ClassUtils.hasForge()) null else
        // TODO: Fix this
            Minecraft.getMinecraft().textureManager
        , false) {

    override fun drawString(text: String?, x: Int, y: Int, color: Int): Int = wrapped.drawString(text, x.toFloat(), y.toFloat(), color)

    override fun drawString(text: String?, x: Float, y: Float, color: Int, dropShadow: Boolean): Int = wrapped.drawString(text, x, y, color, dropShadow)

    override fun drawStringWithShadow(text: String?, x: Float, y: Float, color: Int): Int = wrapped.drawStringWithShadow(text, x, y, color)

    override fun getColorCode(character: Char): Int = wrapped.getColorCode(character)

    override fun getStringWidth(text: String?): Int = wrapped.getStringWidth(text)

    override fun getCharWidth(character: Char): Int = wrapped.getCharWidth(character)
}