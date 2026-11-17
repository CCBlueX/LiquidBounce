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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.features.module.modules.world.ModuleClutch
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.NORMAL
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.rotationTiming
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.Surface
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.outlineShape
import net.ccbluex.liquidbounce.utils.block.stateOrEmpty
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPosOffsets
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.verifyClick
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.floor

/**
 * Builds under the fall [ModuleClutch] predicts. Not a setting: scaffold builds this way for as long as the fall
 * lasts.
 */
internal object ScaffoldClutchFeature {

    /**
     * An anticheat only takes a click for certain if the server had its rotation the tick before. The on tick
     * timings send the rotation along with the click instead.
     */
    private val aimsAhead
        get() = rotationTiming == NORMAL

    fun findPlacementTarget(bestStack: ItemStack): BlockPlacementTarget? {
        val fall = ModuleClutch.fall ?: return null
        val now = target(player.position(), fall, bestStack)
        if (!aimsAhead) {
            return now
        }

        // What the rotation the server has points at already is clicked this tick. Anything else can only be aimed
        // at, for the next tick to click from where the player is by then
        now?.takeIf { it.allowedClick(RotationManager.serverRotation) != null }?.let { return it }

        // An aim that takes a while to turn starts before the click is possible, as long as it gets there in time
        for (tick in fall.indices) {
            val ahead = target(fall[tick], fall.subList(tick + 1, fall.size), bestStack) ?: continue
            return ahead.takeIf { tick == 0 || tick <= ScaffoldRotationValueGroup.calculateTicks(it.rotation) }
        }
        return null
    }

    /**
     * The block to place from [from] to get under [fall], the positions of the ticks after.
     */
    private fun target(from: Vec3, fall: List<Vec3>, stack: ItemStack): BlockPlacementTarget? {
        val clickFrom = PlayerLocationOnPlacement(position = from)
        val range = player.blockInteractionRange()
        var feetY = from.y

        for (position in fall) {
            // A block catches the player where its feet come down through the top of a block layer. At the click
            // the feet have to be above that layer already
            val top = floor(feetY)
            feetY = position.y
            if (position.y >= top || top > from.y) {
                continue
            }

            if (clickFrom.eyePos.y - top > range) {
                break
            }

            // The block goes under the player, not under where it comes down: a knock carries it blocks away, and
            // only a row that keeps up with it gets there. With a block under it the row has to wait for it
            val below = BlockPos.containing(from.x, top - 1, from.z)
            if (below.canStandOn()) {
                return null
            }

            val searchOptions = BlockPlacementTargetFindingOptions(
                BlockOffsetOptions(
                    // A block right on top of ground gains nothing over that ground
                    BlockPosOffsets.FULL.offsets.filterNot { below.offset(it).below().canStandOn() },
                    BlockPlacementTargetFindingOptions.leastBlockDistanceToPos(from.copy(y = top - 0.5)),
                ),
                FaceHandlingOptions(CenterTargetPositionFactory),
                stackToPlaceWith = stack,
                clickFrom,
                range,
            )

            // Without a block in reach on this layer, one further down may still have one
            return findBestBlockPlacementTarget(below, searchOptions) ?: continue
        }

        return null
    }

    fun click(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? =
        if (aimsAhead) target?.allowedClick(RotationManager.serverRotation) else target?.verifyClick(rotation)

    /**
     * What a look has to point at to place [target]: the whole block it clicks, see [allowedClick]. As a target of
     * that size, a hand flicks onto it no slower than onto any other.
     */
    fun surface(target: BlockPlacementTarget): Surface? = target.interactedBlockPos
        .takeIf { aimsAhead && !it.outlineShape.isEmpty }
        ?.let { Surface.of(it, throughWalls = true) }

    /**
     * The click [rotation] allows. It only has to meet the clicked block, not the face to click: no anticheat can
     * tell which face a rotation met a tick ago, and the one that placed the last block of a row still meets it.
     */
    private fun BlockPlacementTarget.allowedClick(rotation: Rotation): BlockHitResult? {
        verifyClick(rotation)?.let { return it }

        val facesPlayer = (player.eyePosition - interactionPoint).dot(direction.unitVec3) > 0.0
        val state = interactedBlockPos.stateOrEmpty
        val pointedAt = raytraceBlock(player.blockInteractionRange(), rotation, interactedBlockPos, state) != null

        return blockHitResult.takeIf { facesPlayer && pointedAt }
    }

}
