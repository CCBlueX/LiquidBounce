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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.sentinel

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * @anticheat Sentinel
 * @anticheatVersion 10.03.2024
 * @testedOn cubecraft.net
 *
 * @note Tested in SkyWars and EggWars, works fine and no automatic ban.
 * It will auto-ban only when flying very large distances.
 *
 * Thanks to icewormy3
 */
internal object FlySentinel10thMar : Choice("Sentinel10thMar") {

    private val jumpHeight by float("Height", 0.42f, 0.1f..1f)
    private val jumpSpeed by float("Speed", 0.35f, 0.1f..1f)
    private val ticks by int("Ticks", 11, 1..20)

    private var spoofOnGround = false

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    val repeatable = tickHandler {
        player.velocity.y = jumpHeight.toDouble()
        player.velocity = player.velocity.withStrafe(speed = jumpSpeed.toDouble())
        spoofOnGround = true
        waitTicks(ticks)
    }

    val moveHandler = handler<MovementInputEvent> {
        it.jump = false
    }

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerMoveC2SPacket) {
            if (spoofOnGround) {
                packet.onGround = true
                spoofOnGround = false
            }
        }
    }

}
