/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d


class PlaceInfo(val blockPos: BlockPos, val Direction: Direction,
                var Vec3d: Vec3d = Vec3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)) {

    companion object {

        /**
         * Allows you to find a specific place info for your [blockPos]
         */
        fun get(blockPos: BlockPos) =
            when {
                canBeClicked(blockPos.add(0, -1, 0)) ->
                    PlaceInfo(blockPos.add(0, -1, 0), Direction.UP)
                canBeClicked(blockPos.add(0, 0, 1)) ->
                    PlaceInfo(blockPos.add(0, 0, 1), Direction.NORTH)
                canBeClicked(blockPos.add(-1, 0, 0)) ->
                    PlaceInfo(blockPos.add(-1, 0, 0), Direction.EAST)
                canBeClicked(blockPos.add(0, 0, -1)) ->
                    PlaceInfo(blockPos.add(0, 0, -1), Direction.SOUTH)
                canBeClicked(blockPos.add(1, 0, 0)) ->
                    PlaceInfo(blockPos.add(1, 0, 0), Direction.WEST)
                else -> null
            }
    }
}