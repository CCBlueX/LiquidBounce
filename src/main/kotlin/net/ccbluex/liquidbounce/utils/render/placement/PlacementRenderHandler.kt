/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.render.placement

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.utils.block.scanBlocksInCuboid
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

class PlacementRenderHandler(private val placementRenderer: PlacementRenderer, val id: Int = 0) {

    private val inList = linkedMapOf<BlockPos, Triple<Long, Long, Box>>()
    private val currentList = linkedMapOf<BlockPos, Pair<Long, Box>>()
    private val outList = linkedMapOf<BlockPos, Triple<Long, Long, Box>>()

    fun render(event: WorldRenderEvent, time: Long) {
        val matrixStack = event.matrixStack

        with(placementRenderer) {
            renderEnvironmentForWorld(matrixStack) {
                BoxRenderer.drawWith(this) {
                    fun drawEntryBox(blockPos: BlockPos, cullData: Long, box: Box, colorFactor: Float) {
                        val vec3d = Vec3d(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble())
                        withPositionRelativeToCamera(vec3d) {
                            drawBox(
                                box,
                                getColor(id).fade(colorFactor),
                                getOutlineColor(id).fade(colorFactor),
                                (cullData shr 32).toInt(),
                                (cullData and 0xFFFFFFFF).toInt()
                            )
                        }
                    }

                    inList.iterator().apply {
                        while (hasNext()) {
                            val entry = next()

                            val sizeFactor = startSizeCurve.getFactor(entry.value.first, time, inTime.toFloat())
                            val expand = MathHelper.lerp(sizeFactor, startSize, 1f)
                            val box = getBox(expand, entry.value.third)
                            val colorFactor = fadeInCurve.getFactor(entry.value.first, time, inTime.toFloat())

                            drawEntryBox(entry.key, entry.value.second, box, colorFactor)

                            if (time - entry.value.first >= outTime) {
                                if (keep) {
                                    currentList[entry.key] = entry.value.second to entry.value.third
                                } else {
                                    outList[entry.key] = Triple(time, entry.value.second, entry.value.third)
                                }
                                remove()
                            }
                        }
                    }

                    currentList.forEach { drawEntryBox(it.key, it.value.first, it.value.second, 1f) }

                    outList.iterator().apply {
                        while (hasNext()) {
                            val entry = next()

                            val sizeFactor = endSizeCurve.getFactor(entry.value.first, time, outTime.toFloat())
                            val expand = 1 - MathHelper.lerp(sizeFactor, 1f, endSize)
                            val box = getBox(expand, entry.value.third)
                            val colorFactor = 1 - fadeOutCurve.getFactor(entry.value.first, time, outTime.toFloat())

                            drawEntryBox(entry.key, entry.value.second, box, colorFactor)

                            if (time - entry.value.first >= outTime) {
                                updateNeighbors(entry.key)
                                remove()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getBox(expand: Float, box: Box): Box {
        return if (expand == 1f) {
            box
        } else if (expand == 0f) {
            EMPTY_BOX
        } else {
            val f = if (expand < 1) -0.5 * expand else (expand - 1) * 0.5
            box.expand(box.lengthX * f, box.lengthY * f, box.lengthZ * f)
        }
    }

    fun isFinished(): Boolean = outList.isEmpty()

    /**
     * Updates the culling of all blocks around a position that has been removed or added.
     */
    fun updateNeighbors(pos: BlockPos) {
        if (!placementRenderer.clump) {
            return
        }

        scanBlocksInCuboid(2, pos) { // TODO in theory a one block radius should be enough
            inList.computeIfPresent(it) { _, value ->
                Triple(value.first, getCullData(it), value.third)
            }?.let { return@scanBlocksInCuboid false }

            currentList.computeIfPresent(it) { _, value ->
                getCullData(it) to value.second
            }?.let { return@scanBlocksInCuboid false }

            return@scanBlocksInCuboid false
        }
    }

    /**
     * Returns a long that stores in the first 32 bits what vertices are to be rendered for the faces and
     * in the other half what vertices are to be rendered for the outline.
     */
    private fun getCullData(pos: BlockPos): Long {
        var faces = 1 shl 30
        var edges = 1 shl 30

        val eastPos = pos.east()
        val westPos = pos.west()
        val upPos = pos.up()
        val downPos = pos.down()
        val southPos = pos.south()
        val northPos = pos.north()

        val east = contains(eastPos)
        val west = contains(westPos)
        val up = contains(upPos)
        val down = contains(downPos)
        val south = contains(southPos)
        val north = contains(northPos)

        faces = cullSide(faces, east, FACE_EAST)
        faces = cullSide(faces, west, FACE_WEST)
        faces = cullSide(faces, up, FACE_UP)
        faces = cullSide(faces, down, FACE_DOWN)
        faces = cullSide(faces, south, FACE_SOUTH)
        faces = cullSide(faces, north, FACE_NORTH)

        edges = cullEdge(edges, north, down, contains(northPos.down()), EDGE_NORTH_DOWN)
        edges = cullEdge(edges, east, down, contains(eastPos.down()), EDGE_EAST_DOWN)
        edges = cullEdge(edges, south, down, contains(southPos.down()), EDGE_SOUTH_DOWN)
        edges = cullEdge(edges, west, down, contains(westPos.down()), EDGE_WEST_DOWN)
        edges = cullEdge(edges, north, west, contains(northPos.west()), EDGE_NORTH_WEST)
        edges = cullEdge(edges, north, east, contains(northPos.east()), EDGE_NORTH_EAST)
        edges = cullEdge(edges, south, east, contains(southPos.east()), EDGE_SOUTH_EAST)
        edges = cullEdge(edges, south, west, contains(westPos.south()), EDGE_SOUTH_WEST)
        edges = cullEdge(edges, north, up, contains(northPos.up()), EDGE_NORTH_UP)
        edges = cullEdge(edges, east, up, contains(eastPos.up()), EDGE_EAST_UP)
        edges = cullEdge(edges, south, up, contains(southPos.up()), EDGE_SOUTH_UP)
        edges = cullEdge(edges, west, up, contains(westPos.up()), EDGE_WEST_UP)

        // combines the data in a single long and inverts it, so that all vertices that are to be rendered are
        // represented by 1s
        return ((faces.toLong() shl 32) or edges.toLong()).inv()
    }

    /**
     * Checks whether the position is rendered.
     */
    private fun contains(pos: BlockPos) = inList.contains(pos) || currentList.contains(pos) || outList.contains(pos)

    /**
     * Applies a mask to the current data if either [direction1Present] and [direction2Present] are `false` or
     * [direction1Present] and [direction2Present] are `true` but [diagonalPresent] is `false`.
     *
     * This will result in the edge only being rendered if it's not surrounded by blocks and is on an actual
     * edge from multiple blocks seen as one entity.
     *
     * @return The updated [currentData]
     */
    private fun cullEdge(
        currentData: Int,
        direction1Present: Boolean,
        direction2Present: Boolean,
        diagonalPresent: Boolean,
        mask: Int
    ): Int {
        return if ((!direction1Present && !direction2Present)
            || (direction1Present && direction2Present && !diagonalPresent)) {
            currentData or mask
        } else {
            currentData
        }
    }

    /**
     * Applies a mask to the current data if either [directionPresent] is `false`.
     *
     * This will result in the face only being visible if it's on the outside of multiple blocks.
     *
     * @return The updated [currentData]
     */
    private fun cullSide(currentData: Int, directionPresent: Boolean, mask: Int): Int {
        return if (!directionPresent) {
            currentData or mask
        } else {
            currentData
        }
    }

    /**
     * Adds a block to be rendered. First it will make an appear-animation, then
     * it will continue to get rendered until it's removed or the world changes.
     */
    fun addBlock(pos: BlockPos, update: Boolean = true, box: Box = FULL_BOX) {
        if (!currentList.contains(pos) && !inList.contains(pos)) {
            inList[pos] = Triple(System.currentTimeMillis(), 0L, box)
            if (update) {
                updateNeighbors(pos)
            }
        }

        outList.remove(pos)
    }

    /**
     * Removes a block from the rendering, it will get an out animation tho.
     */
    fun removeBlock(pos: BlockPos) {
        var cullData = 0L
        var box: Box? = null
        currentList.remove(pos)?.let {
            cullData = it.first
            box = it.second
        } ?: run {
            inList.remove(pos)?.let {
                cullData = it.second
                box = it.third
            } ?: return
        }

        outList[pos] = Triple(System.currentTimeMillis(), cullData, box!!)
    }

    /**
     * Updates all culling data.
     *
     * This can be useful to reduce overhead when adding a bunch of positions,
     * so that positions don't get updated multiple times.
     */
    fun updateAll() {
        for (entry in inList) {
            inList[entry.key] = Triple(entry.value.first, getCullData(entry.key), entry.value.third)
        }
        for (entry in currentList) {
            currentList[entry.key] = getCullData(entry.key) to entry.value.second
        }
    }

    /**
     * Puts all currently rendered positions in the out-animation state and keeps it being rendered until
     * all animations have been finished even though the module might be already disabled.
     */
    fun clearSilently() {
        inList.iterator().apply {
            while (hasNext()) {
                val entry = next()
                outList[entry.key] = Triple(System.currentTimeMillis(), entry.value.second, entry.value.third)
                remove()
            }
        }

        currentList.iterator().apply {
            while (hasNext()) {
                val entry = next()
                outList[entry.key] = Triple(System.currentTimeMillis(), entry.value.first, entry.value.second)
                remove()
            }
        }
    }

    /**
     * Removes all stored positions.
     */
    fun clear() {
        inList.clear()
        currentList.clear()
        outList.clear()
    }

}
