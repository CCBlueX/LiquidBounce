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
package net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes

import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.minecraft.util.math.MathHelper
import kotlin.math.max
import kotlin.math.min

internal object ElytraFlyModePitch40Infinite : ElytraFlyMode("Pitch40Infinite") {

    private val minSpeed by float("MinSpeed", 25f, 10f..70f)
    private val maxSpeed by float("MaxSpeed", 150f, 50f..170f)
    private val maxHeight by int("MaxHeight", 200, 50..360)
    private var infinitePitch = 0f
    private var infiniteFlag = false

    override fun enable() {
        infinitePitch = 0f
        infiniteFlag = false
        if (player.y < maxHeight) {
            // Can disable module or show warning
            // For example: ModuleElytraFly.disable("Go above $maxHeight height!")
        }
    }

    override fun onTick() {
        if (!player.isGliding) {
            return
        }

        val speed = player.velocity.horizontalLength() * 72f
        if (player.y < maxHeight) {
            if (speed < minSpeed && !infiniteFlag) {
                infiniteFlag = true
            }
            if (speed > maxSpeed && infiniteFlag) {
                infiniteFlag = false
            }
        } else {
            infiniteFlag = true
        }

        infinitePitch += if (infiniteFlag) 3f else -3f
        infinitePitch = MathHelper.clamp(infinitePitch, -40f, 40f)

        player.pitch = infinitePitch
    }
} 
