/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.injection.backend.WrapperImpl.classProvider
import net.ccbluex.liquidbounce.utils.extensions.canBeClicked

data class SearchInfo(val type: String, val eyesPos: WVec3, val raytraceEnd: WVec3, val expectedHitVec: WVec3?, val actualHitVec: WVec3, val delta: Double, val expectedPlacePos: WBlockPos)

class PlaceInfo(val blockPos: WBlockPos, val enumFacing: IEnumFacing, var vec3: WVec3 = WVec3(blockPos) + 0.5)
{
	companion object
	{
		/**
		 * Allows you to find a specific place info for your [blockPos]
		 */
		@JvmStatic
		operator fun get(theWorld: IWorld, blockPos: WBlockPos): PlaceInfo?
		{
			val provider = classProvider

			return when
			{
				theWorld.canBeClicked(blockPos.add(0, -1, 0)) -> PlaceInfo(blockPos.add(0, -1, 0), provider.getEnumFacing(EnumFacingType.UP))
				theWorld.canBeClicked(blockPos.add(0, 0, 1)) -> PlaceInfo(blockPos.add(0, 0, 1), provider.getEnumFacing(EnumFacingType.NORTH))
				theWorld.canBeClicked(blockPos.add(-1, 0, 0)) -> PlaceInfo(blockPos.add(-1, 0, 0), provider.getEnumFacing(EnumFacingType.EAST))
				theWorld.canBeClicked(blockPos.add(0, 0, -1)) -> PlaceInfo(blockPos.add(0, 0, -1), provider.getEnumFacing(EnumFacingType.SOUTH))
				theWorld.canBeClicked(blockPos.add(1, 0, 0)) -> PlaceInfo(blockPos.add(1, 0, 0), provider.getEnumFacing(EnumFacingType.WEST))
				else -> null
			}
		}
	}
}
