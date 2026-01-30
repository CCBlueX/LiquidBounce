/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import kotlinx.atomicfu.atomic
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.MapColorMode
import net.ccbluex.liquidbounce.render.RenderPassRenderState
import net.ccbluex.liquidbounce.render.addBoxFaces
import net.ccbluex.liquidbounce.render.addBoxOutlines
import net.ccbluex.liquidbounce.render.buildMesh
import net.ccbluex.liquidbounce.render.drawGenericBlockESP
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.getDynamicTransformsUniform
import net.ccbluex.liquidbounce.render.translate
import net.ccbluex.liquidbounce.render.utils.DistanceFadeUniformValueGroup
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.outlineBox
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.joml.Matrix4fc
import java.util.concurrent.ConcurrentSkipListSet

/**
 * BlockESP module
 *
 * Allows you to see selected blocks through walls.
 */

object ModuleBlockESP : ClientModule("BlockESP", ModuleCategories.RENDER) {

    private val modes = choices("Mode", 0) {
        arrayOf(
            BoxMode,
            OutlineMode("Glow", DrawOutlinesEvent.OutlineType.MINECRAFT_GLOW),
            OutlineMode("Outline", DrawOutlinesEvent.OutlineType.INBUILT_OUTLINE),
        )
    }
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

    private val distanceFade = tree(DistanceFadeUniformValueGroup())

    private sealed class Mode(name: String) : net.ccbluex.liquidbounce.config.types.group.Mode(name) {
        final override val parent get() = modes

        protected var useColor = false
        val dirtyFlag = atomic(true)

        final override fun enable() {
            dirtyFlag.value = true
            super.enable()
        }

        protected fun getDynamicTransformsUniform(
            modelView: Matrix4fc? = null,
            colorModulatorAlpha: Int = -1,
        ) = getDynamicTransformsUniform(
            modelView = modelView,
            colorModulator = if (useColor) {
                Color4b.WHITE
            } else {
                val color = colorMode.activeMode.getColor(BlockPos.ZERO to Blocks.AIR.defaultBlockState())
                if (colorModulatorAlpha == -1) color else color.alpha(colorModulatorAlpha)
            }
        )
    }

    private object BoxMode : Mode("Box") {
        private val outline by boolean("Outline", true).onChanged {
            if (!it && running) {
                outlinesRenderState.clearStates()
            }
        }
        private val facesRenderState = RenderPassRenderState("${ModuleBlockESP.name} $name Faces")
        private val outlinesRenderState = RenderPassRenderState("${ModuleBlockESP.name} $name Outlines")

        override fun disable() {
            facesRenderState.clearStates()
            facesRenderState.clearBuffers()
            outlinesRenderState.clearStates()
            outlinesRenderState.clearBuffers()
            super.disable()
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            if (outline) {
                mc.mainRenderTarget.drawGenericBlockESP(
                    outlinesRenderState,
                    ClientRenderPipelines.relativeLines(useColor),
                    distanceFade,
                ) {
                    getDynamicTransformsUniform(
                        modelView = event.matrixStack.last().pose(),
                        colorModulatorAlpha = 150,
                    )
                }
            }

            mc.mainRenderTarget.drawGenericBlockESP(
                facesRenderState,
                ClientRenderPipelines.relativeQuads(useColor),
                distanceFade,
            ) {
                getDynamicTransformsUniform(modelView = event.matrixStack.last().pose())
            }
        }

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            if (BlockTracker.isEmpty()) {
                facesRenderState.clearStates()
                outlinesRenderState.clearStates()
                return@handler
            }

            if (!dirtyFlag.compareAndSet(expect = true, update = false)) {
                return@handler
            }

            val colorMode = colorMode.activeMode
            useColor = colorMode.isParamSensitive

            facesRenderState.buildMesh(
                pipeline = ClientRenderPipelines.relativeQuads(useColor),
            ) { pose ->
                forEachTrackedBlocks { blockPos, blockState, outlineBox ->
                    val color = if (useColor) colorMode.getColor(blockPos to blockState) else null

                    pose.withPush {
                        translate(blockPos)
                        addBoxFaces(last().pose(), outlineBox, color)
                    }
                }
            }

            if (outline) {
                outlinesRenderState.buildMesh(
                    pipeline = ClientRenderPipelines.relativeLines(useColor),
                ) { pose ->
                    forEachTrackedBlocks { blockPos, blockState, outlineBox ->
                        val color = if (useColor) colorMode.getColor(blockPos to blockState) else null

                        pose.withPush {
                            translate(blockPos)
                            addBoxOutlines(last().pose(), outlineBox, color)
                        }
                    }
                }
            }
        }

    }

    private class OutlineMode(name: String, type: DrawOutlinesEvent.OutlineType) : Mode(name) {
        private val renderState = RenderPassRenderState("${ModuleBlockESP.name} $name")

        override fun disable() {
            renderState.clearStates()
            renderState.clearBuffers()
            super.disable()
        }

        @Suppress("unused")
        private val renderHandler = handler<DrawOutlinesEvent> { event ->
            if (event.type != type) {
                return@handler
            }

            val dirty = event.renderTarget.drawGenericBlockESP(
                renderState,
                ClientRenderPipelines.outlineQuads(useColor),
                distanceFade,
            ) {
                getDynamicTransformsUniform(colorModulatorAlpha = 255)
            }

            if (dirty) {
                event.markDirty()
            }
        }

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            if (BlockTracker.isEmpty()) {
                renderState.clearStates()
                return@handler
            }

            if (!dirtyFlag.compareAndSet(expect = true, update = false)) {
                return@handler
            }

            val colorMode = colorMode.activeMode
            useColor = colorMode.isParamSensitive

            renderState.buildMesh(
                pipeline = ClientRenderPipelines.outlineQuads(useColor),
            ) { pose ->
                forEachTrackedBlocks { blockPos, blockState, outlineBox ->
                    val color = if (useColor) colorMode.getColor(blockPos to blockState) else null

                    pose.withPush {
                        translate(blockPos)
                        addBoxFaces(last().pose(), outlineBox, color?.alpha(255))
                    }
                }
            }
        }

    }

    override fun onEnabled() {
        ChunkScanner.subscribe(BlockTracker)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(BlockTracker)
        markDirtyForModes()
    }

    private fun markDirtyForModes() {
        modes.modes.forEach { it.dirtyFlag.value = true }
    }

    private inline fun forEachTrackedBlocks(
        block: (blockPos: BlockPos, blockState: BlockState, outlineBox: AABB) -> Unit,
    ) {
        for ((blockPos, t) in BlockTracker.iterate()) {
            val blockState = t.state
            val outlineBox = t.box
            block(blockPos, blockState, outlineBox)
        }
    }

    private class TrackedState(@JvmField val state: BlockState, @JvmField val box: AABB)

    private object BlockTracker : AbstractBlockLocationTracker.BlockPos2State<TrackedState>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): TrackedState? {
            return if (!state.isAir && state.block in targets) {
                TrackedState(state, state.outlineBox(pos))
            } else {
                null
            }
        }

        override fun onUpdated() {
            markDirtyForModes()
        }
    }

}
