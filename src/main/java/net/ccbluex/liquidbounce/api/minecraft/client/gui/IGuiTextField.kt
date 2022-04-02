/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.gui

interface IGuiTextField : IGui {
    val xPosition: Int
    var text: String
    var isFocused: Boolean
    var maxStringLength: Int

    fun updateCursorCounter()
    fun textboxKeyTyped(typedChar: Char, keyCode: Int): Boolean
    fun drawTextBox()
    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
    fun keyTyped(typedChar: Char, keyCode: Int): Boolean
}