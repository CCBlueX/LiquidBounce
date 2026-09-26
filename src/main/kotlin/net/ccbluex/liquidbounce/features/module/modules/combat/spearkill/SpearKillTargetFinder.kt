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
package net.ccbluex.liquidbounce.features.module.modules.combat.spearkill

import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.isWithinWorldBorder
import net.ccbluex.liquidbounce.utils.entity.useItem
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.component.AttackRange
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/**
 * Finds the best [ModuleSpearKill] target: the closest entity along the player's look ray
 * that is still outside the held spear's effective attack range and not occluded by terrain.
 */
internal object SpearKillTargetFinder {

    private class Candidate(val entity: LivingEntity, val distanceToDamage: Double, val distSq: Double)

    fun findTarget(maxTargetDistance: Float): Pair<LivingEntity, Double>? {
        val eye = player.eyePosition
        val lookEnd = eye.add(player.lookAngle.scale(maxTargetDistance.toDouble()))
        val reach = player.getAttackRangeWith(player.useItem).reach()
        var best: Pair<LivingEntity, Double>? = null
        var bestDistSq = Double.MAX_VALUE

        for (entity in world.getEntitiesOfClass(
            LivingEntity::class.java,
            player.boundingBox.expandTowards(lookEnd.subtract(eye)).inflate(player.bbWidth / 2.0)
        ) { it !== player && it.isAlive && it.isWithinWorldBorder && it.boundingBox.clip(eye, lookEnd).isPresent }) {

            val candidate = scoreCandidate(entity, eye, lookEnd, reach, bestDistSq) ?: continue
            best = candidate.entity to candidate.distanceToDamage
            bestDistSq = candidate.distSq
        }
        return best
    }

    /**
     * The reach [net.minecraft.world.entity.projectile.ProjectileUtil.getHitEntitiesAlong] uses: the weapon's
     * maximum reach extended by the attacker's own movement along the look direction.
     */
    private fun AttackRange.reach(): Double =
        effectiveMaxRange(player) + player.deltaMovement.dot(player.lookAngle).coerceAtLeast(0.0)

    private fun scoreCandidate(
        entity: LivingEntity,
        eye: Vec3,
        lookEnd: Vec3,
        reach: Double,
        bestDistSq: Double
    ): Candidate? {
        val hitPosition = entity.boundingBox.clip(eye, lookEnd).orElse(null) ?: return null
        if (!hasLineOfSight(eye, hitPosition)) return null

        val distanceToTarget = hitPosition.distanceTo(eye)
        val distanceToDamage = (distanceToTarget - reach).coerceAtLeast(0.0)
        if (distanceToDamage <= 0.0) return null

        val distSq = distanceToDamage * distanceToDamage
        if (distSq >= bestDistSq) return null

        val hit = traceFromPlayer(range = distanceToDamage, block = ClipContext.Block.COLLIDER)
        if (hit.type != HitResult.Type.MISS && hit.location.distanceTo(eye) < distanceToDamage) return null

        return Candidate(entity, distanceToDamage, distSq)
    }
}
