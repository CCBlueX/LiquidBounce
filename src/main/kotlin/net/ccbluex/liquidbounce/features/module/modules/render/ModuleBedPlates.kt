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

import it.unimi.dsi.fastutil.doubles.DoubleObjectPair
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.ccbluex.liquidbounce.utils.kotlin.forEachWithSelf
import net.ccbluex.liquidbounce.utils.kotlin.getValue
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.block.*
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val ITEM_SIZE: Int = 16
private const val BACKGROUND_PADDING: Int = 2

object ModuleBedPlates : ClientModule("BedPlates", Category.RENDER) {
    private val ROMAN_NUMERALS = arrayOf("", "I", "II", "III", "IV", "V")

    private val maxLayers by int("MaxLayers", 5, 1..5)
    private val scale by float("Scale", 1.5f, 0.5f..3.0f)
    private val renderY by float("RenderY", 0.0F, -2.0F..2.0F)
    private val maxDistance by float("MaxDistance", 256.0f, 128.0f..1280.0f)
    private val maxCount by int("MaxCount", 8, 1..64)
    private val highlightUnbreakable by boolean("HighlightUnbreakable", true)
    private val compact by boolean("Compact", true)

    private val fontRenderer
        get() = FontManager.FONT_RENDERER

    private val WHITELIST_NON_SOLID = setOf(
        Blocks.LADDER,

        Blocks.GLASS,
        Blocks.WHITE_STAINED_GLASS,
        Blocks.ORANGE_STAINED_GLASS,
        Blocks.MAGENTA_STAINED_GLASS,
        Blocks.LIGHT_BLUE_STAINED_GLASS,
        Blocks.YELLOW_STAINED_GLASS,
        Blocks.LIME_STAINED_GLASS,
        Blocks.PINK_STAINED_GLASS,
        Blocks.GRAY_STAINED_GLASS,
        Blocks.LIGHT_GRAY_STAINED_GLASS,
        Blocks.CYAN_STAINED_GLASS,
        Blocks.PURPLE_STAINED_GLASS,
        Blocks.BLUE_STAINED_GLASS,
        Blocks.BROWN_STAINED_GLASS,
        Blocks.GREEN_STAINED_GLASS,
        Blocks.RED_STAINED_GLASS,
        Blocks.BLACK_STAINED_GLASS,
    )

    val renderHandler = handler<OverlayRenderEvent> {
        val playerPos = player.blockPos

        val maxDistanceSquared = maxDistance.sq()

        renderEnvironmentForGUI {
            fontRenderer.withBuffers { buf ->
                BedBlockTracker.trackedBlockMap.map { (pos, bedState) ->
                    DoubleObjectPair.of(pos.getSquaredDistance(playerPos), bedState)
                }.filter { (distSq, _) ->
                    distSq < maxDistanceSquared
                }.sortedBy { (distSq, _) ->
                    distSq
                }.take(maxCount).forEachWithSelf { (distSq, bedState), i, self ->
                    val screenPos = WorldToScreen.calculateScreenPos(bedState.pos.add(0.0, renderY.toDouble(), 0.0))
                        ?: return@forEachWithSelf
                    val distance = sqrt(distSq)
                    val surrounding = bedState.surroundingBlocks

                    val z = 1000.0F * (self.size - i - 1) / self.size

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
                                Color4b(0, 0, 0, 128).toARGB()
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

                            val defaultState = it.block.defaultState
                            val color =
                                if (highlightUnbreakable && defaultState.isToolRequired
                                    && Slots.Hotbar.findSlot { s -> s.isSuitableFor(defaultState) } == null) {
                                    Color4b.RED
                                } else {
                                    Color4b.WHITE
                                }

                            // count
                            val countText = process(it.count.toString(), color)
                            draw(
                                countText,
                                topLeftX + ITEM_SIZE - countText.widthWithShadow * fontScale,
                                rectHeight - heightScaled,
                                shadow = true,
                                scale = fontScale,
                            )
                            commit(buf)

                            if (!compact) {
                                // layer
                                val layerText = process(ROMAN_NUMERALS[it.layer], color)
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
    }

    @JvmRecord
    private data class SurroundingBlock(
        val block: Block,
        val count: Int,
        val layer: Int,
    ) : Comparable<SurroundingBlock> {
        override fun compareTo(other: SurroundingBlock): Int = compareValuesBy(this, other,
            { it.layer }, { -it.count }, { -it.block.hardness }, { it.block.translationKey })
    }

    private sealed class BedState(val block: Block, val pos: Vec3d) {
        abstract val surroundingBlocks: Set<SurroundingBlock>

        class Normal(
            block: Block,
            pos: Vec3d,
            override val surroundingBlocks: Set<SurroundingBlock>
        ) : BedState(block, pos)

        class Lazy(
            block: Block,
            pos: Vec3d,
            supplier: () -> Set<SurroundingBlock>
        ) : BedState(block, pos) {
            override val surroundingBlocks by lazy(LazyThreadSafetyMode.NONE, supplier)
        }
    }

    private fun BlockPos.getBedSurroundingBlocks(blockState: BlockState): Set<SurroundingBlock> {
        val layers = Array<Object2IntOpenHashMap<Block>>(maxLayers, ::Object2IntOpenHashMap)

        searchBedLayer(blockState, maxLayers)
            .forEach { (layer, pos) ->
                val state = pos.getState()

                // Ignore empty positions and fluid
                if (state == null || state.isAir) {
                    return@forEach
                }

                val block = state.block
                if (state.isSolidBlock(world, pos) || block in WHITELIST_NON_SOLID) {
                    // Count blocks (getInt default = 0)
                    with(layers[layer - 1]) {
                        put(block, getInt(block) + 1)
                    }
                }
            }

        val result = TreeSet<SurroundingBlock>()

        layers.forEachIndexed { i, map ->
            map.object2IntEntrySet().forEach { (block, count) ->
                result += SurroundingBlock(block, count, i + 1)
            }
        }

        return if (compact) {
            result.groupBy { surrounding ->
                surrounding.block
            }.map { (block, group) ->
                group.reduce { acc, item ->
                    SurroundingBlock(
                        block = block,
                        count = acc.count + item.count,
                        layer = minOf(acc.layer, item.layer)
                    )
                }
            }.toSet()
        } else {
            result
        }
    }

    private fun BlockPos.getBedPlates(headState: BlockState): BedState {
        val bedDirection = headState.get(BedBlock.FACING)

        val bedBlock = headState.block
        val renderPos = Vec3d(
            x - (bedDirection.offsetX * 0.5) + 0.5,
            y + 1.0,
            z - (bedDirection.offsetZ * 0.5) + 0.5,
        )

        // When there are many beds, we don't load them all
        return if (BedBlockTracker.trackedBlockMap.size < maxCount + 4
                    || player.blockPos.getSquaredDistance(this) < maxDistance.sq()) {
            BedState.Normal(bedBlock, renderPos, getBedSurroundingBlocks(headState))
        } else {
            BedState.Lazy(bedBlock, renderPos) { getBedSurroundingBlocks(headState) }
        }
    }

    override fun enable() {
        ChunkScanner.subscribe(BedBlockTracker)
    }

    override fun disable() {
        ChunkScanner.unsubscribe(BedBlockTracker)
    }

    private object BedBlockTracker : AbstractBlockLocationTracker<BedState>() {
        private val searchStart by ThreadLocal.withInitial(BlockPos::Mutable)
        private val searchEnd by ThreadLocal.withInitial(BlockPos::Mutable)

        @Suppress("detekt:CognitiveComplexMethod")
        override fun getStateFor(pos: BlockPos, state: BlockState): BedState? {
            return if (state.isBed) {
                val part = BedBlock.getBedPart(state)
                // Only track the first part (head) of the bed
                if (part == DoubleBlockProperties.Type.FIRST) {
                    pos.getBedPlates(state)
                } else {
                    null
                }
            } else {
                // A non-bed block was updated, we need to update the bed blocks around it
                val distance = maxLayers

                // Get a sub map of the sorted map when there are many beds
                val lookUpMap = if (trackedBlockMap.size > 32) {
                    trackedBlockMap.subMap(
                        searchStart.set(pos, -distance, -distance, -distance), true,
                        // Don't check beds above
                        searchEnd.set(pos, distance, 0, distance), true,
                    )
                } else {
                    trackedBlockMap
                }

                lookUpMap.keys.forEach {
                    // Update if the block is close to a bed
                    if (it.getManhattanDistance(pos) > distance) {
                        return@forEach
                    }

                    val trackedState = it.getState() ?: return@forEach
                    if (!trackedState.isBed) {
                        // The tracked block is not a bed anymore, remove it
                        lookUpMap.remove(it)
                    } else {
                        lookUpMap[it] = it.getBedPlates(trackedState)
                    }
                }

                null
            }
        }
    }
}
