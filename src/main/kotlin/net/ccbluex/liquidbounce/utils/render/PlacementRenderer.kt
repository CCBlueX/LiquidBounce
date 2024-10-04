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
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.scanBlocksInCuboid
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

// TODO make it use AABBs
class PlacementRenderer(
    name: String,
    enabled: Boolean,
    val module: Module,
    val keep: Boolean = true,
    clump: Boolean = true
) : ToggleableConfigurable(module, name, enabled) {

    private val clump by boolean("Clump", clump)

    private val startSize by float("StartSize", 1.2f, 0f..2f)
    private val startSizeCurve by curve("StartCurve", Easing.EXPONENTIAL_OUT)

    private val endSize by float("EndSize", 0.8f, 0f..2f)
    private val endSizeCurve by curve("EndCurve", Easing.EXPONENTIAL_IN)

    private val fadeInCurve by curve("FadeInCurve", Easing.LINEAR)
    private val fadeOutCurve by curve("FadeOutCurve", Easing.LINEAR)

    private val inTime by int("InTime", 1000, 0..5000, "ms")
    private val outTime by int("InTime", 1000, 0..5000, "ms")

    private val color by color("Color", Color4b(0, 255, 0, 150))
    private val outlineColor by color("OutlineColor", Color4b(0, 255, 0, 150))

    private val inList = linkedMapOf<BlockPos, Pair<Long, Long>>()
    private val currentList = linkedMapOf<BlockPos, Long>()
    private val outList = linkedMapOf<BlockPos, Pair<Long, Long>>()
    private var outAnimationsFinished = true

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val time = System.currentTimeMillis()

        renderEnvironmentForWorld(matrixStack) {
            BoxRenderer.drawWith(this) {
                fun drawEntryBox(blockPos: BlockPos, cullData: Long, box: Box, colorFactor: Float) {
                    val vec3d = Vec3d(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble())
                    withPositionRelativeToCamera(vec3d) {
                        drawBox(
                            box,
                            color.fade(colorFactor),
                            outlineColor.fade(colorFactor),
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
                        val box = getBox(expand)
                        val colorFactor = fadeInCurve.getFactor(entry.value.first, time, inTime.toFloat())

                        drawEntryBox(entry.key, entry.value.second, box, colorFactor)

                        if (time - entry.value.first >= outTime) {
                            if (keep) {
                                currentList[entry.key] = entry.value.second
                            } else {
                                outList[entry.key] = System.currentTimeMillis() to entry.value.second
                            }
                            remove()
                        }
                    }
                }

                currentList.forEach { drawEntryBox(it.key, it.value, FULL_BOX, 1f) }

                outList.iterator().apply {
                    while (hasNext()) {
                        val entry = next()

                        val sizeFactor = endSizeCurve.getFactor(entry.value.first, time, outTime.toFloat())
                        val expand = MathHelper.lerp(sizeFactor, 1f, endSize)
                        val box = getBox(expand)
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

    @Suppress("unused")
    private val repeatable = repeatable {
        if (!outAnimationsFinished && outList.isEmpty()) {
            outAnimationsFinished = true
        }
    }

    private fun getBox(expand: Float): Box {
        return if (expand == 0f) {
            FULL_BOX
        } else {
            if (expand < 1) {
                FULL_BOX.expand(expand.toDouble() - 1)  // TODO to properly shrink half blocks we need to get the size and multiply it here
            } else {
                FULL_BOX.expand(1 - expand.toDouble())
            }
        }
    }

    /**
     * Updates the culling of all blocks around a position that has been removed or added.
     */
    private fun updateNeighbors(pos: BlockPos) {
        if (!clump) {
            return
        }

        scanBlocksInCuboid(2, pos) { // TODO in theory a one block radius should be enough
            inList.computeIfPresent(it) { _, value ->
                value.first to getCullData(it)
            }?.let { return@scanBlocksInCuboid false }

            currentList.computeIfPresent(it) { _, _ ->
                getCullData(it)
            }?.let { return@scanBlocksInCuboid false }

            return@scanBlocksInCuboid false
        }
    }

    /**
     * Adds a block to be rendered. First it will make an appear-animation, then
     * it will continue to get rendered until it's removed or the world changes.
     */
    fun addBlock(pos: BlockPos, update: Boolean = true) {
        if (!enabled) {
            return
        }

        if (!currentList.contains(pos) && !inList.contains(pos)) {
            inList[pos] = System.currentTimeMillis() to 0L
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
        if (!enabled) {
            return
        }

        val entry = currentList.remove(pos) ?: inList.remove(pos)?.second ?: return
        outList[pos] = System.currentTimeMillis() to entry
    }

    /**
     * Updates all culling data.
     *
     * This can be useful to reduce overhead when adding a bunch of positions,
     * so that positions don't get updated multiple times.
     */
    fun updateAll() {
        if (!clump) {
            return
        }

        for (entry in inList) {
            inList[entry.key] = entry.value.first to getCullData(entry.key)
        }
        for (entry in currentList) {
            currentList[entry.key] = getCullData(entry.key)
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
        return if ((!direction1Present && !direction2Present) || (direction1Present && direction2Present && !diagonalPresent)) {
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
     * Puts all currently rendered positions in the out-animation state and keeps it being rendered until
     * all animations have been finished even though the module might be already disabled.
     */
    fun clearSilently() {
        inList.iterator().apply {
            while (hasNext()) {
                val entry = next()
                outList[entry.key] = System.currentTimeMillis() to entry.value.second
                remove()
            }
        }

        currentList.iterator().apply {
            while (hasNext()) {
                val entry = next()
                outList[entry.key] = System.currentTimeMillis() to entry.value
                remove()
            }
        }

        outAnimationsFinished = false
    }

    override fun disable() {
        clear()
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        clear()
    }

    /**
     * Removes all stored positions.
     */
    private fun clear() {
        inList.clear()
        currentList.clear()
        outList.clear()
    }

    /**
     * Only run when the module and this is enabled or out-animations are running.
     */
    override fun handleEvents(): Boolean {
        return module.handleEvents() && enabled || !outAnimationsFinished
    }

}
