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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationManager.canSeeUpperBlockSide
import net.ccbluex.liquidbounce.utils.aiming.RotationManager.raytraceUpperBlockSide
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.forEachCollidingBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRadius
import net.ccbluex.liquidbounce.utils.client.clickBlockWithSlot
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.block.Blocks
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.item.Items
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.explosion.Explosion
import kotlin.math.sqrt

object ModuleCrystalAura : Module("CrystalAura", Category.WORLD) {

    private val swing by boolean("Swing", true)

    private object PlaceOptions : ToggleableConfigurable(this, "Place", true) {
        val range by float("Range", 4.5F, 1.0F..5.0F)
        val minEfficiency by float("MinEfficiency", 0.1F, 0.0F..5.0F)
    }

    // Rotation
    private val rotations = RotationsConfigurable()

    private var currentTarget: BlockPos? = null

    init {
        tree(PlaceOptions)
    }

    val networkTickHandler = repeatable {
        val crystalSlot = findHotbarSlot(Items.END_CRYSTAL) ?: return@repeatable

        updateTarget()

        val target = currentTarget ?: return@repeatable

        val rotation = raytraceUpperBlockSide(player.eyePos, PlaceOptions.range.toDouble(), 0.0, target) ?: return@repeatable

        RotationManager.aimAt(rotation.rotation, configurable = rotations)

        val serverRotation = RotationManager.serverRotation ?: return@repeatable

        val rayTraceResult = raytraceBlock(
            PlaceOptions.range.toDouble(),
            serverRotation,
            target,
            target.getState() ?: return@repeatable
        )

        if (rayTraceResult?.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != target) {
            return@repeatable
        }

        clickBlockWithSlot(player, rayTraceResult, crystalSlot)
    }

    private fun updateTarget() {
        // Reset current target
        currentTarget = null

        val player = player
        val world = world

        val playerEyePos = player.eyePos
        val playerPos = Vec3d.of(player.blockPos)
        val range = PlaceOptions.range.toDouble()

        // The bounding box where every considered target is in
        val targetBoundingBox = Box(
            playerPos.subtract(range + 6.0, range + 6.0, range + 6.0),
            playerPos.add(range + 6.0, range + 6.0, range + 6.0)
        )

        val entitiesInRange = world.getOtherEntities(null, targetBoundingBox)

        // No targets to consider? Why bother?
        if (entitiesInRange.isEmpty())
            return

        if (entitiesInRange.any { it is EndCrystalEntity })
            return

        // The bounding box where entities are in that might body block a crystal placement
        val bodyBlockingBoundingBox = Box(
            playerPos.subtract(range + 0.1, range + 0.1, range + 0.1),
            playerPos.add(range + 0.1, range + 0.1, range + 0.1)
        )

        val blockedPositions = HashSet<BlockPos>()

        // Disallow all positions where entities body-block them
        for (entity in entitiesInRange) {
            if (!entity.boundingBox.intersects(bodyBlockingBoundingBox))
                continue

            entity.boundingBox.forEachCollidingBlock { x, y, z ->
                blockedPositions.add(BlockPos(x, y - 1, z))
            }
        }

        // Search for blocks that are either obsidian or bedrock, not disallowed and which do not have other blocks on top
        val possibleTargets = searchBlocksInRadius(PlaceOptions.range) { pos, state ->
            return@searchBlocksInRadius (state.block == Blocks.OBSIDIAN || state.block == Blocks.BEDROCK)
                    && pos !in blockedPositions
                    && pos.up().getState()?.isAir == true
                    && canSeeUpperBlockSide(playerEyePos, pos, range, 0.0)
        }

        val bestTarget = possibleTargets
            .map { Pair(it, approximateExplosionDamage(world, Vec3d.of(it.first).add(0.5, 1.0, 0.5))) }
            .maxByOrNull { it.second }

        // Is the target good enough?
        if (bestTarget == null || bestTarget.second < PlaceOptions.minEfficiency)
            return

        currentTarget = bestTarget.first.first
    }

    /**
     * Approximates how favorable an explosion of a crystal at [pos] in a given [world] would be
     */
    private fun approximateExplosionDamage(
        world: ClientWorld,
        pos: Vec3d
    ): Double {
        val maxDistance = 6.0
        val maxDistanceSquared = maxDistance * maxDistance

        val possibleVictims = world.getOtherEntities(
            null,
            Box(
                pos.subtract(maxDistance, maxDistance, maxDistance),
                pos.add(maxDistance, maxDistance, maxDistance)
            ),
            EntityPredicates.EXCEPT_SPECTATOR.and {
                it.shouldBeAttacked() && it.squaredDistanceTo(pos) <= maxDistanceSquared && it.boundingBox.maxY > pos.y
            }
        )

        var totalDamage = 0.0

        for (possibleVictim in possibleVictims) {
            val pre = Explosion.getExposure(pos, possibleVictim) * (1.0 - sqrt(
                possibleVictim.squaredDistanceTo(pos)
            ) / 12.0)

            totalDamage += pre * pre
        }

        return totalDamage
    }

}
