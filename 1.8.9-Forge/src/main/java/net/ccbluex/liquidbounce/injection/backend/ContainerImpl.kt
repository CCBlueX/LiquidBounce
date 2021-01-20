/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.inventory.ISlot
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot

class ContainerImpl(val wrapped: Container) : IContainer
{
	override val windowId: Int
		get() = wrapped.windowId
	override val inventorySlots: List<ISlot>
		get() = wrapped.inventorySlots.map(Slot::wrap)

	override fun getSlot(id: Int): ISlot = wrapped.getSlot(id).wrap()

	override fun equals(other: Any?): Boolean = other is ContainerImpl && other.wrapped == wrapped
}

inline fun IContainer.unwrap(): Container = (this as ContainerImpl).wrapped
inline fun Container.wrap(): IContainer = ContainerImpl(this)
