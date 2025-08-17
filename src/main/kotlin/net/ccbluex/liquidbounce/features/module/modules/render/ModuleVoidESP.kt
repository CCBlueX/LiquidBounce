/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.longs.LongSets
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.Region
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.block.SideShapeType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.chunk.Chunk

/**
 * VoidESP module
 *
 * Highlight all surrounding positions that may be void.
 */

object ModuleVoidESP : ClientModule("VoidESP", Category.RENDER) {

    override val baseKey: String
        get() = "liquidbounce.module.voidEsp"

    private val yThreshold by int("YThreshold", 16, 3..32)
    private val rangeSide by int("RangeSide", 3, 0..32)
    private val rangeFacing by int("RangeFacing", 8, 1..32)

    private val renderer = tree(
        PlacementRenderer("Render", true, this, keep = true,
            defaultColor = Color4b(255, 0, 0, 90)
        )
    )

    private var lastTickPositions: LongSet = LongSets.EMPTY_SET

    override fun onDisabled() {
        lastTickPositions = LongSets.EMPTY_SET
        renderer.clearSilently()
    }

    private val posStart = BlockPos.Mutable()
    private val posEnd = BlockPos.Mutable()

    private fun Chunk.canBlockStandOn(pos: BlockPos): Boolean {
        return this.getBlockState(pos).isSideSolid(this, pos, Direction.UP, SideShapeType.CENTER)
    }

    /**
     * Search positions around.
     */
    private fun search(): LongSet {
        val positions = LongOpenHashSet()

        // Find the first place where the player can stand
        val startPos = posEnd.set(player.blockPos, Direction.DOWN)
        val yThreshold = yThreshold
        var chunk = world.getChunk(startPos)
        var flag = false

        var i = 0
        while (i++ < yThreshold) {
            if (chunk.canBlockStandOn(startPos)) {
                flag = true
                break
            }
            startPos.y--
        }

        if (!flag) {
            return LongSets.EMPTY_SET
        }

        val facing = player.horizontalFacing
        val side = facing.rotateYClockwise()

        val from = posStart.set(startPos)
            .move(facing, rangeFacing).move(side.opposite, rangeSide)
        val to = posEnd.set(startPos)
            .move(facing.opposite, rangeFacing).move(side, rangeSide)

        Region(from, to).forEach {
            chunk = world.getChunk(it)

            if (chunk.canBlockStandOn(it)) {
                return@forEach
            }

            posStart.set(it)

            repeat(yThreshold) { _ ->
                posStart.y--

                if (chunk.canBlockStandOn(posStart)) {
                    return@forEach
                }

                // Reach the bottom(void)
                if (posStart.y <= chunk.bottomY) {
                    positions.add(it.asLong())
                    return@forEach
                }
            }

            positions.add(it.asLong())
        }

        return positions
    }

    private val tickHandler = handler<PlayerTickEvent> {
        val positions = search()

        // Invalidate last tick positions
        with(lastTickPositions.longIterator()) {
            while (hasNext()) {
                val longValue = nextLong()
                if (longValue !in positions) {
                    renderer.removeBlock(posStart.set(longValue))
                }
            }
        }

        // Add this tick positions
        with(positions.longIterator()) {
            while (hasNext()) {
                val longValue = nextLong()
                renderer.addBlock(posStart.set(longValue))
            }
        }

        lastTickPositions = positions

        // Update cull data
        renderer.updateAll()
    }

}
