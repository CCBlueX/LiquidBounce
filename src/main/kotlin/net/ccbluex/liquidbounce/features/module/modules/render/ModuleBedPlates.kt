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

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.GUIRenderEnvironment
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.manhattanDistanceTo
import net.ccbluex.liquidbounce.utils.kotlin.forEachWithSelf
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.block.*
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.max
import kotlin.math.roundToInt

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
private const val ITEM_SCALE: Float = 1.0F
private const val BACKGROUND_PADDING: Int = 2

object ModuleBedPlates : Module("BedPlates", Category.RENDER) {
    private val maxLayers by int("MaxLayers", 5, 1..5)
    private val scale by float("Scale", 1.5f, 0.5f..3.0f)
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
                    key.asBlockPos().getSquaredDistance(playerPos) to value
                }.filter { (distSq, _) ->
                    distSq < maxDistanceSquared
                }.sortedByDescending { (distSq, _) ->
                    distSq
                }.forEachWithSelf { (_, trackState), i, self ->
                    val bedPlates = trackState.bedPlates
                    withMatrixStack {
                        val z = 1000.0F * i / self.size

                        renderBedPlates(trackState, buf, bedPlates, z)
                    }
                }
            }
        }
    }

    private fun GUIRenderEnvironment.renderBedPlates(
        trackState: TrackedState,
        fontBuffers: FontRendererBuffers,
        bedPlates: Set<Block>,
        z: Float
    ) {
        val screenPos = WorldToScreen.calculateScreenPos(
            pos = trackState.centerPos,
        ) ?: return
        val dc = DrawContext(
            mc,
            mc.bufferBuilders.entityVertexConsumers
        )

        dc.matrices.translate(screenPos.x, screenPos.y, 0.0f)
        dc.matrices.scale(ITEM_SCALE * scale, ITEM_SCALE * scale, 1.0f)

        val bedDistance = mc.player?.pos?.distanceTo(trackState.centerPos) ?: 0.0
        val text = "Bed (${bedDistance.roundToInt()}m)"

        val c = Fonts.DEFAULT_FONT_SIZE.toFloat()

        val scale = 1.0F / (c * 0.15F) * scale

        val processedText = fontRenderer.process(text, defaultColor = Color4b.WHITE)
        val stringWidth = fontRenderer.getStringWidth(processedText)

        val width = max(bedPlates.size * ITEM_SIZE + ITEM_SIZE, (stringWidth * scale * 0.75f).toInt())
        val height = ITEM_SIZE + fontRenderer.height * scale
        dc.matrices.translate(-width / 2f, -height / 2f, z)

        // draw background
        dc.fill(
            -BACKGROUND_PADDING,
            -BACKGROUND_PADDING,
            width + BACKGROUND_PADDING,
            (height + BACKGROUND_PADDING).toInt(),
            Color4b(0, 0, 0, 128).toRGBA()
        )

        fontRenderer.draw(
            processedText,
            -stringWidth / 2.0f,
            0.0f
        )

        matrixStack.push()

        matrixStack.translate(screenPos.x, screenPos.y, z)
        matrixStack.translate(0.0f, -height + fontRenderer.height * scale - 6.0F, 0.0f)
        matrixStack.scale(scale, scale, 1.0F)

        fontRenderer.commit(fontBuffers)

        matrixStack.pop()

        // draw items
        val itemStartX = width / 2 - (bedPlates.size + 1) * ITEM_SIZE / 2
        dc.drawItem(
            trackState.bedItem,
            itemStartX,
            mc.textRenderer.fontHeight
        )
        bedPlates.forEachIndexed { index, block ->
            dc.drawItem(
                block.asItem().defaultStack,
                itemStartX + (index + 1) * ITEM_SIZE,
                mc.textRenderer.fontHeight,
            )
        }
    }

    private fun getBedPlates(pos: BlockPos, state: BlockState): Set<Block> {
        val bedPlates = hashSetOf<Block>()
        val secondPos = pos.offset(BedBlock.getOppositePartDirection(state))

        // If the second part of the bed is a bed block, we don't want to render the bed plates
        if (secondPos.getState()?.block !in BED_BLOCKS) {
            return emptySet()
        }

        val platesFirst = pos.getBedPlatesAround().keys
        val platesSecond = secondPos.getBedPlatesAround().keys
        bedPlates += platesFirst
        bedPlates += platesSecond
        bedPlates.removeAll(BED_BLOCKS)

        return bedPlates
    }

    private fun BlockPos.getBedPlatesAround(): Map<Block, Int> {
        val bedPlateTypes = hashMapOf<Block, Int>()

        for (offsetX in x - maxLayers..x + maxLayers) {
            for (offsetZ in z - maxLayers..z + maxLayers) {
                for (offsetY in y..y + maxLayers) {
                    val blockPos = BlockPos(offsetX, offsetY, offsetZ)
                    val blockState = blockPos.getState() ?: continue
                    val block = blockState.block

                    if (blockState.isAir || block in BED_BLOCKS) {
                        continue
                    }

                    // handle each block around the bed
                    val layer = manhattanDistanceTo(blockPos)
                    if (layer > maxLayers) continue

                    bedPlateTypes.compute(block) { _, value ->
                        value?.let { minOf(it, layer) } ?: layer
                    }
                }
            }
        }

        return bedPlateTypes
    }

    private fun getBedCenterPos(state: BlockState, pos: BlockPos): Vec3d {
        val oppositePartDir = BedBlock.getOppositePartDirection(state)
        return Vec3d(
            pos.x + (oppositePartDir.offsetX.toDouble() / 2) + 0.5,
            pos.y.toDouble() + 1.0,
            pos.z + (oppositePartDir.offsetZ.toDouble() / 2) + 0.5
        )
    }

    override fun enable() {
        ChunkScanner.subscribe(BlockTracker)
    }

    override fun disable() {
        ChunkScanner.unsubscribe(BlockTracker)
    }

    private class TrackedState(
        val bedPlates: Set<Block>,
        val centerPos: Vec3d,
        val bedItem: ItemStack,
    )

    private object BlockTracker : AbstractBlockLocationTracker<TrackedState>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): TrackedState? {
            return if (state.block in BED_BLOCKS) {
                val part = BedBlock.getBedPart(state)
                // Only track the first part of the bed
                if (part == DoubleBlockProperties.Type.FIRST) {
                    TrackedState(
                        bedPlates = getBedPlates(pos, state),
                        centerPos = getBedCenterPos(state, pos),
                        bedItem = state.block.asItem().defaultStack
                    )
                } else {
                    null
                }
            } else if (trackedBlockMap.isNotEmpty()) {
                // println(trackedBlockMap.size.toString())
                // A non-bed block was updated, we need to update the bed blocks around it
                updateAllBeds(pos)
                null
            } else null
        }

        private fun updateAllBeds(pos: BlockPos) {
            trackedBlockMap.forEach { (trackedPos, _) ->
                val trackedPosBlockPos = trackedPos.asBlockPos()
                // Update if the block is close to a bed
                if (trackedPosBlockPos.manhattanDistanceTo(pos) > maxLayers) {
                    return@forEach
                }

                val bedState = trackedPosBlockPos.getState() ?: return@forEach
                if (bedState.block !in BED_BLOCKS) {
                    // The tracked block is not a bed anymore, remove it
                    trackedBlockMap.remove(trackedPos)
                    return@forEach
                }

                trackedBlockMap[trackedPos] = TrackedState(
                    bedPlates = getBedPlates(trackedPosBlockPos, bedState),
                    centerPos = getBedCenterPos(bedState, trackedPosBlockPos),
                    bedItem = bedState.block.asItem().defaultStack
                )
            }
        }
    }
}
