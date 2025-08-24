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

import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.minecraft.item.Items
import kotlin.math.hypot

internal object ElytraFlyModeFirework : ElytraFlyMode("Firework") {

    private val minSpeed by float("MinSpeed", 0.8f, 0.1f..2.0f)
    private val fireDelay by float("FireDelay", 1.5f, 0.5f..3.0f, "seconds")

    private var lastFireworkTime = 0L

    private fun findFireworkSlot() = Slots.OffhandWithHotbar.findClosestSlot(Items.FIREWORK_ROCKET)

    private fun getCurrentSpeed(): Double {
        val velocity = player.velocity
        return hypot(velocity.x, velocity.z)
    }

    private fun shouldUseFirework(): Boolean {
        if (!player.isGliding) return false
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastFirework = (currentTime - lastFireworkTime) / 1000.0f
        
        if (timeSinceLastFirework < fireDelay) return false
        
        val currentSpeed = getCurrentSpeed()
        return currentSpeed < minSpeed
    }

    private fun useFirework() {
        val fireworkSlot = findFireworkSlot() ?: return
        
        useHotbarSlotOrOffhand(fireworkSlot)
        lastFireworkTime = System.currentTimeMillis()
    }

    override fun onTick() {
        if (!player.isGliding) return

        if (shouldUseFirework()) {
            useFirework()
        }
    }
}
