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

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.PlayerMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.raytraceBox
import net.ccbluex.liquidbounce.utils.combat.CpsScheduler
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.entity.*
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.FireballEntity
import net.minecraft.entity.projectile.ShulkerBulletEntity
import kotlin.math.cos

/**
 * ProjectilePuncher module
 *
 * Shoots back incoming projectiles around you.
 */

object ModuleProjectilePuncher : Module("ProjectilePuncher", Category.WORLD) {

    private val cps by intRange("CPS", 5..8, 1..20)
    private val swing by boolean("Swing", true)
    private val range by float("Range", 3f, 3f..6f)
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    // Target
    private val targetTracker = tree(TargetTracker())

    // Rotation
    private val rotations = tree(RotationsConfigurable())

    private val cpsTimer = tree(CpsScheduler())

    override fun disable() {
        targetTracker.cleanup()
    }

    val tickHandler = handler<PlayerMovementTickEvent> {
        if (player.isSpectator) {
            return@handler
        }

        updateTarget()
    }

    val repeatable = repeatable {
        val target = targetTracker.lockedOnTarget ?: return@repeatable

        val condition = target.boxedDistanceTo(player) <= range &&
            facingEnemy(toEntity = target, rotation = RotationManager.serverRotation, range = range.toDouble(),
                wallsRange = 0.0)
        val clicks = cpsTimer.clicks(condition = { condition }, cps)

        repeat(clicks) {
            target.attack(swing)
        }
    }

    private fun updateTarget() {
        val rangeSquared = range * range

        targetTracker.validateLock { it.squaredBoxedDistanceTo(player) <= rangeSquared }

        for (entity in world.entities) {
            if (!shouldAttack(entity)) {
                continue
            }

            val nextTickFireballPosition = entity.pos.add(entity.pos.subtract(entity.prevPos))

            val entityBox = entity.dimensions.getBoxAt(nextTickFireballPosition)
            val distanceSquared = entityBox.squaredBoxedDistanceTo(player.eyes)

            if (distanceSquared > rangeSquared) {
                continue
            }

            // find best spot
            val spot = raytraceBox(
                player.eyes, entity.box, range = range.toDouble(), wallsRange = 0.0
            ) ?: continue

            // lock on target tracker
            targetTracker.lock(entity)

            // aim at target
            RotationManager.aimAt(spot.rotation, considerInventory = !ignoreOpenInventory, configurable = rotations)
            break
        }
    }

    private fun shouldAttack(entity: Entity): Boolean {
        if (entity !is FireballEntity && entity !is ShulkerBulletEntity)
            return false

        // Check if the fireball is going towards the player
        val vecToPlayer = player.pos.subtract(entity.pos)

        val dot = vecToPlayer.dotProduct(entity.pos.subtract(entity.prevPos))

        return dot > -cos(Math.toRadians(30.0))
    }

}
