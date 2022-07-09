/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.BlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.Vec3
import net.ccbluex.liquidbounce.api.minecraft.world.World

import net.ccbluex.liquidbounce.utils.extensions.canBeClicked

data class SearchInfo(val type: String, val eyesPos: Vec3, val raytraceEnd: Vec3, val expectedHitVec: Vec3?, val actualHitVec: Vec3, val delta: Double, val expectedPlacePos: BlockPos)

class PlaceInfo(val blockPos: BlockPos, val enumFacing: IEnumFacing, var vec3: Vec3 = Vec3(blockPos) + 0.5)
{
    companion object
    {
        /**
         * Allows you to find a specific place info for your [blockPos]
         */
        @JvmStatic
        operator fun get(theWorld: World, blockPos: BlockPos): PlaceInfo?
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
