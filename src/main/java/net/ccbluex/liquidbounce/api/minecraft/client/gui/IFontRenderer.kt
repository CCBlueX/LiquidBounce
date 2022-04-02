/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.gui

import net.ccbluex.liquidbounce.ui.font.GameFontRenderer

interface IFontRenderer {
    val fontHeight: Int

    fun getStringWidth(str: String): Int
    fun drawString(str: String, x: Int, y: Int, color: Int): Int
    fun drawString(str: String, x: Float, y: Float, color: Int, shadow: Boolean): Int
    fun drawCenteredString(text: String, x: Float, y: Float, color: Int): Int
    fun drawCenteredString(text: String, x: Float, y: Float, color: Int, shadow: Boolean): Int
    fun drawStringWithShadow(text: String, x: Int, y: Int, color: Int): Int
    fun isGameFontRenderer(): Boolean
    fun getGameFontRenderer(): GameFontRenderer
    fun drawString(str: String, x: Float, y: Float, color: Int): Int
}