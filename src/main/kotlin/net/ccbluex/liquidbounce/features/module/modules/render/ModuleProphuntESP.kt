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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.entity.FallingBlockEntity
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket

object ModuleProphuntESP : ClientModule("ProphuntESP", Category.RENDER,
    aliases = arrayOf("BlockUpdateDetector", "FallingBlockESP")) {

    private val renderer = PlacementRenderer("RenderBlockUpdates", true, this,
        defaultColor = Color4b(255, 179, 72, 90), keep = false
    )

    private val tracking by multiEnumChoice("Tracking", Tracking.entries, canBeNone = false)

    private enum class Tracking(override val choiceName: String): NamedChoice {
        FALLING_BLOCKS("FallingBlocks"),
        BLOCK_UPDATES("BlockUpdates"),
        CHUNK_DELTA_UPDATES("ChunkDeltaUpdates"),
    }

    init {
        tree(renderer)
    }

    override fun onDisabled() {
        renderer.clearSilently()
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (Tracking.FALLING_BLOCKS in tracking) {
            for (entity in world.entities) {
                if (entity is FallingBlockEntity) {
                    renderer.addBlock(entity.blockPos, update = false)
                }
            }
        }
        renderer.updateAll()
    }

    @Suppress("unused")
    private val networkHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        when {
            packet is BlockUpdateS2CPacket && Tracking.BLOCK_UPDATES in tracking -> mc.execute {
                renderer.addBlock(packet.pos, update = false)
            }
            packet is ChunkDeltaUpdateS2CPacket && Tracking.CHUNK_DELTA_UPDATES in tracking -> mc.execute {
                packet.visitUpdates { pos, _ -> renderer.addBlock(pos, update = false) }
            }
        }
    }
}
