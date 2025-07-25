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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.minecraft.entity.effect.StatusEffects

object ScaffoldStrafeFeature : ToggleableConfigurable(ModuleScaffold, "Strafe", false) {

    private val speed by float("Speed", 0.247f, 0.0f..5.0f)
    private val hypixel by boolean("Hypixel", false)
    private val onlyOnGround by boolean("OnlyOnGround", false)

    private var moveTicks = 0

    override fun enable() {
        moveTicks = 0
        super.enable()
    }

    override fun disable() {
        if (!hypixel) {
            return
        }
        player.velocity = player.velocity.multiply(
            0.5,
            1.0,
            0.5
        )
        super.disable()
    }

    @Suppress("unused")
    private val moveTickHandler = tickHandler {
        if (player.moving) {
            moveTicks++
            return@tickHandler
        }
        moveTicks = 0
    }

    @Suppress("unused")
    private val strafeHandler = tickHandler {
        if (onlyOnGround && !player.isOnGround) {
            return@tickHandler
        }

        if (hypixel) {
            var speed = 0.207

            if ((player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: -1) >= 0) {
                speed = 0.295
            }

            if (player.age % 20 == 0 || moveTicks <= 7) {
                speed = 0.09800000190734863
            }

            player.velocity = player.velocity.withStrafe(speed = speed)
        } else {
            player.velocity.withStrafe(speed = speed.toDouble())
        }
    }
}
