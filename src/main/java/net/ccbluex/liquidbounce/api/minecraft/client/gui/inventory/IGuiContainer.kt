/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.inventory.ISlot

interface IGuiContainer : IGuiScreen {
    fun handleMouseClick(slot: ISlot, slotNumber: Int, clickedButton: Int, clickType: Int)

    val inventorySlots: IContainer?
}