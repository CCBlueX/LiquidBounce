/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.injection.backend.WrapperImpl.classProvider
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import kotlin.math.floor

class WBlockPos(x: Int, y: Int, z: Int) : WVec3i(x, y, z)
{
	companion object
	{
		val ORIGIN: WBlockPos = WBlockPos(0, 0, 0)
	}

	constructor(x: Double, y: Double, z: Double) : this(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())

	constructor(source: IEntity) : this(source.posX, source.posY, source.posZ)

	/**
	 * Add the given coordinates to the coordinates of this BlockPos
	 */
	fun add(x: Int, y: Int, z: Int): WBlockPos = if (x == 0 && y == 0 && z == 0) this else WBlockPos(this.x + x, this.y + y, this.z + z)

	/**
	 * Add the given coordinates to the coordinates of this BlockPos
	 */
	fun add(x: Double, y: Double, z: Double): WBlockPos = if (x == 0.0 && y == 0.0 && z == 0.0) this else WBlockPos(this.x + x, this.y + y, this.z + z)

	@JvmOverloads
	fun offset(side: IEnumFacing, n: Int = 1): WBlockPos = if (n == 0) this else WBlockPos(x + side.directionVec.x * n, y + side.directionVec.y * n, z + side.directionVec.z * n)

	fun up(): WBlockPos = this.up(1)

	fun up(n: Int): WBlockPos = offset(classProvider.getEnumFacing(EnumFacingType.UP), n)

	fun down(): WBlockPos = this.down(1)

	fun down(n: Int): WBlockPos = offset(classProvider.getEnumFacing(EnumFacingType.DOWN), n)

	fun west(): WBlockPos = this.west(1)

	fun west(n: Int): WBlockPos = offset(classProvider.getEnumFacing(EnumFacingType.WEST), n)

	fun east(): WBlockPos = this.east(1)

	fun east(n: Int): WBlockPos = offset(classProvider.getEnumFacing(EnumFacingType.EAST), n)

	fun north(): WBlockPos = this.north(1)

	fun north(n: Int): WBlockPos = offset(classProvider.getEnumFacing(EnumFacingType.NORTH), n)

	fun south(): WBlockPos = this.south(1)

	fun south(n: Int): WBlockPos = offset(classProvider.getEnumFacing(EnumFacingType.SOUTH), n)

	fun getBlock() = BlockUtils.getBlock(this)
}
