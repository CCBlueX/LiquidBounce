/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.gui

interface IFontRenderer {
    val fontHeight: Float

    fun getStringWidth(str: String): Int
    fun drawString(str: String, x: Float, y: Float, color: Int, shadow: Boolean)
}