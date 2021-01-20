/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.inventory

interface IContainer
{
	val windowId: Int
	val inventorySlots: List<ISlot>

	fun getSlot(id: Int): ISlot
}
