/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.injection.backend.utils.FontRendererWrapper
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.minecraft.client.gui.FontRenderer

class FontRendererImpl(val wrapped: FontRenderer) : IFontRenderer {
    override val fontHeight: Int
        get() = wrapped.FONT_HEIGHT

    override fun getStringWidth(str: String): Int = wrapped.getStringWidth(str)

    override fun drawString(str: String, x: Int, y: Int, color: Int) = wrapped.drawString(str, x, y, color)
    override fun drawString(str: String, x: Float, y: Float, color: Int) = wrapped.drawString(str, x.toInt(), y.toInt(), color)

    override fun drawString(str: String, x: Float, y: Float, color: Int, shadow: Boolean) = wrapped.drawString(str, x, y, color, shadow)

    override fun drawCenteredString(text: String, x: Float, y: Float, color: Int) = drawString(text, x - getStringWidth(text) / 2F, y, color)

    override fun drawCenteredString(text: String, x: Float, y: Float, color: Int, shadow: Boolean) = drawString(text, x - getStringWidth(text) / 2F, y, color, shadow)

    override fun drawStringWithShadow(text: String, x: Int, y: Int, color: Int) = wrapped.drawStringWithShadow(text, x.toFloat(), y.toFloat(), color)

    override fun isGameFontRenderer(): Boolean = wrapped is FontRendererWrapper

    override fun getGameFontRenderer(): GameFontRenderer = (wrapped as FontRendererWrapper).wrapped as GameFontRenderer

    override fun equals(other: Any?): Boolean {
        return other is FontRendererImpl && other.wrapped == this.wrapped
    }
}

inline fun IFontRenderer.unwrap(): FontRenderer = (this as FontRendererImpl).wrapped
inline fun FontRenderer.wrap(): IFontRenderer = FontRendererImpl(this)