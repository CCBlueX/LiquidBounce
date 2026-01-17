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

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.drawPlane
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max

/**
 * NewChunks module
 *
 * Highlights chunks that are likely newly generated.
 */
object ModuleNewChunks : ClientModule("NewChunks", ModuleCategories.RENDER) {

    private val renderDistance by int("RenderDistance", 32, 4..128, "chunks")
    private val renderY by float("RenderY", 0.0f, -64.0f..320.0f)
    private val autoY by boolean("AutoY", false)

    private val smooth by boolean("Smooth", true)
    private val persist by boolean("Persist", true)

    private val newColor by color("NewColor", Color4b(0, 255, 0, 80))
    private val oldColor by color("OldColor", Color4b(255, 0, 0, 80))

    private val chunks = ConcurrentHashMap<ChunkPos, Boolean>()

    private fun reset() {
        chunks.clear()
    }

    override fun onDisabled() = reset()

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is ClientboundForgetLevelChunkPacket -> {
                if (!persist) {
                    chunks.remove(packet.pos)
                }
            }

            is ClientboundLevelChunkWithLightPacket -> {
                val pos = ChunkPos(packet.x, packet.z)

                chunks.putIfAbsent(pos, false)
            }

            is ClientboundSectionBlocksUpdatePacket -> {
                packet.runUpdates { bp, state ->
                    val fluid = state.fluidState
                    if (!fluid.isEmpty && !fluid.isSource) {
                        chunks[ChunkPos(bp)] = true
                    }
                }
            }

            is ClientboundBlockUpdatePacket -> {
                val fluid = packet.blockState.fluidState
                if (!fluid.isEmpty && !fluid.isSource) {
                    chunks[ChunkPos(packet.pos)] = true
                }
            }
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (chunks.isEmpty()) return@handler

        val maxDist = renderDistance.toDouble() * 16.0
        val renderDistSq = maxDist * maxDist

        val drawY = if (autoY) player.y - 100.0 else renderY.toDouble()

        renderEnvironmentForWorld(event.matrixStack) {
            startBatch()
            for ((chunk, isNew) in chunks) {
                val chunkX = chunk.minBlockX
                val chunkZ = chunk.minBlockZ

                if (player.distanceToSqr(chunkX + 8.0, player.y, chunkZ + 8.0) > renderDistSq) {
                    continue
                }

                var color = if (isNew) newColor else oldColor

                if (smooth && !isNew) {
                    var totalWeight = 0.0

                    for (dx in -2..2) {
                        for (dz in -2..2) {
                            if (dx == 0 && dz == 0) continue

                            if (chunks[ChunkPos(chunk.x + dx, chunk.z + dz)] == true) {
                                val dist = max(abs(dx), abs(dz))

                                totalWeight += (3.0 - dist) / 2.0
                            }
                        }
                    }

                    if (totalWeight > 0.0) {
                        val ratio = (totalWeight / 12.0).coerceAtMost(1.0)

                        color = oldColor.interpolateTo(newColor, ratio)
                    }
                }

                withPositionRelativeToCamera(Vec3(chunkX.toDouble(), drawY, chunkZ.toDouble())) {
                    drawPlane(16f, 16f, color, color.darker())
                }
            }
            commitBatch()
        }
    }
}
