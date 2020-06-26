/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.gui

import net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory.IGuiContainer

interface IGuiScreen {
    val height: Int
    val width: Int

    fun asGuiContainer(): IGuiContainer
    fun asGuiGameOver(): IGuiGameOver

    // Non-virtual calls. Used for GuiScreen-Wrapping
    fun superInitGui()
    fun superMouseReleased(mouseX: Int, mouseY: Int, state: Int)
}