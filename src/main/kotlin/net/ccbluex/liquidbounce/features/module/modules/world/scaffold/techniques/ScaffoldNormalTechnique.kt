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

import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldCeilingFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldHeadHitterFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldDownFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldEagleFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldStabilizeMovementFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldTellyFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldTellyFeature.Mode
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.targetfinding.AimMode
import net.ccbluex.liquidbounce.utils.block.targetfinding.AngleYawTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.DiagonalYawTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.EdgePointTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.NearestRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.PositionFactoryConfiguration
import net.ccbluex.liquidbounce.utils.block.targetfinding.RandomTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.ReverseYawTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.StabilizedRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.core.Vec3i
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.round
import kotlin.random.Random

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

    private var randomization = Random.nextDouble(-0.02, 0.02)

    override fun findPlacementTarget(
        predictedPos: Vec3,
        predictedPose: Pose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget? {
        // Prioritize the block that is closest to the line, if there was no line found, prioritize the nearest block
        val priorityComparator: Comparator<Vec3i> = if (optimalLine != null) {
            compareByDescending { vec -> optimalLine.squaredDistanceTo(Vec3.atCenterOf(vec)) }
        } else {
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE
        }

        val offsets = if (ModuleFreeze.running) {
            FULL_INVESTIGATION_OFFSETS
        } else if (ScaffoldDownFeature.shouldGoDown) {
            INVESTIGATE_DOWN_OFFSETS
        } else {
            NORMAL_INVESTIGATION_OFFSETS
        }

        // Face position factory for current config
        val facePositionFactory = getFacePositionFactoryForConfig(predictedPos, predictedPose, optimalLine)

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
            PlayerLocationOnPlacement(position = predictedPos, pose = predictedPose),
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

        if (requiresSight) {
            val target = target ?: return null
            val raycast = traceFromPlayer(rotation = target.rotation)

            if (raycast.type == HitResult.Type.BLOCK && raycast.blockPos == target.interactedBlockPos) {
                return target.rotation
            }
        }

        return super.getRotations(target)
    }

    override fun getCrosshairTarget(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? {
        val crosshairTarget = super.getCrosshairTarget(target ?: return null, rotation)

        // Prefer a visible hit result
        if (crosshairTarget != null && target.doesCrosshairTargetMatchRequirements(crosshairTarget)) {
            return crosshairTarget
        }

        // Allow a non-visible hit result
        if (ScaffoldDownFeature.shouldGoDown) {
            return target.blockHitResult
        }

        return null
    }

    private fun getFacePositionFactoryForConfig(predictedPos: Vec3, predictedPose: Pose, optimalLine: Line?):
        FaceTargetPositionFactory {
        val config = PositionFactoryConfiguration(
            predictedPos.add(0.0, player.getEyeHeight(predictedPose).toDouble(), 0.0),
            randomization,
        )

        return when (aimMode) {
            AimMode.CENTER -> CenterTargetPositionFactory
            AimMode.RANDOM -> RandomTargetPositionFactory
            AimMode.STABILIZED -> StabilizedRotationTargetPositionFactory(config, optimalLine)
            AimMode.NEAREST_ROTATION -> NearestRotationTargetPositionFactory(config)
            AimMode.REVERSE_YAW -> ReverseYawTargetPositionFactory(config)
            AimMode.DIAGONAL_YAW -> DiagonalYawTargetPositionFactory(config)
            AimMode.ANGLE_YAW -> AngleYawTargetPositionFactory(config)
            AimMode.EDGE_POINT -> EdgePointTargetPositionFactory(config)
        }
    }

    @Suppress("unused")
    private val afterJumpEvent = handler<PlayerAfterJumpEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
        randomization = Random.nextDouble(-0.01, 0.01)
    }

}
