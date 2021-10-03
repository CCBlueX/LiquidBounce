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

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.MC_1_8
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.eyesPos
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

    private val swing by boolean("Swing", true)
    private val range by float("Range", 3f, 3f..6f)

    // Target
    private val targetTracker = tree(TargetTracker())

    // Rotation
    private val rotations = RotationsConfigurable()

    override fun disable() {
        targetTracker.cleanup()
    }

    val repeatable = repeatable {
        attack()
    }

    private fun attack() {
        if (player.isSpectator) {
            return
        }

        val squaredRange = range * range

        targetTracker.validateLock { it.squaredBoxedDistanceTo(player) <= squaredRange }

        for (entity in world.entities) {
            if (entity is FireballEntity || entity is ShulkerBulletEntity) {
                if (entity.squaredBoxedDistanceTo(player) > squaredRange) {
                    continue
                }

                // find best spot (and skip if no spot was found)
                val (rotation, _) = RotationManager.raytraceBox(
                    player.eyesPos,
                    entity.boundingBox,
                    range = range.toDouble(),
                    wallsRange = 0.0
                ) ?: continue

                // lock on target tracker
                targetTracker.lock(entity)

                // aim at target
                RotationManager.aimAt(rotation, configurable = rotations)
                break
            }
        }

        val entity = targetTracker.lockedOnTarget ?: return
        attackEntity(entity)
        targetTracker.cleanup()
    }

    private fun attackEntity(entity: Entity) {
        EventManager.callEvent(AttackEvent(entity))

        // Swing before attacking (on 1.8)
        if (swing && protocolVersion == MC_1_8) {
            player.swingHand(Hand.MAIN_HAND)
        }

        network.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, player.isSneaking))

        // Swing after attacking (on 1.9+)
        if (swing && protocolVersion != MC_1_8) {
            player.swingHand(Hand.MAIN_HAND)
        }

        // Reset cool down
        player.resetLastAttackedTicks()
    }
}
