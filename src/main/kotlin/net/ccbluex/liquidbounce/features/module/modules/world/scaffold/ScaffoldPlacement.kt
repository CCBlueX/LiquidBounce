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

import net.ccbluex.agent.simulation.SimulatedInput
import net.ccbluex.agent.simulation.SimulatedPlayer
import net.ccbluex.agent.v26_3.toAgent
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.isValidCrosshairTarget
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging.ScaffoldBridging
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging.ScaffoldBridgingNormal
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging.ScaffoldBridgingTelly
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldDownFeature
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.block.outlineShape
import net.ccbluex.liquidbounce.utils.block.targetfinding.AimMode
import net.ccbluex.liquidbounce.utils.block.targetfinding.AngleYawTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPosOffsets
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.DiagonalYawTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.DirectionTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.EdgePointTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.NearestRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.RandomTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.ReverseYawTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.StabilizedRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.Visibility
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.placementTargetClickedBy
import net.ccbluex.liquidbounce.utils.block.targetfinding.surface
import net.ccbluex.liquidbounce.utils.block.targetfinding.verifyClick
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.fromClientPlayer
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
import kotlin.math.hypot
import kotlin.math.round

/** Blocks a predicted click has to land inside the edges of its face, see [ScaffoldPlacement.placesInTime]. */
private const val FACE_MARGIN = 0.03

/** Ticks ahead a kept look may still place the next block, see [ScaffoldPlacement.findTarget]. */
private const val HOLD_TICKS = 3

/** Ticks a held look may leave the player standing at an edge before scaffold aims at the face instead. */
private const val STALL_TICKS = 3

/**
 * Where scaffold puts the next block while it bridges, and where on it [ModuleScaffold.bridging] recommends to look.
 */
internal object ScaffoldPlacement {

    /** The look scaffold keeps this tick because it places the next block now or soon, see [findTarget]. */
    private var keptLook: Rotation? = null

    /** The look the bridging holds this tick, see [ScaffoldBridging.look]. */
    var heldLook: Rotation? = null
        private set

    /** Ticks the player has stood without the kept held look placing anything. */
    private var stalledTicks = 0

    /** The stack [findTarget] last looked for a placement with. */
    private var stack = ItemStack.EMPTY

    private val bridging
        get() = ModuleScaffold.bridging.activeMode

    /** How to aim at the face when the bridging holds no look: Telly aims the way Normal does by default. */
    private val aimMode
        get() = if (bridging === ScaffoldBridgingNormal) ScaffoldBridgingNormal.aimMode else AimMode.STABILIZED

    /** Whether the look may meet the clicked face through blocks, and the face may be turned away from the eyes. */
    private val throughWalls
        get() = ModuleScaffold.visibility == Visibility.NOT_VISIBLE || ScaffoldDownFeature.shouldGoDown

    fun reset() {
        keptLook = null
        heldLook = null
        stalledTicks = 0
        ScaffoldBridging.reset()
    }

    /**
     * The best placement for the block the player needs, unless the look [ModuleScaffold.bridging] recommends to hold
     * makes one that does as well: a bridging player keeps the mouse still for as long as each click still puts the
     * next block down.
     */
    fun findTarget(
        predictedPos: Vec3,
        predictedPose: Pose,
        optimalLine: Line?,
        bestStack: ItemStack,
    ): BlockPlacementTarget? {
        keptLook = null
        stack = bestStack
        heldLook = bridging.look(predictedPos)
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
            FaceHandlingOptions(facePositionFactory(optimalLine), considerFacingAwayFaces = throughWalls),
            stackToPlaceWith = bestStack,
            PlayerLocationOnPlacement(
                position = predictedPos,
                pose = predictedPose,
                // Nearest to where scaffold aims rather than to a view still turning there
                rotation = ModuleScaffold.aim ?: RotationManager.serverRotation,
            ),
        )
        val needed = getTargetedPosition(predictedPos.toBlockPos())
        val best = findBestBlockPlacementTarget(needed, searchOptions)
        if (best == null) {
            // Nothing to place: a held look stays rather than going back to the camera
            keptLook = heldLook
            return null
        }
        val look = recommendedLook() ?: return best

        // Without a click that puts the block itself down, any block next to it brings it in reach as well
        val positions = if (best.placedBlock == needed) {
            listOf(needed)
        } else {
            Direction.Plane.HORIZONTAL.map(needed::relative) + best.placedBlock
        }
        val now = placementTargetClickedBy(look, player.eyePosition, bestStack, positions)
            ?.takeIf { isValidCrosshairTarget(it.blockHitResult) }
        // The velocity keeps the input's push while an edge holds the player back, the position does not move
        val moved = hypot(player.x - player.xo, player.z - player.zo)
        // Below 0.01 blocks a tick the player counts as standing
        stalledTicks = if (now == null && player.onGround() && moved < 0.01) {
            stalledTicks + 1
        } else {
            0
        }
        keptLook = look.takeIf { now != null || keepsLook(look, bestStack, positions) }
        return now ?: best
    }

    /**
     * The look [ModuleScaffold.bridging] would rather hold than turn to a point on the face: the look it holds, or
     * where scaffold aims already for the modes aiming nearest to it. Modes aiming at a point have none.
     */
    private fun recommendedLook(): Rotation? = when {
        bridging.holdsLook -> heldLook
        aimMode == AimMode.STABILIZED || aimMode == AimMode.NEAREST_ROTATION -> {
            val view = RotationManager.currentRotation ?: player.rotation
            // Once the hand has settled near the aim, the view on the mouse grid is what clicks, and it may just miss
            ModuleScaffold.aim?.takeIf { aim -> view.rotationDeltaLengthTo(aim) > RotationUtil.gcd } ?: view
        }
        else -> null
    }

    /**
     * Whether [look] stays although it does not place right now. A held look meets the face at the latest where
     * SafeWalk stops the player, so it stays on the ground until the player stood there for [STALL_TICKS]; in the air
     * it only reaches the row the player left. Any other look stays only while it places one of the next ticks.
     */
    private fun keepsLook(look: Rotation, stack: ItemStack, positions: List<BlockPos>) =
        if (bridging.holdsLook) {
            player.onGround() && stalledTicks < STALL_TICKS
        } else {
            placesSoon(look, stack, positions)
        }

    /**
     * Whether [rotation] puts [stack] at one of [positions] from where the player is in one of the next [ticks] ticks
     * while still on the ground. If that's at the edge, SafeWalk holds the player there until it does. [wellInside]
     * only counts clicks that land clear of the face's edges.
     */
    private fun placesSoon(
        rotation: Rotation,
        stack: ItemStack,
        positions: List<BlockPos>,
        input: SimulatedInput = SimulatedInput.fromClientPlayer(DirectionalInput(player.input)),
        ticks: Int = HOLD_TICKS,
        wellInside: Boolean = false,
    ): Boolean {
        val simulation = SimulatedPlayer.fromClientPlayer(input)
        repeat(ticks) {
            simulation.tick()
            if (!simulation.onGround()) {
                return false
            }
            val eyes = simulation.position().add(0.0, player.eyeHeight.toDouble(), 0.0)
            val placement = placementTargetClickedBy(rotation, eyes, stack, positions)
            if (placement != null && (!wellInside || placement.isWellInsideFace())) {
                return true
            }
        }
        return false
    }

    /**
     * Whether [rotation] puts [target] down before the player, walking on without sneaking, leaves the ground. Only a
     * view that reached the aim stays the same over those ticks. A view resting near the held look barely moves,
     * so its next tick counts as well.
     */
    fun placesInTime(target: BlockPlacementTarget, rotation: Rotation): Boolean {
        val held = ModuleScaffold.aim?.let { aim -> rotation.rotationDeltaLengthTo(aim) <= RotationUtil.gcd } == true
        val ticks = when {
            held -> HOLD_TICKS
            bridging.holdsLook -> 1
            else -> return false
        }
        // Built by hand: asking for SafeWalk here would ask scaffold again
        val input = SimulatedInput(
            DirectionalInput(player.input).toInput(
                jump = player.input.keyPresses.jump,
                // The held key alone does not sprint: walking against the view, movement correction keeps it off
                sprint = player.isSprinting,
            ).toAgent()
        )
        return placesSoon(rotation, stack, listOf(target.placedBlock), input, ticks, wellInside = true)
    }

    fun rotation(target: BlockPlacementTarget?): Rotation? {
        if (ScaffoldBridgingTelly.running && ScaffoldBridgingTelly.doNotAim) {
            return when (ScaffoldBridgingTelly.resetMode) {
                ScaffoldBridgingTelly.ResetMode.REVERSE -> Rotation(
                    round(player.rotation.yaw / 45) * 45,
                    if (player.xRot < 45f) 45f else player.xRot
                )

                ScaffoldBridgingTelly.ResetMode.RESET -> null
            }
        }

        keptLook?.let { return it }
        val target = target ?: return null
        if (ModuleScaffold.visibility == Visibility.VISIBLE_AND_RAYTRACED) {
            val raycast = traceFromPlayer(rotation = target.rotation)
            if (raycast.type != HitResult.Type.BLOCK || raycast.blockPos != target.interactedBlockPos) {
                return null
            }
        }
        return target.rotation
    }

    /**
     * Whether scaffold keeps the look the bridging holds, which is where to look exactly. A kept aim of the other modes
     * is only somewhere on the face, and the hand may rest anywhere it still meets it.
     */
    val holdsBridgingLook
        get() = keptLook != null && bridging.holdsLook

    /** What a look has to meet to place [target]: the face it clicks. */
    fun surface(target: BlockPlacementTarget) = target.surface(throughWalls)

    /**
     * The hit a click with [rotation] places [target] with. Through blocks the ray never gets there, so the planned
     * hit stands in for it once [rotation] meets [surface].
     */
    fun click(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? {
        target ?: return null
        return target.verifyClick(rotation)
            ?: target.blockHitResult.takeIf { throughWalls && surface(target).isHitBy(player.eyePosition, rotation) }
    }

    private fun facePositionFactory(optimalLine: Line?): FaceTargetPositionFactory = if (bridging.holdsLook) {
        heldLook?.let(::DirectionTargetPositionFactory) ?: NearestRotationTargetPositionFactory
    } else {
        when (aimMode) {
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

}

    /**
     * Whether the predicted click lands at least [FACE_MARGIN] inside the edges of its face. A prediction that only
     * grazes the face often misses for real, and the player walks off without SafeWalk stepping in.
     */
    private fun BlockPlacementTarget.isWellInsideFace(): Boolean {
        val bounds = interactedBlockPos.outlineShape.bounds()
        val local = blockHitResult.location.subtract(Vec3.atLowerCornerOf(interactedBlockPos))
        return Direction.Axis.entries.all { axis ->
            axis == direction.axis ||
                local.get(axis) in bounds.min(axis) + FACE_MARGIN..bounds.max(axis) - FACE_MARGIN
        }
    }
