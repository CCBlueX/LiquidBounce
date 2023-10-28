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

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.raytraceBox
import net.ccbluex.liquidbounce.utils.client.isOldCombat
import net.ccbluex.liquidbounce.utils.combat.CpsScheduler
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.FireballEntity
import net.minecraft.entity.projectile.ShulkerBulletEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand

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

    val tickHandler = handler<PlayerNetworkMovementTickEvent> {
        if (it.state != EventState.PRE || player.isSpectator) {
            return@handler
        }

        updateTarget()
    }

    val repeatable = repeatable {
        val target = targetTracker.lockedOnTarget ?: return@repeatable
        val rotation = RotationManager.currentRotation ?: return@repeatable

        val clicks = cpsTimer.clicks(condition = {
            target.boxedDistanceTo(player) <= range && facingEnemy(
                target, rotation, range.toDouble(), 0.0
            )
        }, cps)

        repeat(clicks) {
            attackEntity(target)
        }
    }

    private fun updateTarget() {
        val player = mc.player ?: return
        val world = mc.world ?: return

        val rangeSquared = range * range

        targetTracker.validateLock { it.squaredBoxedDistanceTo(player) <= rangeSquared }

        for (entity in world.entities) {
            if (entity !is FireballEntity && entity !is ShulkerBulletEntity) {
                continue
            }

            val distanceSquared = entity.squaredBoxedDistanceTo(player)


            // Avoid fireball if its speed-predicted next position goes further than the normal distance.
            // Useful in preventing the user from changing their own thrown fireball's direction
            if (distanceSquared > rangeSquared || !shouldAttack(entity)) {
                continue
            }

            // find best spot
            val spot = raytraceBox(
                player.eyes, entity.box, range = range.toDouble(), wallsRange = 0.0
            ) ?: continue

            // lock on target tracker
            targetTracker.lock(entity)

            // aim at target
            RotationManager.aimAt(spot.rotation, openInventory = ignoreOpenInventory, configurable = rotations)
            break
        }
    }

    private fun shouldAttack(entity: Entity): Boolean {
        if (entity !is FireballEntity)
            return true

        // Check if the fireball is going towards the player
        val vecToPlayer = entity.pos.subtract(player.pos)

        val dot = vecToPlayer.dotProduct(player.velocity)

        return dot > 0.0
    }

    private fun attackEntity(entity: Entity) {
        EventManager.callEvent(AttackEvent(entity))

        // Swing before attacking (on 1.8)
        if (swing && isOldCombat) {
            player.swingHand(Hand.MAIN_HAND)
        }

        network.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, player.isSneaking))

        // Swing after attacking (on 1.9+)
        if (swing && !isOldCombat) {
            player.swingHand(Hand.MAIN_HAND)
        }

        // Reset cool down
        player.resetLastAttackedTicks()
    }
}
