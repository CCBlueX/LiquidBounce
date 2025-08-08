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

package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.NoSlowMode
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.Hand

/**
 * Bypassing Grim 2.3.71
 * @from https://github.com/GrimAnticheat/Grim/issues/2216
 */
internal class NoSlowSharedGrim2371(override val parent: ChoiceConfigurable<*>) : NoSlowMode("Grim2371",parent) {
    val repeatable = tickHandler {
        working = false

        waitTicks(2)
        for (i in 0..3) {
            waitTicks(1)
            if (i > 1) {
                working = true
                val hand: Hand = Hand.MAIN_HAND.takeIf { player.getActiveHand() == Hand.MAIN_HAND } ?: Hand.OFF_HAND
                interaction.sendSequencedPacket(world) { sequence ->
                    // This time we use a new sequence
                    PlayerInteractItemC2SPacket(
                        hand, sequence,
                        player.yaw, player.pitch
                    )
                }
            } else {
                working = false
            }
        }
        working = false
    }
}
