/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * Zoot module
 *
 * Accelerate game time to stop bad effects and fire faster
 */
object ModuleZoot : Module("Zoot", Category.PLAYER) {

    val badEffects by boolean("BadEffects", true)
    val fire by boolean("Fire", true)
    val noAir by boolean("NoAir", false)

    val repeatable = repeatable {
        if (player.isOnGround || !noAir) {
            if (fire && !player.abilities.creativeMode && player.isOnFire) {
                // Accelerate game time (1.8.X)
                repeat(9) {
                    network.sendPacket(PlayerMoveC2SPacket(player.isOnGround))
                }

                // Skip to next tick
                return@repeatable
            }

            if (badEffects) {
                val (effect, status) = player.activeStatusEffects.maxByOrNull { it.value.duration }
                    ?: return@repeatable

                if (!effect.isBeneficial && !status.isPermanent) {
                    // Accelerate game time (1.8.X)
                    repeat(status.duration / 20) {
                        network.sendPacket(PlayerMoveC2SPacket(player.isOnGround))
                    }
                }
            }
        }
    }
}
