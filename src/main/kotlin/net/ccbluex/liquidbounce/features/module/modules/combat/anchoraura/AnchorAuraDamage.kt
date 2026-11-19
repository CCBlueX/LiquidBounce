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
package net.ccbluex.liquidbounce.features.module.modules.combat.anchoraura

import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.getBoundingBoxAt
import net.ccbluex.liquidbounce.utils.entity.getDamageFromExplosion
import net.ccbluex.liquidbounce.utils.math.center
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/** Anchor-specific blast calculation: power 5, a ten-block range, and bad-respawn-point damage semantics. */
internal object AnchorAuraDamage : MinecraftShortcuts {

    private const val POWER = 5f
    private const val EXPLOSION_RANGE = 10f
    private const val DAMAGE_DISTANCE = 100f
    private const val TERRAIN_BLAST_RESISTANCE = 9f

    private data class SplashDamage(val self: Float, val friends: Float)

    private val splashCache = HashMap<Long, SplashDamage>()

    /** Clears values that are only valid for the current world tick. */
    fun clearCache() {
        splashCache.clear()
    }

    fun calculate(
        target: LivingEntity,
        anchorPos: BlockPos,
        terrain: Boolean,
        selfPredictionTicks: Int,
        targetPredictionTicks: Int,
        useCache: Boolean = true,
    ): AnchorDamageInput {
        val center = anchorPos.center
        val enemyDamage = enemyDamage(target, anchorPos, center, terrain, targetPredictionTicks)
        val splash = if (useCache) {
            splashCache.computeIfAbsent(anchorPos.asLong()) {
                splashDamage(anchorPos, center, terrain, selfPredictionTicks)
            }
        } else {
            splashDamage(anchorPos, center, terrain, selfPredictionTicks)
        }

        return AnchorDamageInput(
            enemyDamage = enemyDamage,
            selfDamage = splash.self,
            friendDamage = splash.friends,
            enemyHealth = target.getActualHealth() + target.absorptionAmount,
            selfHealth = player.health + player.absorptionAmount,
        )
    }

    /** Both current and predicted positions must meet enemy thresholds; use the lower damage for scoring. */
    private fun enemyDamage(
        target: LivingEntity,
        anchorPos: BlockPos,
        center: Vec3,
        terrain: Boolean,
        predictionTicks: Int,
    ): Float {
        val current = damage(target, anchorPos, center, terrain)
        if (predictionTicks <= 0 || target !is Player) {
            return current
        }

        val predictedPos = PlayerSimulationCache.getSimulationForOtherPlayers(target)
            .getSnapshotAt(predictionTicks).pos
        val predicted = damage(
            target,
            anchorPos,
            center,
            terrain,
            target.getBoundingBoxAt(predictedPos),
        )
        return minOf(current, predicted)
    }

    /** Self checks use the worse position so prediction can only make safety stricter. */
    private fun selfDamage(
        anchorPos: BlockPos,
        center: Vec3,
        terrain: Boolean,
        predictionTicks: Int,
    ): Float {
        val current = damage(player, anchorPos, center, terrain)
        if (predictionTicks <= 0) {
            return current
        }

        val predictedPos = PlayerSimulationCache.getSimulationForLocalPlayer()
            .getSnapshotAt(predictionTicks).pos
        val predicted = damage(
            player,
            anchorPos,
            center,
            terrain,
            player.getBoundingBoxAt(predictedPos),
        )
        return maxOf(current, predicted)
    }

    private fun splashDamage(
        anchorPos: BlockPos,
        center: Vec3,
        terrain: Boolean,
        selfPredictionTicks: Int,
    ): SplashDamage {
        var friendDamage = 0f
        val nearbyFriends = world.getEntitiesBoxInRange(center, EXPLOSION_RANGE.toDouble()) { entity ->
            entity is LivingEntity && FriendManager.isFriend(entity)
        }

        for (friend in nearbyFriends) {
            val living = friend as LivingEntity
            friendDamage = maxOf(friendDamage, damage(living, anchorPos, center, terrain))
        }

        return SplashDamage(selfDamage(anchorPos, center, terrain, selfPredictionTicks), friendDamage)
    }

    private fun damage(
        entity: LivingEntity,
        anchorPos: BlockPos,
        center: Vec3,
        terrain: Boolean,
        boundingBox: AABB? = null,
    ): Float = entity.getDamageFromExplosion(
        pos = center,
        power = POWER,
        explosionRange = EXPLOSION_RANGE,
        damageDistance = DAMAGE_DISTANCE,
        // Vanilla removes the respawn anchor before applying the explosion.
        exclude = listOf(anchorPos),
        maxBlastResistance = if (terrain) TERRAIN_BLAST_RESISTANCE else null,
        entityBoundingBox = boundingBox,
        damageSource = entity.damageSources().badRespawnPointExplosion(center),
    )

}
