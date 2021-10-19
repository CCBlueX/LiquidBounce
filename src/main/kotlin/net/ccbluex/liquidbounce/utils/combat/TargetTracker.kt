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

package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.eyesPos
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity

/**
 * A target tracker to choose the best enemy to attack
 */
class TargetTracker(defaultPriority: PriorityEnum = PriorityEnum.HEALTH) : Configurable("Target") {

    var lockedOnTarget: Entity? = null
    var maxDistanceSquared: Double = 0.0

    val priority by enumChoice("Priority", defaultPriority, PriorityEnum.values())
    val lockOnTarget by boolean("LockOnTarget", false)
    val sortOut by boolean("SortOut", true)
    val delayableSwitch by intRange("DelayableSwitch", 10..20, 0..40)

    /**
     * Update should be called to always pick the best target out of the current world context
     */
    fun enemies(enemyConf: EnemyConfigurable = globalEnemyConfigurable): Iterable<Entity> {
        val player = mc.player!!

        val entities = mc.world!!.entities
            .filter { it.shouldBeAttacked(enemyConf) }
            .sortedBy { player.squaredBoxedDistanceTo(it) } // Sort by distance

        entities.lastOrNull()?.let { maxDistanceSquared = it.squaredBoxedDistanceTo(player) }

        val eyePos = player.eyesPos

        when (priority) {
            PriorityEnum.HEALTH -> entities.sortedBy { if (it is LivingEntity) it.health else 0f } // Sort by health
            PriorityEnum.DIRECTION -> entities.sortedBy { RotationManager.rotationDifference(RotationManager.makeRotation(it.boundingBox.center, eyePos)) } // Sort by FOV
            PriorityEnum.AGE -> entities.sortedBy { -it.age } // Sort by existence
        }

        return entities.asIterable()
    }

    fun cleanup() {
        lockedOnTarget = null
    }

    fun lock(entity: Entity) {
        lockedOnTarget = entity
    }

    fun validateLock(validator: (Entity) -> Boolean) {
        if (!validator(lockedOnTarget ?: return)) {
            lockedOnTarget = null
        }
    }

}

enum class PriorityEnum(override val choiceName: String) : NamedChoice {
    HEALTH("Health"),
    DISTANCE("Distance"),
    DIRECTION("Direction"),
    HURT_TIME("HurtTime"),
    AGE("Age")
}
