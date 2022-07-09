/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.event

import net.ccbluex.liquidbounce.api.minecraft.event.IClickEvent
import net.minecraft.event.ClickEvent

class ClickEventImpl(val wrapped: ClickEvent) : IClickEvent
{
	override fun equals(other: Any?): Boolean = other is ClickEventImpl && other.wrapped == wrapped
}

fun IClickEvent.unwrap(): ClickEvent = (this as ClickEventImpl).wrapped
fun ClickEvent.wrap(): IClickEvent = ClickEventImpl(this)
