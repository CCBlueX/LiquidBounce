/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.canSeeUpperBlockSide
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.aiming.raytraceUpperBlockSide
import net.ccbluex.liquidbounce.utils.block.forEachCollidingBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRadius
import net.ccbluex.liquidbounce.utils.client.clickBlockWithSlot
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.combat.getEntitiesInCuboid
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.Blocks
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.item.Items
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object SubmoduleCrystalPlacer {
    private var currentTarget: BlockPos? = null

    fun tick() {
        val crystalSlot = findHotbarSlot(Items.END_CRYSTAL) ?: return

        updateTarget()

        val target = currentTarget ?: return

        val rotation =
            raytraceUpperBlockSide(
                player.eyePos,
                ModuleCrystalAura.PlaceOptions.range.toDouble(),
                wallsRange = 0.0,
                target,
            ) ?: return

        RotationManager.aimAt(
            rotation.rotation,
            configurable = ModuleCrystalAura.rotations,
            priority = Priority.IMPORTANT_FOR_USER_SAFETY,
            provider = ModuleCrystalAura
        )

        val serverRotation = RotationManager.serverRotation

        val rayTraceResult =
            raytraceBlock(
                ModuleCrystalAura.PlaceOptions.range.toDouble(),
                serverRotation,
                target,
                target.getState() ?: return,
            )

        if (rayTraceResult?.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != target) {
            return
        }

        clickBlockWithSlot(player, rayTraceResult, crystalSlot)
    }

    private fun updateTarget() {
        // Reset current target
        currentTarget = null

        val playerEyePos = player.eyePos
        val playerPos = Vec3d.of(player.blockPos)
        val range = ModuleCrystalAura.PlaceOptions.range.toDouble()

        val entitiesInRange = world.getEntitiesInCuboid(playerPos, range + 6.0)

        // No targets to consider? Why bother?
        if (entitiesInRange.isEmpty()) {
            return
        }

        if (entitiesInRange.any { it is EndCrystalEntity }) {
            return
        }

        // The bounding box where entities are in that might body block a crystal placement
        val bodyBlockingBoundingBox =
            Box(
                playerPos.subtract(range + 0.1, range + 0.1, range + 0.1),
                playerPos.add(range + 0.1, range + 0.1, range + 0.1),
            )

        val blockedPositions = HashSet<BlockPos>()

        // Disallow all positions where entities body-block them
        for (entity in entitiesInRange) {
            if (!entity.boundingBox.intersects(bodyBlockingBoundingBox)) {
                continue
            }

            entity.boundingBox.forEachCollidingBlock { x, y, z ->
                blockedPositions.add(BlockPos(x, y - 1, z))
            }
        }

        // Search for blocks that are either obsidian or bedrock,
        // not disallowed and which do not have other blocks on top
        val possibleTargets =
            searchBlocksInRadius(ModuleCrystalAura.PlaceOptions.range) { pos, state ->
                return@searchBlocksInRadius (state.block == Blocks.OBSIDIAN || state.block == Blocks.BEDROCK) &&
                        pos !in blockedPositions &&
                        pos.up().getState()?.isAir == true &&
                        canSeeUpperBlockSide(playerEyePos, pos, range, 0.0)
            }

        val bestTarget =
            possibleTargets
                .map {
                    val damageSourceLoc = Vec3d.of(it.first).add(0.5, 1.0, 0.5)

                    Pair(it, ModuleCrystalAura.approximateExplosionDamage(world, damageSourceLoc))
                }
                .maxByOrNull { it.second }

        // Is the target good enough?
        if (bestTarget == null || bestTarget.second < ModuleCrystalAura.PlaceOptions.minEfficiency) {
            return
        }

        currentTarget = bestTarget.first.first
    }
}
