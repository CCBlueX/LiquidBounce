/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldCeilingFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldHeadHitterFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldDownFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldEagleFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldStabilizeMovementFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldTellyFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldTellyFeature.Mode
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.entity.EntityPose
import net.minecraft.item.ItemStack
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
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

    private val INVESTIGATE_DOWN_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(intArrayOf(0, -1, 1, -2, 2))
    internal val NORMAL_INVESTIGATION_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(intArrayOf(0, -1, 1))

    private var randomization = Random.nextDouble(-0.02, 0.02)

    override fun findPlacementTarget(
        predictedPos: Vec3d,
        predictedPose: EntityPose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget? {
        // Prioritize the block that is closest to the line, if there was no line found, prioritize the nearest block
        val priorityComparator: Comparator<Vec3i> = if (optimalLine != null) {
            compareByDescending { vec -> optimalLine.squaredDistanceTo(Vec3d.of(vec).add(0.5, 0.5, 0.5)) }
        } else {
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE
        }

        // Face position factory for current config
        val facePositionFactory = getFacePositionFactoryForConfig(predictedPos, predictedPose, optimalLine)

        val searchOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(
                if (ScaffoldDownFeature.shouldGoDown) INVESTIGATE_DOWN_OFFSETS else NORMAL_INVESTIGATION_OFFSETS,
                priorityComparator,
            ),
            FaceHandlingOptions(facePositionFactory),
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
                    if (player.pitch < 45f) 45f else player.pitch
                )

                Mode.RESET -> null
            }
        }

        if (requiresSight) {
            val target = target ?: return null
            val raycast = raycast(rotation = target.rotation) ?: return null

            if (raycast.type == HitResult.Type.BLOCK && raycast.blockPos == target.interactedBlockPos) {
                return target.rotation
            }
        }

        return super.getRotations(target)
    }

    fun getFacePositionFactoryForConfig(predictedPos: Vec3d, predictedPose: EntityPose, optimalLine: Line?):
        FaceTargetPositionFactory {
        val config = PositionFactoryConfiguration(
            predictedPos.add(0.0, player.getEyeHeight(predictedPose).toDouble(), 0.0),
            randomization,
        )

        return when (aimMode) {
            AimMode.CENTER -> CenterTargetPositionFactory
            AimMode.RANDOM -> RandomTargetPositionFactory(config)
            AimMode.STABILIZED -> StabilizedRotationTargetPositionFactory(config, optimalLine)
            AimMode.NEAREST_ROTATION -> NearestRotationTargetPositionFactory(config)
            AimMode.REVERSE_YAW -> ReverseYawTargetPositionFactory(config)
            AimMode.DIAGONAL_YAW -> DiagonalYawTargetPositionFactory(config)
            AimMode.ANGLE_YAW -> AngleYawTargetPositionFactory(config)
            AimMode.EDGE_POINT -> EdgePointTargetPositionFactory(config)
        }
    }

    @Suppress("unused")
    val afterJumpEvent = handler<PlayerAfterJumpEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
        randomization = Random.nextDouble(-0.01, 0.01)
    }

    private fun commonOffsetToInvestigate(xzOffsets: IntArray): List<Vec3i> = buildList(xzOffsets.size.sq() * 2) {
        for (x in xzOffsets) {
            for (z in xzOffsets) {
                add(Vec3i(x, 0, z))
                add(Vec3i(x, -1, z))
            }
        }
    }

}
