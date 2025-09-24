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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket

/**
 * Bypassing Grim 2.3.71
 * @from https://github.com/GrimAnticheat/Grim/issues/2216
 */
internal class NoSlowSharedGrim2371(override val parent: ChoiceConfigurable<*>) : Choice("Grim2371") {

    companion object {
        @JvmStatic
        var shouldPreventNoSlow = false
            private set
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        onCancellation { shouldPreventNoSlow = false }

        repeat(2) {
            waitTicks(1)
            shouldPreventNoSlow = false
            interaction.sendSequencedPacket(world) { sequence ->
                PlayerInteractItemC2SPacket(
                    player.getActiveHand(), sequence,
                    player.yaw, player.pitch
                )
            }
        }

        shouldPreventNoSlow = true
    }

    override fun disable() {
        shouldPreventNoSlow = false
        super.disable()
    }

}
