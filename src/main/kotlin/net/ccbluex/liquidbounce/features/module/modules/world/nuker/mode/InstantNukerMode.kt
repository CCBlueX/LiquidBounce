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

package net.ccbluex.liquidbounce.features.module.modules.world.nuker.mode

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.areaMode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.ignoreOpenInventory
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.mode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.swingMode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.wasTarget
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.Direction

object InstantNukerMode : Choice("Instant") {

    override val parent: ChoiceConfigurable<Choice>
        get() = mode

    private val range by float("Range", 5f, 1f..50f)
    private val bps by intRange("BPS", 40..50, 1..200)
    private val doNotStop by boolean("DoNotStop", false)

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (ModuleBlink.running) {
            return@tickHandler
        }

        if (!ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            return@tickHandler
        }

        val targets = areaMode.activeChoice.lookupTargets(range, count = bps.random())

        if (targets.none()) {
            wasTarget = null
            waitTicks(1)
            return@tickHandler
        }

        for ((pos, _) in targets) {
            network.sendPacket(
                PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN)
            )

            swingMode.swing(Hand.MAIN_HAND)
            wasTarget = pos

            if (!doNotStop) {
                network.sendPacket(
                    PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN)
                )
            }
        }
    }

}
