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

package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow


import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.BowItem

/**
 * @desc Fast charge options (like FastBow) can be used to charge the bow faster.
 * @warning Should only be used on vanilla minecraft. Most anti cheats patch these kinds of exploits
 *
 * TODO: Add version specific options
 */
object AutoBowFastChargeFeature : ToggleableValueGroup(ModuleAutoBow, "FastCharge", false) {

    private val speed by int("Speed", 20, 3..20)

    private val notInTheAir by boolean("NotInTheAir", true)
    private val notDuringMove by boolean("NotDuringMove", false)
    private val notDuringRegeneration by boolean("NotDuringRegeneration", false)

    private val packetType by enumChoice("PacketType", MovePacketType.FULL)

    @Suppress("unused")
    private val tickRepeatable = tickHandler {
        val currentItem = if (player.isUsingItem) player.useItem else return@tickHandler

        // Should speed up game ticks when using bow
        if (currentItem?.item is BowItem) {
            if (notInTheAir && !player.onGround()) {
                return@tickHandler
            }

            if (notDuringMove && player.moving) {
                return@tickHandler
            }

            if (notDuringRegeneration && player.hasEffect(MobEffects.REGENERATION)) {
                return@tickHandler
            }

            repeat(speed) {
                if (!player.isUsingItem) {
                    return@repeat
                }

                // Speed up ticks (MC 1.8)
                network.send(packetType.generatePacket())

                // Show visual effect (not required to work - but looks better)
                player.updatingUsingItem()
            }
        }
    }
}
