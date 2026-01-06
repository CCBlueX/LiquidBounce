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
import net.ccbluex.liquidbounce.render.GenericColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.MapColorMode
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.entity.cameraDistanceSq
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.vertex.PoseStack
import net.ccbluex.liquidbounce.render.drawBoxOutlined
import net.ccbluex.liquidbounce.utils.block.outlineBox
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import java.util.concurrent.ConcurrentSkipListSet

/**
 * BlockESP module
 *
 * Allows you to see selected blocks through walls.
 */

object ModuleBlockESP : ClientModule("BlockESP", Category.RENDER) {

    private val modes = choices("Mode", Glow, arrayOf(Box, Glow, Outline))
    private val targets by blocks(
        "Targets",
        ConcurrentSkipListSet(findBlocksEndingWith("_BED", "DRAGON_EGG"))
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

    private val maximumDistance by float("MaximumDistance", 128F, 1F..512F)

    private object Box : Choice("Box") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val outline by boolean("Outline", true)

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack

            drawBoxMode(mc.mainRenderTarget, matrixStack, this.outline, false)
        }

        fun drawBoxMode(
            framebuffer: RenderTarget,
            matrixStack: PoseStack,
            drawOutline: Boolean,
            isOutlineShader: Boolean,
        ): Boolean {
            var dirty = false

            renderEnvironmentForWorld(matrixStack, framebuffer) {
                dirty = drawInternal(
                    colorMode.activeChoice,
                    isOutlineShader,
                    drawOutline
                )
            }

            return dirty
        }

        private fun WorldRenderEnvironment.drawInternal(
            colorMode: GenericColorMode<Pair<BlockPos, BlockState>>,
            isOutlineShader: Boolean,
            drawOutline: Boolean
        ): Boolean {
            var dirty = false

            startBatch()
            val maxDistanceSq = maximumDistance.sq()
            for ((blockPos, t) in BlockTracker.iterate()) {
                if (blockPos.cameraDistanceSq() > maxDistanceSq) continue

                val blockState = t.state

                if (blockState.isAir) continue

                val boundingBox = t.box

                val color = colorMode.getColor(Pair(blockPos, blockState))

                withPositionRelativeToCamera(blockPos) {
                    if (isOutlineShader) {
                        drawBoxOutlined(boundingBox, color.alpha(255))
                    } else {
                        drawBox(
                            boundingBox,
                            faceColor = color,
                            outlineColor = if (drawOutline) color.with(a = 150) else null,
                        )
                    }
                }

                dirty = true
            }
            commitBatch()

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

            val dirty = Box.drawBoxMode(
                event.framebuffer,
                event.matrixStack,
                drawOutline = false,
                isOutlineShader = true
            )

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

            val dirty = Box.drawBoxMode(
                event.framebuffer,
                event.matrixStack,
                drawOutline = false,
                isOutlineShader = true
            )

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

    private class TrackedState(@JvmField val state: BlockState, @JvmField val box: AABB)

    private object BlockTracker : AbstractBlockLocationTracker.BlockPos2State<TrackedState>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): TrackedState? {
            return if (state.block in targets) {
                TrackedState(state, state.outlineBox(pos))
            } else {
                null
            }
        }
    }

}
