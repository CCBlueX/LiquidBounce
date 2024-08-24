/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.TargetChangeEvent
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.web.socket.protocol.rest.game.PlayerData
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity

/**
 * A target tracker to choose the best enemy to attack
 */
class TargetTracker(
    defaultPriority: PriorityEnum = PriorityEnum.HEALTH
) : Configurable("Target") {

    var lockedOnTarget: LivingEntity? = null
        private set
    var maximumDistance: Double = 0.0

    private val fov by float("FOV", 180f, 0f..180f)
    private val hurtTime by int("HurtTime", 10, 0..10)
    private val priority by enumChoice("Priority", defaultPriority)

    /**
     * Update should be called to always pick the best target out of the current world context
     */
    fun enemies(): List<LivingEntity> {
        var entities = world.entities
            .filterIsInstance<LivingEntity>()
            .filter(this::validate)
            // Sort by distance (closest first) - in case of tie at priority level
            .sortedBy { it.boxedDistanceTo(player) }

        entities = when (priority) {
            // Lowest health first
            PriorityEnum.HEALTH -> entities.sortedBy { it.getActualHealth() }
            // Closest to your crosshair first
            PriorityEnum.DIRECTION -> entities.sortedBy { RotationManager.rotationDifference(it) }
            // Oldest entity first
            PriorityEnum.AGE -> entities.sortedBy { -it.age }
            // With the lowest hurt time first
            PriorityEnum.HURT_TIME -> entities.sortedBy { it.hurtTime } // Sort by hurt time
            // Closest to you first
            else -> entities
        }

        // Update max distance squared
        entities.minByOrNull { it.squaredBoxedDistanceTo(player) }
            ?.let { maximumDistance = it.squaredBoxedDistanceTo(player) }

        return entities
    }

    fun cleanup() {
        unlock()
    }

    fun lock(entity: LivingEntity, reportToUI: Boolean = true) {
        lockedOnTarget = entity

        if (entity is PlayerEntity && reportToUI) {
            EventManager.callEvent(TargetChangeEvent(PlayerData.fromPlayer(entity)))
        }
    }

    private fun unlock() {
        lockedOnTarget = null
    }

    fun validateLock(validator: (Entity) -> Boolean) {
        val lockedOnTarget = lockedOnTarget ?: return

        if (!validate(lockedOnTarget) || !validator(lockedOnTarget)) {
            this.lockedOnTarget = null
        }
    }

    private fun validate(entity: LivingEntity)
            = entity != player
            && !entity.isRemoved
            && entity.shouldBeAttacked()
            && fov >= RotationManager.rotationDifference(entity)
            && entity.hurtTime <= hurtTime

}

enum class PriorityEnum(override val choiceName: String) : NamedChoice {
    HEALTH("Health"),
    DISTANCE("Distance"),
    DIRECTION("Direction"),
    HURT_TIME("HurtTime"),
    AGE("Age")
}
