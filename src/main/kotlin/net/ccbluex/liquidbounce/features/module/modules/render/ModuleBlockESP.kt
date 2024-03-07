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
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.MultiColorBoxRenderer
import net.ccbluex.liquidbounce.render.SingleColorBoxRenderer
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.item.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

/**
 * BlockESP module
 *
 * Allows you to see selected blocks through walls.
 */

object ModuleBlockESP : Module("BlockESP", Category.RENDER) {

    private val modes = choices("Mode", Box, arrayOf(Box))
    private val targets by blocks("Targets",
        findBlocksEndingWith("_BED", "DRAGON_EGG").toHashSet()).onChange {
        if (enabled) {
            disable()
            enable()
        }
        it
    }

    private val colorMode = choices(
        "ColorMode",
        MapColorMode,
        arrayOf(MapColorMode, StaticColorMode, RainbowColorMode)
    )
    private val fullBox = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

    private object Box : Choice("Box") {
        override val parent: ChoiceConfigurable
            get() = modes

        private val outline by boolean("Outline", true)

        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack

            val colorMode = colorMode.activeChoice as BlockESPColorMode

            colorMode.beginFrame()

            val boxRenderer = MultiColorBoxRenderer()

            renderEnvironmentForWorld(matrixStack) {
                synchronized(BlockTracker.trackedBlockMap) {
                    for (pos in BlockTracker.trackedBlockMap.keys) {
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

                        val color = colorMode.getColorFor(blockPos, blockState)

                        withPositionRelativeToCamera(vec3d) {
                            boxRenderer.drawBox(
                                this,
                                boundingBox,
                                faceColor = color,
                                outlineColor = color.alpha(150).takeIf { outline }
                            )
                        }
                    }
                }

                boxRenderer.draw()
            }
        }

    }

    private interface BlockESPColorMode {
        fun getColorFor(pos: BlockPos, state: BlockState): Color4b
        fun beginFrame() {}
    }

    private object MapColorMode : Choice("MapColor"), BlockESPColorMode {
        override val parent: ChoiceConfigurable
            get() = colorMode

        override fun getColorFor(pos: BlockPos, state: BlockState): Color4b {
            return Color4b(state.getMapColor(mc.world!!, pos).color).alpha(100)
        }
    }
    private object StaticColorMode : Choice("Static"), BlockESPColorMode {
        override val parent: ChoiceConfigurable
            get() = colorMode

        val color by color("Color", Color4b(255, 179, 72, 50))
        override fun getColorFor(pos: BlockPos, state: BlockState): Color4b {
            return color
        }
    }
    private object RainbowColorMode : Choice("Rainbow"), BlockESPColorMode {
        private lateinit var currColor: Color4b

        override val parent: ChoiceConfigurable
            get() = colorMode

        override fun beginFrame() {
            this.currColor = rainbow().alpha(50)
        }

        override fun getColorFor(pos: BlockPos, state: BlockState): Color4b {
            return this.currColor
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
