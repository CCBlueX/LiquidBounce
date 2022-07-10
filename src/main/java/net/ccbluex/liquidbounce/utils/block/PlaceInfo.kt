/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.utils.extensions.canBeClicked
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraft.world.World

data class SearchInfo(val type: String, val eyesPos: Vec3, val raytraceEnd: Vec3, val expectedHitVec: Vec3?, val actualHitVec: Vec3, val delta: Double, val expectedPlacePos: BlockPos)

class PlaceInfo(val blockPos: BlockPos, val enumFacing: EnumFacing, var vec3: Vec3 = Vec3(blockPos) + 0.5)
{
    companion object
    {
        /**
         * Allows you to find a specific place info for your [blockPos]
         */
        @JvmStatic
        operator fun get(theWorld: World, blockPos: BlockPos): PlaceInfo?
        {
            return when
            {
                theWorld.canBeClicked(blockPos.add(0, -1, 0)) -> PlaceInfo(blockPos.add(0, -1, 0), EnumFacing.UP)
                theWorld.canBeClicked(blockPos.add(0, 0, 1)) -> PlaceInfo(blockPos.add(0, 0, 1), EnumFacing.NORTH)
                theWorld.canBeClicked(blockPos.add(-1, 0, 0)) -> PlaceInfo(blockPos.add(-1, 0, 0), EnumFacing.EAST)
                theWorld.canBeClicked(blockPos.add(0, 0, -1)) -> PlaceInfo(blockPos.add(0, 0, -1), EnumFacing.SOUTH)
                theWorld.canBeClicked(blockPos.add(1, 0, 0)) -> PlaceInfo(blockPos.add(1, 0, 0), EnumFacing.WEST)
                else -> null
            }
        }
    }
}
