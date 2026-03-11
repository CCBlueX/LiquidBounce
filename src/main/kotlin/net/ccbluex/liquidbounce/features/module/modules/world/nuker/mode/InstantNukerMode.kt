/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.areaMode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.ignoreOpenInventory
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.mode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.swingMode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.wasTarget
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.InteractionHand

object InstantNukerMode : Mode("Instant") {

    override val parent: ModeValueGroup<Mode>
        get() = mode

    private val range by float("Range", 5f, 1f..50f)
    private val bps by intRange("BPS", 40..50, 1..200)
    private val doNotStop by boolean("DoNotStop", false)

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (ModuleBlink.running) {
            return@tickHandler
        }

        if (!ignoreOpenInventory && mc.screen is AbstractContainerScreen<*>) {
            return@tickHandler
        }

        val targets = areaMode.activeMode.lookupTargets(range, count = bps.random())

        if (targets.isEmpty()) {
            wasTarget = null
            waitTicks(1)
            return@tickHandler
        }

        for ((pos, _) in targets) {
            interaction.startPrediction(world) { sequence ->
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    pos,
                    Direction.DOWN,
                    sequence
                )
            }

            swingMode.swing(InteractionHand.MAIN_HAND)
            wasTarget = pos

            if (!doNotStop) {
                interaction.startPrediction(world) { sequence ->
                    ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        pos,
                        Direction.DOWN,
                        sequence
                    )
                }
            }
        }
    }

}
