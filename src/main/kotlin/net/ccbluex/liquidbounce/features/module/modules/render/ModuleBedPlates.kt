package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.block.*
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val BED_BLOCKS = setOf(
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
    val maxLayers by int("MaxLayers", 5, 1..5)
    val scale by float("Scale", 1.5f, 0.5f..3.0f)

    val renderHandler = handler<OverlayRenderEvent> {
        renderEnvironmentForGUI {
            synchronized(BlockTracker.trackedBlockMap) {
                val trackedBlockMap = BlockTracker.trackedBlockMap

                for ((pos, trackState) in trackedBlockMap) {
                    val bedPlates = trackState.bedPlates
                    if (bedPlates.isNotEmpty()) {
                        with(matrixStack) {
                            push()
                            try {
                                val screenPos = WorldToScreen.calculateScreenPos(
                                    pos = trackState.centerPos,
                                ) ?: return@with
                                val dc = DrawContext(
                                    mc,
                                    mc.bufferBuilders.entityVertexConsumers
                                )

                                dc.matrices.translate(screenPos.x, screenPos.y, 0.0f)
                                dc.matrices.scale(ITEM_SCALE * scale, ITEM_SCALE * scale, 1.0f)

                                val bedDistance = mc.player?.pos?.distanceTo(trackState.centerPos) ?: 0.0
                                val text = "Bed (${bedDistance.roundToInt()}m)"

                                val width = max(bedPlates.size * ITEM_SIZE, mc.textRenderer.getWidth(text))
                                val height = ITEM_SIZE + mc.textRenderer.fontHeight
                                dc.matrices.translate(-width / 2f, -height / 2f, 0.0f)

                                // draw background
                                dc.fill(
                                    -BACKGROUND_PADDING,
                                    -BACKGROUND_PADDING,
                                    width + BACKGROUND_PADDING,
                                    height + BACKGROUND_PADDING,
                                    Color4b(0, 0, 0, 128).toRGBA()
                                )

                                // draw text
                                dc.drawTextWithShadow(
                                    mc.textRenderer,
                                    text,
                                    width / 2 - mc.textRenderer.getWidth(text) / 2,
                                    0,
                                    Color4b.WHITE.toRGBA()
                                )

                                // draw items
                                val itemStartX = width / 2 - bedPlates.size * ITEM_SIZE / 2
                                for ((index, block) in bedPlates.withIndex()) {
                                    dc.drawItem(
                                        block.asItem().defaultStack,
                                        itemStartX + index * ITEM_SIZE,
                                        mc.textRenderer.fontHeight,
                                    )
                                }
                                if (bedPlates.isEmpty()) {
                                    dc.drawItem(
                                        Items.RED_BED.defaultStack,
                                        width / 2 - ITEM_SIZE / 2,
                                        mc.textRenderer.fontHeight,
                                    )
                                }
                            } finally {
                                pop()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getBedPlates(pos: BlockPos, state: BlockState): List<Block> {
        val bedPlates = arrayListOf<Block>()
        val secondPos = pos.offset(BedBlock.getOppositePartDirection(state))

        // If the second part of the bed is a bed block, we don't want to render the bed plates
        if(secondPos.getState()?.block !in BED_BLOCKS) {
            return emptyList()
        }

        val firstPlates = pos.getBedPlatesAround()
        val secondPlates = secondPos.getBedPlatesAround()

        for (layer in 1..maxLayers) {
            val platesFirst = firstPlates.filterValues { it == layer }.keys
            val platesSecond = secondPlates.filterValues { it == layer }.keys
            if (platesFirst.isEmpty() && platesSecond.isEmpty()) {
                break
            }
            platesFirst.forEach {
                if (it !in bedPlates) {
                    bedPlates.add(it)
                }
            }
            platesSecond.forEach {
                if (it !in bedPlates) {
                    bedPlates.add(it)
                }
            }
        }

        return bedPlates.toList()
    }

    private fun BlockPos.getBedPlatesAround(): Map<Block, Int> {
        val bedPlateTypes = mutableMapOf<Block, Int>()

        // handle each block around the bed
        val handleBlock : (BlockPos, BlockState) -> Unit = { blockPos, blockState ->
            val block = blockState.block
            if (!blockState.isAir && block !in BED_BLOCKS) {
                val layer = manhattanDistanceTo(blockPos)
                bedPlateTypes.compute(block) { _, value ->
                    if (value == null || layer < value) {
                        layer
                    } else {
                        value
                    }
                }
            }
        }

        for (offsetX in x - maxLayers..x + maxLayers) {
            for (offsetZ in z - maxLayers..z + maxLayers) {
                for (offsetY in y..y + maxLayers) {
                    val blockPos = BlockPos(offsetX, offsetY, offsetZ)
                    val blockState = blockPos.getState() ?: continue
                    handleBlock(blockPos, blockState)
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

    private fun BlockPos.manhattanDistanceTo(other: BlockPos): Int {
        return abs(x - other.x) + abs(y - other.y) + abs(z - other.z)
    }

    private class TrackedState(
        val bedPlates: List<Block>,
        val centerPos: Vec3d,
    )

    private object BlockTracker : AbstractBlockLocationTracker<TrackedState>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): TrackedState? {
            return if (state.block in BED_BLOCKS) {
                val part = BedBlock.getBedPart(state)
                // Only track the first part of the bed
                if (part == DoubleBlockProperties.Type.FIRST) {
                    TrackedState(
                        bedPlates = getBedPlates(pos, state),
                        centerPos = getBedCenterPos(state, pos)
                    )
                } else {
                    null
                }
            } else {
                // A non-bed block was updated, we need to update the bed blocks around it
                updateAllBeds(pos)
                null
            }
        }

        private fun updateAllBeds(pos: BlockPos) {
            trackedBlockMap.forEach { (trackedPos, _) ->
                val trackedPosBlockPos = BlockPos(trackedPos.x, trackedPos.y, trackedPos.z)
                // Update if the block is close to a bed
                if (trackedPosBlockPos.manhattanDistanceTo(pos) <= maxLayers) {
                    val bedState = trackedPosBlockPos.getState() ?: return@forEach
                    if (bedState.block !in BED_BLOCKS) {
                        // The tracked block is not a bed anymore, remove it
                        trackedBlockMap.remove(trackedPos)
                        return@forEach
                    }
                    trackedBlockMap[trackedPos] = TrackedState(
                        bedPlates = getBedPlates(trackedPosBlockPos, bedState),
                        centerPos = getBedCenterPos(bedState, trackedPosBlockPos)
                    )
                }
            }
        }
    }
}
