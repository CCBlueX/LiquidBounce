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

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.minecraft.entity.attribute.EntityAttributes

internal object NoFallPacket : Choice("Packet") {
    private val packetType by enumChoice("PacketType", MovePacketType.FULL)
    private val filter = choices("Filter", FallDistance, arrayOf(FallDistance, Always))

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    val repeatable = tickHandler {
        if (filter.activeChoice.isActive) {
            network.sendPacket(packetType.generatePacket().apply {
                onGround = true
            })

            if (filter.activeChoice is FallDistance && FallDistance.resetFallDistance) {
                player.onLanding()
            }
        }
    }

    private abstract class Filter(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = filter

        abstract val isActive: Boolean
    }

    private object FallDistance : Filter("FallDistance") {
        override val isActive: Boolean
            get() = player.fallDistance - player.velocity.y > distance.activeChoice.value && player.age > 20

        private val distance = choices("Distance", Smart, arrayOf(Smart, Constant))
        val resetFallDistance by boolean("ResetFallDistance", true)

        private abstract class DistanceMode(name: String) : Choice(name) {
            override val parent: ChoiceConfigurable<*>
                get() = distance

            abstract val value: Float
        }

        private object Smart : DistanceMode("Smart") {
            override val value: Float
                get() = player.getAttributeValue(EntityAttributes.SAFE_FALL_DISTANCE).toFloat()
        }

        private object Constant : DistanceMode("Constant") {
            override val value by float("Value", 2f, 0f..5f)
        }
    }

    private object Always : Filter("Always") {
        override val isActive: Boolean
            get() = true
    }
}
