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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.block.BlockState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

/**
 * BlockESP module
 *
 * Allows you to see selected blocks through walls.
 */

object ModuleBlockESP : Module("BlockESP", Category.RENDER) {

    private val modes = choices("Mode", Glow, arrayOf(Box, Glow, Outline))
    private val targets by blocks(
        "Targets",
        findBlocksEndingWith("_BED", "DRAGON_EGG").toHashSet()
    ).onChange {
        if (enabled) {
            disable()
            enable()
        }
        it
    }

    private val colorMode = choices<GenericColorMode<Pair<BlockPos, BlockState>>>(
        "ColorMode",
        { it.choices[0] },
        {
            arrayOf(
                MapColorMode(it),
                GenericStaticColorMode(it, Color4b(255, 179, 72, 50)),
                GenericRainbowColorMode(it)
            )
        }
    )

    private val fullBox = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

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
                synchronized(BlockTracker.trackedBlockMap) {
                    val trackedBlockMap = BlockTracker.trackedBlockMap

                    dirty = drawInternal(this, trackedBlockMap, colorMode, fullAlpha, drawOutline)
                }
            }

            return dirty
        }

        private fun WorldRenderEnvironment.drawInternal(
            env: WorldRenderEnvironment,
            blocks: MutableMap<AbstractBlockLocationTracker.TargetBlockPos, TrackedState>,
            colorMode: GenericColorMode<Pair<BlockPos, BlockState>>,
            fullAlpha: Boolean,
            drawOutline: Boolean
        ): Boolean {
            var dirty = false

            BoxRenderer.drawWith(env) {
                for (pos in blocks.keys) {
                    val vec3d = Vec3d(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

                    val blockPos = vec3d.toBlockPos()
                    val blockState = blockPos.getState() ?: continue

                    if (blockState.isAir) {
                        continue
                    }

                    val outlineShape = blockState.getOutlineShape(world, blockPos)
                    val boundingBox = if (outlineShape.isEmpty) {
                        fullBox
                    } else {
                        outlineShape.boundingBox
                    }

                    var color = colorMode.getColor(Pair(blockPos, blockState))

                    if (fullAlpha) {
                        color = color.alpha(255)
                    }

                    withPositionRelativeToCamera(vec3d) {
                        drawBox(
                            boundingBox,
                            faceColor = color,
                            outlineColor = color.alpha(150).takeIf { drawOutline }
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

            if (dirty)
                event.markDirty()
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

            if (dirty)
                event.markDirty()
        }
    }

    override fun enable() {
        ChunkScanner.subscribe(BlockTracker)
    }

    override fun disable() {
        ChunkScanner.unsubscribe(BlockTracker)
    }

    private object TrackedState

    private object BlockTracker : AbstractBlockLocationTracker<TrackedState>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): TrackedState? {
            return if (!state.isAir && targets.contains(state.block)) {
                TrackedState
            } else {
                null
            }
        }

    }

}
