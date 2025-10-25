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

package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.grim

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.entity.direction
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import kotlin.math.cos
import kotlin.math.sin

class SpeedGrimCollide(override val parent: ChoiceConfigurable<*>) : Choice("GrimCollide") {

    private val speed by float("BoostSpeed", 0.08F, 0.01F..0.08F, "b/t")

    /**
     * Grim Collide mode for the Speed module.
     * The simulation when colliding with another player basically gives lenience.
     *
     * We can exploit this by increasing our speed by
     * 0.08 when we collide with any entity.
     *
     * This only works on client version being 1.9+.
     */
    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        if (player.input.movementForward == 0.0f && player.input.movementSideways == 0.0f) {
            return@handler
        }

        var collisions = 0
        val box = player.boundingBox.expand(1.0)

        for (entity in world.entities) {
            val entityBox = entity.boundingBox

            if (canCauseSpeed(entity) && box.intersects(entityBox)) {
                collisions++
            }
        }

        // Grim gives 0.08 leniency per entity which is customizable by speed.
        val yaw = Math.toRadians(player.direction.toDouble())
        val boost = this.speed * collisions
        player.addVelocity(-sin(yaw) * boost, 0.0, cos(yaw) * boost)
    }

    private fun canCauseSpeed(entity: Entity) =
        entity != player && entity is LivingEntity && entity !is ArmorStandEntity

}
