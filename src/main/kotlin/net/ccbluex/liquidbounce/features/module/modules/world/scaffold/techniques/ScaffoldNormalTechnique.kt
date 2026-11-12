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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques

import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldCeilingFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldHeadHitterFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldDownFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldEagleFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldStabilizeMovementFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldTellyFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldTellyFeature.Mode
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
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
import net.ccbluex.liquidbounce.utils.block.targetfinding.verifyClick
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPoint
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.round

/** Ticks ahead a held aim may still line up the next block, see [ScaffoldNormalTechnique.getRotations]. */
private const val HOLD_TICKS = 3

/**
 * Normal technique, which is basically just normal scaffold.
 */
object ScaffoldNormalTechnique : ScaffoldTechnique("Normal") {

    private val aimMode by enumChoice("RotationMode", AimMode.STABILIZED)
    private val requiresSight by boolean("RequiresSight", false)

    init {
        tree(ScaffoldEagleFeature)
        tree(ScaffoldTellyFeature)
        tree(ScaffoldDownFeature)
        tree(ScaffoldStabilizeMovementFeature)
        tree(ScaffoldCeilingFeature)
        tree(ScaffoldHeadHitterFeature)
    }

    override fun findPlacementTarget(
        predictedPos: Vec3,
        predictedPose: Pose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget? {
        val priorityComparator = priorityComparator(predictedPos, optimalLine)

        val offsets = if (ModuleFreeze.running) {
            BlockPosOffsets.FULL.offsets
        } else if (ScaffoldDownFeature.shouldGoDown) {
            BlockPosOffsets.DOWN.offsets
        } else {
            BlockPosOffsets.NORMAL.offsets
        }

        // Face position factory for current config
        val facePositionFactory = getFacePositionFactory(optimalLine)

        val searchOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(
                offsets,
                priorityComparator,
            ),
            FaceHandlingOptions(
                facePositionFactory,
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
        return findBestBlockPlacementTarget(getTargetedPosition(predictedPos.toBlockPos()), searchOptions)
    }

    override fun getRotations(target: BlockPlacementTarget?): Rotation? {
        if (ScaffoldTellyFeature.enabled && ScaffoldTellyFeature.doNotAim) {
            return when (ScaffoldTellyFeature.resetMode) {
                Mode.REVERSE -> Rotation(
                    round(player.rotation.yaw / 45) * 45,
                    if (player.xRot < 45f) 45f else player.xRot
                )

                Mode.RESET -> null
            }
        }

        // A bridging player keeps the mouse still while the next block comes under it
        val held = ModuleScaffold.aim
        if (target != null && held != null && aimsNearest && target.linesUp(held)) {
            return held
        }

        if (requiresSight) {
            val target = target ?: return null
            val raycast = traceFromPlayer(rotation = target.rotation)

            if (raycast.type == HitResult.Type.BLOCK && raycast.blockPos == target.interactedBlockPos) {
                return target.rotation
            }
        }

        return super.getRotations(target)
    }

    override fun getCrosshairTarget(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? =
        target?.verifyClick(
            rotation,
            // Going down allows a non-visible hit result
            onFailure = if (ScaffoldDownFeature.shouldGoDown) FailedClick.PLANNED_HIT else FailedClick.NOTHING,
        )

    private val aimsNearest
        get() = aimMode == AimMode.STABILIZED || aimMode == AimMode.NEAREST_ROTATION

    /**
     * Whether [rotation] clicks this target now, or from where the player is in one of the next [HOLD_TICKS] ticks
     * while still on the ground.
     */
    private fun BlockPlacementTarget.linesUp(rotation: Rotation): Boolean {
        if (clicks(rotation, player.eyePosition)) {
            return true
        }

        val simulation = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(DirectionalInput(player.input))
        )
        repeat(HOLD_TICKS) {
            simulation.tick()
            if (!simulation.onGround()) {
                return false
            }
            if (clicks(rotation, simulation.pos.add(0.0, player.eyeHeight.toDouble(), 0.0))) {
                return true
            }
        }
        return false
    }

    private fun BlockPlacementTarget.clicks(rotation: Rotation, eyes: Vec3): Boolean {
        val hit = traceFromPoint(start = eyes, direction = rotation.directionVector)
        return doesCrosshairTargetMatchRequirements(hit) && ModuleScaffold.isValidCrosshairTarget(hit)
    }

    private fun getFacePositionFactory(optimalLine: Line?): FaceTargetPositionFactory = when (aimMode) {
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
