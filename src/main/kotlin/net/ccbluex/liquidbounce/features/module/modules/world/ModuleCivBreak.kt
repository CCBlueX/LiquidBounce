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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleStorageESP
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction

/**
 * CivBreak module
 *
 * Allows you to break same block faster.
 */
object ModuleCivBreak : Module("CivBreak", Category.WORLD, disableOnQuit = true) {

    private val color by color("Color", Color4b(0, 100, 255))

    var pos: BlockPos? = null
    var dir: Direction? = null


    val repeatable = repeatable {
        if (pos != null && dir != null) {
            // Alright, for some reason when we spam STOP_DESTROY_BLOCK
            // server accepts us to destroy the same block instantly over and over.
            network.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, dir))
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerActionC2SPacket && packet.action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            pos = packet.pos
            dir = packet.direction
        }
    }

    // render

    private val fullBox = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

    @Suppress("unused")
    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        if (pos == null || dir == null) return@handler

        renderEnvironmentForWorld(matrixStack) {
            withPositionRelativeToCamera(pos!!.toVec3d()) {
                withColor(color) {
                    drawOutlinedBox(fullBox)
                }
            }
        }
    }

    override fun disable() {
        pos = null
        dir = null
    }


}
