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
package net.ccbluex.liquidbounce.features.module.modules.render

import it.unimi.dsi.fastutil.doubles.DoubleObjectImmutablePair
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair
import it.unimi.dsi.fastutil.ints.IntObjectPair
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.kotlin.forEachWithSelf
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.block.*
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.math.roundToInt
import kotlin.math.sqrt

val BED_BLOCKS = setOf(
    Blocks.RED_BED,
    Blocks.BLUE_BED,
    Blocks.GREEN_BED,
    Blocks.BLACK_BED,
    Blocks.WHITE_BED,
    Blocks.YELLOW_BED,
    Blocks.PURPLE_BED,
    Blocks.ORANGE_BED,
    Blocks.PINK_BED,
    Blocks.LIGHT_BLUE_BED,
    Blocks.LIGHT_GRAY_BED,
    Blocks.LIME_BED,
    Blocks.MAGENTA_BED,
    Blocks.BROWN_BED,
    Blocks.CYAN_BED,
    Blocks.GRAY_BED
)
private const val ITEM_SIZE: Int = 16
private const val BACKGROUND_PADDING: Int = 2

object ModuleBedPlates : Module("BedPlates", Category.RENDER) {
    private val ROMAN_NUMERALS = arrayOf("", "I", "II", "III", "IV", "V")

    private val maxLayers by int("MaxLayers", 5, 1..5)
    private val scale by float("Scale", 1.5f, 0.5f..3.0f)
    private val renderY by float("RenderY", 0.0F, -2.0F..2.0F)
    private val maxDistance by float("MaxDistance", 256.0f, 128.0f..1280.0f)

    private val fontRenderer by lazy {
        Fonts.DEFAULT_FONT.get()
    }

    val renderHandler = handler<OverlayRenderEvent> {
        val playerPos = player.blockPos

        val maxDistanceSquared = maxDistance * maxDistance

        renderEnvironmentForGUI {
            fontRenderer.withBuffers { buf ->
                BlockTracker.trackedBlockMap.map { (key, value) ->
                    DoubleObjectImmutablePair(key.getSquaredDistance(playerPos), value)
                }.filter {
                    it.keyDouble() < maxDistanceSquared
                }.sortedByDescending {
                    it.keyDouble()
                }.forEachWithSelf { entry, i, self ->
                    val bedState = entry.value()
                    val screenPos = WorldToScreen.calculateScreenPos(bedState.pos.add(0.0, renderY.toDouble(), 0.0))
                        ?: return@forEachWithSelf
                    val distance = sqrt(entry.keyDouble())
                    val surrounding = bedState.surroundingBlocks

                    val z = 1000.0F * i / self.size

                    // without padding
                    val rectWidth = ITEM_SIZE * (1 + surrounding.size)
                    val rectHeight = ITEM_SIZE

                    // draw items and background
                    with(DrawContext(mc, mc.bufferBuilders.entityVertexConsumers)) {
                        with(matrices) {
                            translate(screenPos.x, screenPos.y, z)
                            scale(scale, scale, 1.0F)
                            translate(-0.5F * rectWidth, -0.5F * rectHeight, -1F)

                            fill(
                                -BACKGROUND_PADDING,
                                -BACKGROUND_PADDING,
                                rectWidth + BACKGROUND_PADDING,
                                rectHeight + BACKGROUND_PADDING,
                                Color4b(0, 0, 0, 128).toRGBA()
                            )

                            var itemX = 0
                            drawItem(bedState.block.asItem().defaultStack, itemX, 0)
                            surrounding.forEach {
                                itemX += ITEM_SIZE
                                drawItem(it.block.asItem().defaultStack, itemX, 0)
                            }
                        }
                    }

                    // draw texts
                    withMatrixStack {
                        translate(screenPos.x, screenPos.y, z)
                        scale(scale, scale, 1.0F)
                        translate(-0.5F * rectWidth, -0.5F * rectHeight, 150F + 20F)

                        val fontScale = 1.0F / (size * 0.15F)
                        val heightScaled = fontScale * height

                        var topLeftX = 0
                        draw(
                            process("${distance.roundToInt()}m"),
                            0F,
                            rectHeight - heightScaled,
                            shadow = true,
                            scale = fontScale,
                        )
                        commit(buf)
                        surrounding.forEach {
                            topLeftX += ITEM_SIZE

                            // count
                            val countText = process(it.count.toString())
                            draw(
                                countText,
                                topLeftX + ITEM_SIZE - countText.widthWithShadow * fontScale,
                                rectHeight - heightScaled,
                                shadow = true,
                                scale = fontScale,
                            )
                            commit(buf)

                            // layer
                            val layerText = process(ROMAN_NUMERALS[it.layer])
                            draw(
                                layerText,
                                topLeftX.toFloat(),
                                0F,
                                shadow = true,
                                scale = fontScale,
                            )
                            commit(buf)
                        }
                    }
                }
            }
        }
    }

    @JvmRecord
    private data class SurroundingBlock(
        val block: Block,
        val count: Int,
        val layer: Int,
    ) : Comparable<SurroundingBlock> {
        override fun compareTo(other: SurroundingBlock): Int = compareValuesBy(this, other,
            SurroundingBlock::layer, { -it.count }, { -it.block.hardness })
    }

    @JvmRecord
    private data class BedState(
        val block: Block,
        val pos: Vec3d,
        val surroundingBlocks: Set<SurroundingBlock>,
    )

    private fun BlockPos.searchLayer(layers: Int, vararg directions: Direction): Sequence<IntObjectPair<BlockPos>> =
        sequence {
            val queue = ArrayDeque<IntObjectPair<BlockPos>>(layers * layers * directions.size / 2).apply {
                add(IntObjectImmutablePair(0, this@searchLayer))
            }
            val visited = hashSetOf(this@searchLayer)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()

                val layer = current.keyInt()

                if (layer == layers) {
                    continue
                }

                if (layer > 0) {
                    yield(current)
                }

                val pos = current.value()

                for (direction in directions) {
                    val newPos = pos.offset(direction)

                    if (newPos !in visited && getManhattanDistance(newPos) <= layers) {
                        visited.add(newPos)
                        queue.add(IntObjectImmutablePair(layer + 1, newPos))
                    }
                }
            }
        }

    private fun getBedPlates(headState: BlockState, head: BlockPos): BedState {
        val bedDirection = headState.get(BedBlock.FACING)

        // up + left + right
        val directions = if (bedDirection.axis == Direction.Axis.X) {
            arrayOf(Direction.UP, Direction.SOUTH, Direction.NORTH)
        } else {
            arrayOf(Direction.UP, Direction.WEST, Direction.EAST)
        }

        val opposite = bedDirection.opposite
        val layers = Array<Object2IntOpenHashMap<Block>>(maxLayers - 1) { Object2IntOpenHashMap() }

        (head.searchLayer(maxLayers, bedDirection, *directions) +
            head.offset(opposite).searchLayer(maxLayers, opposite, *directions))
            .mapNotNull {
                IntObjectImmutablePair(it.keyInt(), it.value()?.getState() ?: return@mapNotNull null)
            }
            .filterNot {
                // Ignore empty positions
                it.value().isAir
            }
            .forEach {
                // Count blocks
                val map = layers[it.keyInt() - 1]
                val block = it.value().block
                if (map.containsKey(block)) {
                    map.put(block, map.getInt(block) + 1)
                } else {
                    map.put(block, 1)
                }
            }

        return BedState(
            headState.block,
            Vec3d(
                head.x + (opposite.offsetX * 0.5) + 0.5,
                head.y + 1.0,
                head.z + (opposite.offsetZ * 0.5) + 0.5,
            ),
            sortedSetOf<SurroundingBlock>().apply {
                // flat map
                layers.forEachIndexed { i, map ->
                    map.object2IntEntrySet().forEach {
                        add(
                            SurroundingBlock(
                                it.key,
                                it.intValue,
                                i + 1,
                            )
                        )
                    }
                }
            },
        )
    }

    override fun enable() {
        ChunkScanner.subscribe(BlockTracker)
    }

    override fun disable() {
        ChunkScanner.unsubscribe(BlockTracker)
    }

    private object BlockTracker : AbstractBlockLocationTracker<BedState>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): BedState? {
            return when {
                state.block in BED_BLOCKS -> {
                    val part = BedBlock.getBedPart(state)
                    // Only track the first part (head) of the bed
                    if (part == DoubleBlockProperties.Type.FIRST) {
                        getBedPlates(state, pos)
                    } else {
                        null
                    }
                }

                trackedBlockMap.isNotEmpty() -> {
                    // A non-bed block was updated, we need to update the bed blocks around it
                    trackedBlockMap.keys.forEach {
                        // Update if the block is close to a bed
                        val trackedPos = it
                        if (trackedPos.getManhattanDistance(pos) > maxLayers) {
                            return@forEach
                        }

                        val trackedState = trackedPos.getState() ?: return@forEach
                        if (trackedState.block !in BED_BLOCKS) {
                            // The tracked block is not a bed anymore, remove it
                            trackedBlockMap.remove(it)
                        } else {
                            trackedBlockMap[it] = getBedPlates(trackedState, trackedPos)
                        }
                    }
                    null
                }

                else -> null
            }
        }
    }
}
