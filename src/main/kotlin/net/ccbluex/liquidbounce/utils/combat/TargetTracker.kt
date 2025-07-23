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
package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.ValueType.*
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity

/**
 * A target tracker to choose the best enemy to attack
 */
open class TargetTracker(
    defaultPriority: TargetPriority = TargetPriority.HEALTH,
    rangeValue: RangedValueProvider = NoneRangedValueProvider
) : TargetSelector(defaultPriority, rangeValue) {

    constructor(defaultPriority: TargetPriority = TargetPriority.HEALTH, range: RangedValue<*>) :
        this(defaultPriority, DummyRangedValueProvider(range))

    var target: LivingEntity? = null

    fun selectFirst(predicate: ((LivingEntity) -> Boolean)? = null): LivingEntity? {
        val enemies = targets()
        return (if (predicate != null) enemies.firstOrNull(predicate) else enemies.firstOrNull()).also { target = it }
    }

    fun <R> select(evaluator: (LivingEntity) -> R): R? {
        for (enemy in targets()) {
            val value = evaluator(enemy)
            if (value != null) {
                target = enemy
                return value
            }
        }

        reset()
        return null
    }

    fun reset() {
        target = null
    }

    fun validate(validator: ((LivingEntity) -> Boolean)? = null) {
        val target = target ?: return

        if (!validate(target) || validator != null && !validator(target)) {
            reset()
        }
    }
}

open class TargetSelector(
    defaultPriority: TargetPriority = TargetPriority.HEALTH,
    rangeValue: RangedValueProvider = NoneRangedValueProvider
) : Configurable("Target") {

    constructor(defaultPriority: TargetPriority = TargetPriority.HEALTH, range: RangedValue<*>) :
        this(defaultPriority, DummyRangedValueProvider(range))

    var closestSquaredEnemyDistance: Double = 0.0

    private val range = rangeValue.register(this)
    private val fov by float("FOV", 180f, 0f..180f)
    private val hurtTime by int("HurtTime", 10, 0..10)
    private val priority by enumChoice("Priority", defaultPriority)

    /**
     * Update should be called to always pick the best target out of the current world context
     */
    fun targets(): MutableList<LivingEntity> {
        val entities = world.entities
            .asSequence()
            .filterIsInstance<LivingEntity>()
            .filter(::validate)
            // Sort by distance (closest first) - in case of tie at priority level
            .sortedBy { it.squaredBoxedDistanceTo(player) }
            .toMutableList()

        if (entities.isEmpty()) {
            return entities
        }

        // Sort by entity type
        entities.sortWith(Comparator.comparingInt { entity ->
            when (entity) {
                is PlayerEntity -> 0
                is HostileEntity -> 1
                else -> 2
            }
        })

        when (priority) {
            // Lowest health first
            TargetPriority.HEALTH -> entities.sortBy { it.getActualHealth() }
            // Closest to your crosshair first
            TargetPriority.DIRECTION -> entities.sortBy { RotationUtil.crosshairAngleToEntity(it) }
            // Oldest entity first
            TargetPriority.AGE -> entities.sortByDescending { it.age }
            // With the lowest hurt time first
            TargetPriority.HURT_TIME -> entities.sortBy { it.hurtTime } // Sort by hurt time
            // Closest to you first
            else -> {} // Do nothing
        }

        // Update max distance squared
        closestSquaredEnemyDistance = entities.minOf { it.squaredBoxedDistanceTo(player) }

        return entities
    }

    open fun validate(entity: LivingEntity) =
        entity != player
        && !entity.isRemoved
        && entity.shouldBeAttacked()
        && fov >= RotationUtil.crosshairAngleToEntity(entity)
        && entity.hurtTime <= hurtTime
        && validateRange(entity)

    private fun validateRange(entity: LivingEntity): Boolean {
        if (range == null) return true

        val distanceSq = entity.squaredBoxedDistanceTo(player)
        val range = range.get()

        @Suppress("UNCHECKED_CAST")
        return when (this.range.valueType) {
            FLOAT -> distanceSq <= (range as Float).sq()
            FLOAT_RANGE ->
                distanceSq >= (range as ClosedFloatingPointRange<Float>).start.sq()
                && distanceSq <= range.endInclusive.sq()
            INT -> distanceSq <= (range as Int).sq()
            INT_RANGE -> distanceSq >= (range as IntRange).first.sq() && distanceSq <= range.last.sq()
            else -> true
        }
    }

    val maxRange: Float
        get() {
            if (range == null) return Float.MAX_VALUE

            val value = range.get()

            @Suppress("UNCHECKED_CAST")
            return when (range.valueType) {
                FLOAT -> value as Float
                FLOAT_RANGE -> (value as ClosedFloatingPointRange<Float>).endInclusive
                INT -> (value as Int).toFloat()
                INT_RANGE -> (value as IntRange).last.toFloat()
                else -> Float.MAX_VALUE
            }
        }

}

enum class TargetPriority(override val choiceName: String) : NamedChoice {
    HEALTH("Health"),
    DISTANCE("Distance"),
    DIRECTION("Direction"),
    HURT_TIME("HurtTime"),
    AGE("Age")
}
