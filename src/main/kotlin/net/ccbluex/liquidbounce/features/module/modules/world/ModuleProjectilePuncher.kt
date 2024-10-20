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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.raytraceBox
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.FireballEntity
import net.minecraft.entity.projectile.ShulkerBulletEntity
import net.minecraft.util.math.MathHelper

/**
 * ProjectilePuncher module
 *
 * Shoots back incoming projectiles around you.
 */
object ModuleProjectilePuncher : Module("ProjectilePuncher", Category.WORLD, aliases = arrayOf("AntiFireball")) {

    private val clickScheduler = tree(ClickScheduler(ModuleProjectilePuncher, false))

    private val swing by boolean("Swing", true)
    private val range by float("Range", 3f, 3f..6f)
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    // Target
    private var target: Entity? = null

    // Rotation
    private val rotations = tree(RotationsConfigurable(this))

    override fun disable() {
        target = null
    }

    val tickHandler = handler<SimulatedTickEvent> {
        if (player.isSpectator) {
            return@handler
        }

        updateTarget()
    }

    val repeatable = repeatable {
        val target = target ?: return@repeatable

        if (target.squaredBoxedDistanceTo(player) > range * range ||
            !facingEnemy(
                toEntity = target,
                rotation = RotationManager.serverRotation,
                range = range.toDouble(),
                wallsRange = 0.0
            )) {
            return@repeatable
        }

        clickScheduler.clicks {
            target.attack(swing)
            true
        }
    }

    private fun updateTarget() {
        val rangeSquared = range * range

        target = null

        for (entity in world.entities.sortedBy { it.squaredBoxedDistanceTo(player) }) {
            if (!shouldAttack(entity)) {
                continue
            }

            val nextTickFireballPosition = entity.pos + entity.pos - entity.prevPos

            val entityBox = entity.dimensions.getBoxAt(nextTickFireballPosition)
            val distanceSquared = entityBox.squaredBoxedDistanceTo(player.eyes)

            if (distanceSquared > rangeSquared) {
                continue
            }

            // find best spot
            val spot = raytraceBox(
                player.eyes, entity.box, range = range.toDouble(), wallsRange = 0.0
            ) ?: continue

            target = entity

            // aim at target
            RotationManager.aimAt(
                spot.rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotations,
                Priority.IMPORTANT_FOR_USER_SAFETY,
                this@ModuleProjectilePuncher
            )
            break
        }
    }

    private fun shouldAttack(entity: Entity): Boolean {
        if (entity !is FireballEntity && entity !is ShulkerBulletEntity) {
            return false
        }

        val fireballVelocity = entity.pos - entity.prevPos

        // If the fireball is not moving the player can obviously not be hit. Additionally the code below only works if
        // the fireball is moving.
        if (MathHelper.approximatelyEquals(fireballVelocity.lengthSquared(), 0.0)) {
            return false
        }

        // Check if the fireball is going towards the player
        val vecToPlayer = player.box.center - entity.pos

        val dot = vecToPlayer.dotProduct(fireballVelocity)

        // if angle less than PI/3 (60 degrees) then
        val isMovingTowardsPlayer = dot > 0.5 * fireballVelocity.length() * vecToPlayer.length()

        val extendedHitbox = player.box.expand(entity.box.lengthX / 2.0)

        // If the fireball was already inside of the player's hitbox, but would be moving away from the player, this
        // would unecessarily trigger the player to attack the fireball.
        val touchesHitbox = extendedHitbox.raycast(entity.pos, fireballVelocity * 20.0).isPresent
        val willHitPlayer = !extendedHitbox.contains(entity.pos) && touchesHitbox

        // We need two checks in order to prevent following situation: The fireball is very close to the player and
        // moving towards their feet. The moving towards player check would fail since the velocity line is not similar
        // enough to the vector to the player. This situation is covered by the second check.
        return isMovingTowardsPlayer || willHitPlayer
    }

}
