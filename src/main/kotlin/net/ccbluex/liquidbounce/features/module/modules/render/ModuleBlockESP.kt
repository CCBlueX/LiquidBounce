/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.RenderEngine
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorVertexFormat
import net.ccbluex.liquidbounce.render.engine.memory.putVertex
import net.ccbluex.liquidbounce.render.utils.drawBoxNew
import net.ccbluex.liquidbounce.render.utils.drawBoxOutlineNew
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.render.espBoxInstancedOutlineRenderTask
import net.ccbluex.liquidbounce.utils.render.espBoxInstancedRenderTask
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

/**
 * BlockESP module
 *
 * Allows you to see selected blocks through walls.
 */

object ModuleBlockESP : Module("BlockESP", Category.RENDER) {

    private val targetedBlocksSetting by blocks("Targets", hashSetOf(Blocks.DRAGON_EGG, Blocks.RED_BED))
    private val color by color("Color", Color4b(255, 179, 72, 255))
    private val colorRainbow by boolean("Rainbow", false)

    val box = drawBoxNew(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), Color4b.WHITE)

    val boxOutline = drawBoxOutlineNew(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), Color4b.WHITE)

    val renderHandler = handler<EngineRenderEvent> { event ->
        val base = if (colorRainbow) rainbow() else color

        val baseColor = Color4b(base.r, base.g, base.b, 50)
        val outlineColor = Color4b(base.r, base.g, base.b, 100)

        val markedBlocks = BlockTracker.trackedBlockMap.keys

        val instanceBuffer = PositionColorVertexFormat()
        val instanceBufferOutline = PositionColorVertexFormat()

        instanceBuffer.initBuffer(markedBlocks.size)
        instanceBufferOutline.initBuffer(markedBlocks.size)

        for (pos in markedBlocks) {
            val pos3 = Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

            instanceBuffer.putVertex { this.position = pos3; this.color = baseColor }
            instanceBufferOutline.putVertex { this.position = pos3; this.color = outlineColor }
        }

        RenderEngine.enqueueForRendering(
            RenderEngine.CAMERA_VIEW_LAYER,
            espBoxInstancedRenderTask(instanceBuffer, box.first, box.second)
        )
        RenderEngine.enqueueForRendering(
            RenderEngine.CAMERA_VIEW_LAYER,
            espBoxInstancedOutlineRenderTask(instanceBufferOutline, boxOutline.first, boxOutline.second)
        )
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
            return if (targetedBlocksSetting.contains(state.block)) {
                TrackedState
            } else {
                null
            }
        }

    }

}
