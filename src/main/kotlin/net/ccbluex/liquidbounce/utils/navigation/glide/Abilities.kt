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

package net.ccbluex.liquidbounce.utils.navigation.glide

import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkSettings
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import kotlin.math.min

/** How the player can glide, captured on the render thread for one search. */
class Abilities(
    val rockets: Int,
    /** Ticks the weakest rocket pushes. */
    val rocketTicks: Int,
    /** Ticks the elytra lasts before it would break. */
    val glideTicks: Int,
) {

    companion object {

        private const val TICKS_PER_DAMAGE = 20
        private const val ROCKET_TICKS_PER_FLIGHT = 10

        fun capture(useRockets: Boolean): Abilities? {
            val elytra = player.getItemBySlot(EquipmentSlot.CHEST)
            if (!LivingEntity.canGlideUsing(elytra, EquipmentSlot.CHEST)) {
                return null
            }

            val glideTicks = if (elytra.isDamageableItem) {
                (elytra.maxDamage - elytra.damageValue - 1) * TICKS_PER_DAMAGE
            } else {
                Int.MAX_VALUE
            }

            var rockets = 0
            var flight = Int.MAX_VALUE
            if (useRockets) {
                for (slot in Slots.OffhandWithHotbar) {
                    val stack = slot.itemStack
                    if (stack.`is`(Items.FIREWORK_ROCKET)) {
                        rockets += stack.count
                        flight = min(flight, stack.get(DataComponents.FIREWORKS)?.flightDuration ?: 1)
                    }
                }
            }

            val rocketTicks = if (rockets > 0) ROCKET_TICKS_PER_FLIGHT * (1 + flight) else 0
            return Abilities(rockets, rocketTicks, glideTicks)
        }
    }

}

internal fun WalkSettings.captureGlide(): Abilities? = if (allowElytra) Abilities.capture(elytraRockets) else null
