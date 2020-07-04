/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.gui

interface IGui {
    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
    fun handleMouseInput()
    fun keyTyped(typedChar: Char, keyCode: Int)

    var height: Int
    var width: Int
    var top: Int
    var bottom: Int
    var left: Int
    var right: Int

}