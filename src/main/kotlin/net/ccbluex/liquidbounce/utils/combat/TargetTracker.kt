/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleMurderMystery
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity

/**
 * A target tracker to choose the best enemy to attack
 */
class TargetTracker(defaultPriority: PriorityEnum = PriorityEnum.HEALTH) : Configurable("Target") {

    var lockedOnTarget: Entity? = null
    var maxDistanceSquared: Double = 0.0

    val fov by float("FOV", 180f, 0f..180f)
    val priority by enumChoice("Priority", defaultPriority, PriorityEnum.values())

    /**
     * Update should be called to always pick the best target out of the current world context
     */
    fun enemies(enemyConf: EnemyConfigurable = globalEnemyConfigurable): Iterable<Entity> {
        val player = mc.player ?: return emptyList()
        val world = mc.world ?: return emptyList()

        var entities = world.entities.filter {
            it.shouldBeAttacked(enemyConf) && fov >= RotationManager.rotationDifference(it) &&
                (!ModuleMurderMystery.enabled || (it is AbstractClientPlayerEntity && ModuleMurderMystery.shouldAttack(it)))
        }

        entities = when (priority) {
            PriorityEnum.HEALTH -> entities.sortedBy { if (it is LivingEntity) it.health else 0f } // Sort by health
            PriorityEnum.DIRECTION -> entities.sortedBy { RotationManager.rotationDifference(it) } // Sort by FOV
            PriorityEnum.AGE -> entities.sortedBy { -it.age } // Sort by existence
            PriorityEnum.DISTANCE -> entities.sortedBy { it.squaredBoxedDistanceTo(player) } // Sort by distance
            PriorityEnum.HURT_TIME -> entities.sortedBy { if (it is LivingEntity) it.hurtTime else 0 } // Sort by hurt time
        }

        entities.minByOrNull { it.squaredBoxedDistanceTo(player) }
            ?.let { maxDistanceSquared = it.squaredBoxedDistanceTo(player) }

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
    HEALTH("Health"), DISTANCE("Distance"), DIRECTION("Direction"), HURT_TIME("HurtTime"), AGE("Age")
}
