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
package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.VisualsConfigurable.showCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.canDoCriticalHit
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.modes
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.minecraft.entity.LivingEntity

/**
 * Packet criticals mode
 */
object CriticalsPacket : Choice("Packet") {

    private val mode by enumChoice("Mode", Mode.NO_CHEAT_PLUS)
    private val packetType by enumChoice("PacketType", MovePacketType.FULL)

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        if (event.isCancelled || event.entity !is LivingEntity) {
            return@handler
        }

        val ignoreSprinting = ModuleCriticals.WhenSprinting.shouldAttemptCritWhileSprinting()

        if (!canDoCriticalHit(true, ignoreSprinting)) {
            return@handler
        }

        when (mode) {
            Mode.VANILLA -> {
                p(0.2)
                p(0.01)
                showCriticals(event.entity)
            }

            Mode.NO_CHEAT_PLUS -> {
                p(0.11)
                p(0.1100013579)
                p(0.0000013579)
                showCriticals(event.entity)
            }

            Mode.FALLING -> {
                p(0.0625)
                p(0.0625013579)
                p(0.0000013579)
                showCriticals(event.entity)
            }

            Mode.LOW -> {
                p(1e-9)
                p(0.0)
                showCriticals(event.entity)
            }

            Mode.DOWN -> {
                p(-1e-9)
                showCriticals(event.entity)
            }

            Mode.GRIM -> {
                if (!player.isOnGround) {
                    // If player is in air, go down a little bit.
                    // Vanilla still crits and movement is too small
                    // for simulation checks.

                    // Requires packet type to be .FULL
                    p(-0.000001)

                    showCriticals(event.entity)
                }
            }

            Mode.BLOCKSMC -> {
                if (player.age % 4 == 0) {
                    p(0.0011, true)
                    p(0.0)
                    showCriticals(event.entity)
                }
            }
        }
    }

    private fun p(mod: Double, onGround: Boolean = false) {
        network.sendPacket(packetType.generatePacket().apply {
            this.y += mod
            this.onGround = onGround
        })
    }

    enum class Mode(override val choiceName: String) : NamedChoice {
        VANILLA("Vanilla"),
        NO_CHEAT_PLUS("NoCheatPlus"),
        FALLING("Falling"),
        LOW("Low"),
        DOWN("Down"),
        GRIM("Grim"),
        BLOCKSMC("BlocksMC"),
    }
}
