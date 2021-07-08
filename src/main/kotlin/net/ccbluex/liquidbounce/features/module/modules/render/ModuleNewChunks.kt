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
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.*
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorVertexFormat
import net.ccbluex.liquidbounce.render.engine.memory.putVertex
import net.ccbluex.liquidbounce.render.shaders.InstancedColoredPrimitiveShader
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket
import net.minecraft.util.math.ChunkPos

object ModuleNewChunks : Module("NewChunks", Category.RENDER) {

    private val color by color("Color", Color4b(255, 179, 72, 255))

    private val newlyLoadedChunks = hashSetOf<ChunkPos>()

    val packetHandler = handler<PacketEvent> { event ->
        if (event.packet !is ChunkDataS2CPacket) {
            return@handler
        }

        // todo: check if full chunk is required (has been removed in 1.17)
        this.newlyLoadedChunks.add(ChunkPos(event.packet.x, event.packet.z))
    }

    val renderHandler = handler<EngineRenderEvent> {
        val vertexFormat = PositionColorVertexFormat()

        vertexFormat.initBuffer(4)

        vertexFormat.putVertex { this.position = Vec3(0.0, 0.0, 0.0); this.color = Color4b.WHITE }
        vertexFormat.putVertex { this.position = Vec3(0.0, 0.0, 16.0); this.color = Color4b.WHITE }
        vertexFormat.putVertex { this.position = Vec3(16.0, 0.0, 16.0); this.color = Color4b.WHITE }
        vertexFormat.putVertex { this.position = Vec3(16.0, 0.0, 0.0); this.color = Color4b.WHITE }

        val instanceBuffer = PositionColorVertexFormat()

        instanceBuffer.initBuffer(newlyLoadedChunks.size)

        for (newlyLoadedChunk in newlyLoadedChunks) {
            instanceBuffer.putVertex { this.position = Vec3(newlyLoadedChunk.startX.toDouble(), 0.0, newlyLoadedChunk.startZ.toDouble()); this.color = color }
        }

        RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER_WITHOUT_BOBBING, VertexFormatRenderTask(
            vertexFormat,
            PrimitiveType.LineLoop,
            InstancedColoredPrimitiveShader,
            perInstance = instanceBuffer,
            state = GlRenderState(lineWidth = 1.0F, lineSmooth = true)
        ))
    }

    override fun disable() {
        this.newlyLoadedChunks.clear()
    }
}
