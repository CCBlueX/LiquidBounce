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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos

/**
 * BlockESP module
 *
 * Allows you to see selected blocks through walls.
 */

object ModuleBlockESP : ClientModule("BlockESP", Category.RENDER) {

    private val modes = choices("Mode", Glow, arrayOf(Box, Glow, Outline))
    private val targets by blocks(
        "Targets",
        findBlocksEndingWith("_BED", "DRAGON_EGG").toHashSet()
    ).onChange {
        if (running) {
            onDisabled()
            onEnabled()
        }
        it
    }

    private val colorMode = choices("ColorMode", 0) {
        arrayOf(
            MapColorMode(it),
            GenericStaticColorMode(it, Color4b(255, 179, 72, 50)),
            GenericRainbowColorMode(it)
        )
    }

    private object Box : Choice("Box") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val outline by boolean("Outline", true)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack

            drawBoxMode(matrixStack, this.outline, false)
        }

        fun drawBoxMode(matrixStack: MatrixStack, drawOutline: Boolean, fullAlpha: Boolean): Boolean {
            val colorMode = colorMode.activeChoice

            var dirty = false

            renderEnvironmentForWorld(matrixStack) {
                dirty = drawInternal(
                    BlockTracker.allPositions(),
                    colorMode,
                    fullAlpha,
                    drawOutline
                )
            }

            return dirty
        }

        private fun WorldRenderEnvironment.drawInternal(
            blocks: Sequence<BlockPos>,
            colorMode: GenericColorMode<Pair<BlockPos, BlockState>>,
            fullAlpha: Boolean,
            drawOutline: Boolean
        ): Boolean {
            var dirty = false

            BoxRenderer.drawWith(this) {
                for (blockPos in blocks) {
                    val blockState = blockPos.getState() ?: continue

                    if (blockState.isAir) {
                        continue
                    }

                    val outlineShape = blockState.getOutlineShape(world, blockPos)
                    val boundingBox = if (outlineShape.isEmpty) {
                        FULL_BOX
                    } else {
                        outlineShape.boundingBox
                    }

                    var color = colorMode.getColor(Pair(blockPos, blockState))

                    if (fullAlpha) {
                        color = color.with(a = 255)
                    }

                    withPositionRelativeToCamera(blockPos.toVec3d()) {
                        drawBox(
                            boundingBox,
                            faceColor = color,
                            outlineColor = color.with(a = 150).takeIf { drawOutline }
                        )
                    }

                    dirty = true
                }
            }

            return dirty
        }
    }

    private object Glow : Choice("Glow") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        val renderHandler = handler<DrawOutlinesEvent> { event ->
            if (event.type != DrawOutlinesEvent.OutlineType.MINECRAFT_GLOW) {
                return@handler
            }

            val dirty = Box.drawBoxMode(event.matrixStack, drawOutline = false, fullAlpha = true)

            if (dirty) {
                event.markDirty()
            }
        }

    }

    private object Outline : Choice("Outline") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        val renderHandler = handler<DrawOutlinesEvent> { event ->
            if (event.type != DrawOutlinesEvent.OutlineType.INBUILT_OUTLINE) {
                return@handler
            }

            val dirty = Box.drawBoxMode(event.matrixStack, drawOutline = false, fullAlpha = true)

            if (dirty) {
                event.markDirty()
            }
        }
    }

    override fun onEnabled() {
        ChunkScanner.subscribe(BlockTracker)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(BlockTracker)
    }

    private object BlockTracker : AbstractBlockLocationTracker.State2BlockPos<Block>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): Block? =
            state.block?.takeIf { it in targets }
    }

}
