package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.minecraft.client.gui.FontRenderer

val FontRenderer.serialized: String?
    get() = if (this is GameFontRenderer) "${this.defaultFont}" else if (this == Fonts.minecraftFont) "Minecraft" else Fonts.getFontDetails(this)?.let { "$it" }

fun FontRenderer.drawStringWithShadow(text: String?, x: Int, y: Int, color: Int): Int = this.drawString(text, x.toFloat(), y.toFloat(), color, true)

fun FontRenderer.drawString(text: String?, x: Float, y: Float, color: Int): Int = this.drawString(text, x, y, color, false)
