/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3i
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.util.EnumFacing

class EnumFacingImpl(val wrapped: EnumFacing) : IEnumFacing
{
	override val opposite: IEnumFacing
		get() = wrapped.opposite.wrap()
	override val directionVec: WVec3i
		get() = wrapped.directionVec.wrap()
	override val axisOrdinal: Int
		get() = wrapped.axis.ordinal

	override fun isNorth(): Boolean = wrapped == EnumFacing.NORTH

	override fun isSouth(): Boolean = wrapped == EnumFacing.SOUTH

	override fun isEast(): Boolean = wrapped == EnumFacing.EAST

	override fun isWest(): Boolean = wrapped == EnumFacing.WEST

	override fun isUp(): Boolean = wrapped == EnumFacing.UP

	override fun equals(other: Any?): Boolean = other is EnumFacingImpl && other.wrapped == wrapped

	override fun toString(): String = wrapped.name2
}

fun IEnumFacing.unwrap(): EnumFacing = (this as EnumFacingImpl).wrapped
fun EnumFacing.wrap(): IEnumFacing = EnumFacingImpl(this)
