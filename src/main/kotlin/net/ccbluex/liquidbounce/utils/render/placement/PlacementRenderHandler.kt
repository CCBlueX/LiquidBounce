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
package net.ccbluex.liquidbounce.utils.render.placement

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper

/**
 * A renderer instance that can be added to a [PlacementRenderer], it contains the core logic.
 * Culling is handled in each handler for its boxes individually.
 *
 * This class is not thread-safe. You can use it on the render thread. (the most recommended way)
 */
@Suppress("TooManyFunctions")
class PlacementRenderHandler(private val placementRenderer: PlacementRenderer, val id: Int = 0) {

    private val inList = Long2ObjectLinkedOpenHashMap<InOutBlockData>()
    private val currentList = Long2ObjectLinkedOpenHashMap<CurrentBlockData>()
    private val outList = Long2ObjectLinkedOpenHashMap<InOutBlockData>()

    private val culler = BlockCuller(this)

    @JvmRecord
    private data class InOutBlockData(val startTime: Long, val cullData: Long, val box: Box) {
        fun toCurrent() = CurrentBlockData(cullData, box)
    }

    @JvmRecord
    private data class CurrentBlockData(val cullData: Long, val box: Box) {
        fun toInOut(startTime: Long) = InOutBlockData(startTime, cullData, box)
    }

    private val blockPosCache = BlockPos.Mutable()

    fun render(event: WorldRenderEvent, time: Long) {
        val matrixStack = event.matrixStack

        with(placementRenderer) {
            val color = getColor(id)
            val outlineColor = getOutlineColor(id)

            renderEnvironmentForWorld(matrixStack) {
                BoxRenderer.drawWith(this) {
                    fun drawEntryBox(blockPos: BlockPos, cullData: Long, box: Box, colorFactor: Float) {
                        withPositionRelativeToCamera(blockPos.toVec3d()) {
                            drawBox(
                                box,
                                color.fade(colorFactor),
                                outlineColor.fade(colorFactor),
                                (cullData shr 32).toInt(),
                                (cullData and 0xFFFFFFFF).toInt()
                            )
                        }
                    }

                    inList.long2ObjectEntrySet().iterator().apply {
                        while (hasNext()) {
                            // Do not use destructuring declaration which returns boxed [Long] values
                            val entry = next()
                            val pos = entry.longKey
                            val value = entry.value

                            val sizeFactor = startSizeCurve.getFactor(value.startTime, time, inTime.toFloat())
                            val expand = MathHelper.lerp(sizeFactor, startSize, 1f)
                            val box = getBox(if (expand < 1f) 1f - expand else expand, value.box)
                            val colorFactor = fadeInCurve.getFactor(value.startTime, time, inTime.toFloat())

                            drawEntryBox(blockPosCache.set(pos), value.cullData, box, colorFactor)

                            if (time - value.startTime >= outTime) {
                                if (keep) {
                                    currentList[pos] = value.toCurrent()
                                } else {
                                    outList[pos] = value.copy(startTime = time)
                                }
                                remove()
                            }
                        }
                    }

                    currentList.long2ObjectEntrySet().forEach { entry ->
                        val pos = entry.longKey
                        val value = entry.value
                        drawEntryBox(blockPosCache.set(pos), value.cullData, value.box, 1f)
                    }

                    outList.long2ObjectEntrySet().iterator().apply {
                        while (hasNext()) {
                            val entry = next()
                            val pos = entry.longKey
                            val value = entry.value

                            val sizeFactor = endSizeCurve.getFactor(value.startTime, time, outTime.toFloat())
                            val expand = 1f - MathHelper.lerp(sizeFactor, 1f, endSize)
                            val box = getBox(expand, value.box)
                            val colorFactor = 1f - fadeOutCurve.getFactor(value.startTime, time, outTime.toFloat())

                            drawEntryBox(blockPosCache.set(pos), value.cullData, box, colorFactor)

                            if (time - value.startTime >= outTime) {
                                remove()
                                updateNeighbors(blockPosCache.set(pos))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getBox(expand: Float, box: Box): Box {
        return when (expand) {
            1f -> box
            0f -> EMPTY_BOX
            else -> {
                val f = if (expand < 1) -0.5 * expand else (expand - 1) * 0.5
                box.expand(box.lengthX * f, box.lengthY * f, box.lengthZ * f)
            }
        }
    }

    fun isFinished(): Boolean = outList.isEmpty()

    /**
     * Updates the culling of all blocks around a position that has been removed or added.
     */
    private fun updateNeighbors(pos: BlockPos) {
        if (!placementRenderer.clump) {
            return
        }

        // TODO in theory a one block radius should be enough
        pos.searchBlocksInCuboid(2).forEach {
            val longValue = it.asLong()

            val inValue = inList[longValue]
            if (inValue != null) {
                inList.put(longValue, inValue.copy(cullData = this.culler.getCullData(longValue)))
                return@forEach
            }

            val currentValue = currentList[longValue]
            if (currentValue != null) {
                currentList.put(longValue, currentValue.copy(cullData = this.culler.getCullData(longValue)))
                return@forEach
            }
        }
    }

    /**
     * Checks whether the position (in long value) is rendered.
     */
    internal operator fun contains(pos: Long): Boolean {
        return inList.containsKey(pos) || currentList.containsKey(pos) || outList.containsKey(pos)
    }

    /**
     * Adds a block to be rendered. First it will make an appear-animation, then
     * it will continue to get rendered until it's removed or the world changes.
     *
     * @param pos The position, can be [BlockPos.Mutable].
     */
    fun addBlock(pos: BlockPos, update: Boolean = true, box: Box = FULL_BOX) {
        val longValue = pos.asLong()
        if (!currentList.containsKey(longValue) && !inList.containsKey(longValue)) {
            inList.put(longValue, InOutBlockData(System.currentTimeMillis(), 0L, box))
            if (update) {
                updateNeighbors(pos)
            }
        }

        outList.remove(longValue)
    }

    /**
     * Removes a block from the rendering, it will get an out animation tho.
     *
     * @param pos The position, can be [BlockPos.Mutable].
     */
    fun removeBlock(pos: BlockPos) {
        val longValue = pos.asLong()
        var cullData = 0L
        var box: Box? = null

        currentList.remove(longValue)?.let {
            cullData = it.cullData
            box = it.box
        } ?: run {
            inList.remove(longValue)?.let {
                cullData = it.cullData
                box = it.box
            } ?: return
        }

        outList.put(longValue, InOutBlockData(System.currentTimeMillis(), cullData, box!!))
    }

    /**
     * Updates all culling data.
     *
     * This can be useful to reduce overhead when adding a bunch of positions,
     * so that positions don't get updated multiple times.
     */
    fun updateAll() {
        inList.long2ObjectEntrySet().forEach { entry ->
            val key = entry.longKey
            val value = entry.value
            entry.setValue(value.copy(cullData = this.culler.getCullData(key)))
        }

        currentList.long2ObjectEntrySet().forEach { entry ->
            val key = entry.longKey
            val value = entry.value
            entry.setValue(value.copy(cullData = this.culler.getCullData(key)))
        }
    }

    /**
     * Updates the box of [pos] to [box].
     *
     * This method won't affect positions that are in the state of fading out.
     */
    fun updateBox(pos: BlockPos, box: Box) {
        val longValue = pos.asLong()
        var needUpdate = false

        if (inList.containsKey(longValue)) {
            needUpdate = true
            inList.put(longValue, inList.get(longValue).copy(box = box))
        }

        if (currentList.containsKey(longValue)) {
            needUpdate = true
            currentList.put(longValue, currentList.get(longValue).copy(box = box))
        }

        if (needUpdate) {
            updateNeighbors(pos)
        }
    }

    /**
     * Puts all currently rendered positions in the out-animation state and keeps it being rendered until
     * all animations have been finished even though the module might be already disabled.
     */
    fun clearSilently() {
        inList.long2ObjectEntrySet().iterator().apply {
            while (hasNext()) {
                val entry = next()
                val pos = entry.longKey
                val value = entry.value
                outList.put(pos, value.copy(startTime = System.currentTimeMillis()))
                remove()
            }
        }

        currentList.long2ObjectEntrySet().iterator().apply {
            while (hasNext()) {
                val entry = next()
                val pos = entry.longKey
                val value = entry.value
                outList.put(pos, value.toInOut(startTime = System.currentTimeMillis()))
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
