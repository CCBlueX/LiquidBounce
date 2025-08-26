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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * NoClip module
 *
 * Allows you to fly through blocks.
 */
object ModuleNoClip : ClientModule("NoClip", Category.MOVEMENT) {

    val speed by float("Speed", 0.32f, 0.1f..0.4f)
    private val onlyInVehicle by boolean("OnlyInVehicle", false)
    private val disableOnSetback by boolean("DisableOnSetback", true)

    private var noClipSet = false

    @Suppress("unused")
    private val handleGameTick = tickHandler {
        if (paused()) {
            if (noClipSet) {
                onDisabled()
            }

            return@tickHandler
        }

        noClipSet = true
        player.noClip = true
        player.fallDistance = 0f
        player.isOnGround = false

        val speed = speed.toDouble()
        player.controllingVehicle?.let {
            it.noClip = true

            if (!ModuleVehicleControl.running) {
                it.velocity = it.velocity.withStrafe(speed = speed)
                it.velocity.y = when {
                    mc.options.jumpKey.isPressed -> speed
                    mc.options.sneakKey.isPressed -> -speed
                    else -> 0.0
                }
            }
        } ?: run {
            player.velocity = player.velocity.withStrafe(speed = speed)

            player.velocity.y = when {
                mc.options.jumpKey.isPressed -> speed
                mc.options.sneakKey.isPressed -> -speed
                else -> 0.0
            }
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        // Setback detection
        if (event.packet is PlayerPositionLookS2CPacket && disableOnSetback && !paused()) {
            chat(markAsError(this.message("setbackDetected")))
            enabled = false
        }
    }

    override fun onDisabled() {
        noClipSet = false
        player.noClip = false
        player.controllingVehicle?.let { it.noClip = false }
    }

    fun paused() = onlyInVehicle && mc.player?.controllingVehicle == null

}
