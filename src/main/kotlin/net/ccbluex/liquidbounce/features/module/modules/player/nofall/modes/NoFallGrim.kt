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

package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * Bypassing GrimAC Anti Cheat(8/3/2025,Loyisa Server)
 * Minecraft Version 1.9+
 *
 * @author XeContrast
 */
internal object NoFallGrim : Choice("Grim19") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    private var start = false

    @Suppress("unused")
    val playerNetworkMovementTickEvent = handler<PlayerNetworkMovementTickEvent> {
        if (start && player.isOnGround) {
            it.cancelEvent()
            network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(true,player.horizontalCollision))
        }
    }

    val repeatable = tickHandler {
        if (player.fallDistance > 2.5) {
            start = true
        }
    }

    @Suppress("unused")
    val movementInputEvent = handler<MovementInputEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
        if (start && player.isOnGround) {
            it.jump = true
            player.onLanding()
            start = false
        }
    }

    override fun enable() {
        start = false
    }

}
