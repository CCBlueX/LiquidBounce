/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory.IGuiRepair
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.inventory.ISlot
import net.minecraft.client.gui.GuiRepair

class GuiRepairImpl<out T : GuiRepair>(wrapped: T) : GuiScreenImpl<T>(wrapped), IGuiRepair
{
	override val inventorySlots: IContainer?
		get() = wrapped.inventorySlots?.wrap()

	override fun handleMouseClick(slot: ISlot, slotNumber: Int, clickedButton: Int, clickType: Int) = wrapped.handleMouseClick(slot.unwrap(), slotNumber, clickedButton, clickType)
	override fun highlight(slotNumber: Int, length: Long, color: Int)
	{
		asGuiContainer().highlight(slotNumber, length, color)
	}
}

fun IGuiRepair.unwrap(): GuiRepair = (this as GuiRepairImpl<*>).wrapped
fun GuiRepair.wrap(): IGuiRepair = GuiRepairImpl(this)
