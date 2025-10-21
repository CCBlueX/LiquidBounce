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
 */

package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow


import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.BowItem

/**
 * @desc Fast charge options (like FastBow) can be used to charge the bow faster.
 * @warning Should only be used on vanilla minecraft. Most anti cheats patch these kinds of exploits
 *
 * TODO: Add version specific options
 */
object AutoBowFastChargeFeature : ToggleableConfigurable(ModuleAutoBow, "FastCharge", false) {

    private val speed by int("Speed", 20, 3..20)

    private val notInTheAir by boolean("NotInTheAir", true)
    private val notDuringMove by boolean("NotDuringMove", false)
    private val notDuringRegeneration by boolean("NotDuringRegeneration", false)

    private val packetType by enumChoice("PacketType", MovePacketType.FULL)

    @Suppress("unused")
    val tickRepeatable = tickHandler {
        val currentItem = player.activeItem

        // Should speed up game ticks when using bow
        if (currentItem?.item is BowItem) {
            if (notInTheAir && !player.isOnGround) {
                return@tickHandler
            }

            if (notDuringMove && player.moving) {
                return@tickHandler
            }

            if (notDuringRegeneration && player.hasStatusEffect(StatusEffects.REGENERATION)) {
                return@tickHandler
            }

            repeat(speed) {
                if (!player.isUsingItem) {
                    return@repeat
                }

                // Speed up ticks (MC 1.8)
                network.sendPacket(packetType.generatePacket())

                // Show visual effect (not required to work - but looks better)
                player.tickActiveItemStack()
            }
        }
    }
}
