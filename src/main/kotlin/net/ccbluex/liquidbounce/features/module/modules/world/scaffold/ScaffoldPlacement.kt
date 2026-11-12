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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold

import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.isValidCrosshairTarget
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldDownFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldTellyFeature
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.block.targetfinding.AimMode
import net.ccbluex.liquidbounce.utils.block.targetfinding.AngleYawTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPosOffsets
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.DiagonalYawTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.EdgePointTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FailedClick
import net.ccbluex.liquidbounce.utils.block.targetfinding.NearestRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.RandomTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.ReverseYawTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.StabilizedRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.placementTargetClickedBy
import net.ccbluex.liquidbounce.utils.block.targetfinding.verifyClick
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.round

/** Ticks ahead a kept look may still place the next block, see [ScaffoldPlacement.findTarget]. */
private const val HOLD_TICKS = 3

/**
 * Where scaffold puts the next block while it bridges, and how it looks at it.
 */
internal object ScaffoldPlacement {

    /** The look scaffold keeps this tick because it places the next block now or soon, see [findTarget]. */
    private var keptLook: Rotation? = null

    /** Whether the face position factory aims at the point nearest to where scaffold looks. */
    val aimsNearest
        get() = ModuleScaffold.aimMode == AimMode.STABILIZED || ModuleScaffold.aimMode == AimMode.NEAREST_ROTATION

    /**
     * The best placement for the block the player needs, unless the look scaffold has already makes one that does as
     * well: a bridging player keeps the mouse still for as long as each click still puts the next block down.
     */
    fun findTarget(
        predictedPos: Vec3,
        predictedPose: Pose,
        optimalLine: Line?,
        bestStack: ItemStack,
    ): BlockPlacementTarget? {
        keptLook = null
        val offsets = when {
            ModuleFreeze.running -> BlockPosOffsets.FULL.offsets
            ScaffoldDownFeature.shouldGoDown -> BlockPosOffsets.DOWN.offsets
            else -> BlockPosOffsets.NORMAL.offsets
        }
        val priorityComparator = if (optimalLine != null) {
            // Prioritize the block that is closest to the line
            BlockPlacementTargetFindingOptions.leastBlockDistanceToLine(optimalLine)
        } else {
            BlockPlacementTargetFindingOptions.leastBlockDistanceToPos(predictedPos)
        }

        val searchOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(offsets, priorityComparator),
            FaceHandlingOptions(
                facePositionFactory(optimalLine),
                considerFacingAwayFaces = ScaffoldDownFeature.shouldGoDown
            ),
            stackToPlaceWith = bestStack,
            PlayerLocationOnPlacement(
                position = predictedPos,
                pose = predictedPose,
                // Nearest to where scaffold aims rather than to a view still turning there
                rotation = ModuleScaffold.aim ?: RotationManager.serverRotation,
            ),
        )
        val needed = getTargetedPosition(predictedPos.toBlockPos())
        val best = findBestBlockPlacementTarget(needed, searchOptions) ?: return null
        if (!aimsNearest) {
            return best
        }

        // Without a click that puts the block itself down, any block next to it brings it in reach as well
        val positions = if (best.placedBlock == needed) {
            listOf(needed)
        } else {
            Direction.Plane.HORIZONTAL.map(needed::relative) + best.placedBlock
        }
        val view = RotationManager.currentRotation ?: player.rotation
        // Once the hand has settled near the aim, the view on the mouse grid is what clicks, and it may just miss
        val look = ModuleScaffold.aim?.takeIf { aim -> view.rotationDeltaLengthTo(aim) > RotationUtil.gcd } ?: view
        val now = placementTargetClickedBy(look, player.eyePosition, bestStack, positions)
            ?.takeIf { isValidCrosshairTarget(it.blockHitResult) }
        keptLook = look.takeIf { now != null || placesSoon(look, bestStack, positions) }
        return now ?: best
    }

    fun rotation(target: BlockPlacementTarget?): Rotation? {
        if (ScaffoldTellyFeature.enabled && ScaffoldTellyFeature.doNotAim) {
            return when (ScaffoldTellyFeature.resetMode) {
                ScaffoldTellyFeature.Mode.REVERSE -> Rotation(
                    round(player.rotation.yaw / 45) * 45,
                    if (player.xRot < 45f) 45f else player.xRot
                )

                ScaffoldTellyFeature.Mode.RESET -> null
            }
        }

        keptLook?.let { look ->
            if (target != null) {
                return look
            }
        }

        if (ModuleScaffold.requiresSight) {
            val target = target ?: return null
            val raycast = traceFromPlayer(rotation = target.rotation)

            if (raycast.type == HitResult.Type.BLOCK && raycast.blockPos == target.interactedBlockPos) {
                return target.rotation
            }
        }

        return target?.rotation
    }

    // Going down allows a non-visible hit result
    fun click(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? = target?.verifyClick(
        rotation,
        onFailure = if (ScaffoldDownFeature.shouldGoDown) FailedClick.PLANNED_HIT else FailedClick.NOTHING,
    )

    /**
     * Whether [rotation] puts [stack] at one of [positions] from where the player is in one of the next [HOLD_TICKS]
     * ticks while still on the ground. If that's at the edge, SafeWalk holds the player there until it does.
     */
    private fun placesSoon(rotation: Rotation, stack: ItemStack, positions: List<BlockPos>): Boolean {
        val simulation = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(DirectionalInput(player.input))
        )
        repeat(HOLD_TICKS) {
            simulation.tick()
            if (!simulation.onGround()) {
                return false
            }
            val eyes = simulation.pos.add(0.0, player.eyeHeight.toDouble(), 0.0)
            if (placementTargetClickedBy(rotation, eyes, stack, positions) != null) {
                return true
            }
        }
        return false
    }

    private fun facePositionFactory(optimalLine: Line?): FaceTargetPositionFactory = when (ModuleScaffold.aimMode) {
        AimMode.CENTER -> CenterTargetPositionFactory
        AimMode.RANDOM -> RandomTargetPositionFactory
        AimMode.STABILIZED -> StabilizedRotationTargetPositionFactory(optimalLine)
        AimMode.NEAREST_ROTATION -> NearestRotationTargetPositionFactory
        AimMode.REVERSE_YAW -> ReverseYawTargetPositionFactory
        AimMode.DIAGONAL_YAW -> DiagonalYawTargetPositionFactory
        AimMode.ANGLE_YAW -> AngleYawTargetPositionFactory
        AimMode.EDGE_POINT -> EdgePointTargetPositionFactory
    }

}
