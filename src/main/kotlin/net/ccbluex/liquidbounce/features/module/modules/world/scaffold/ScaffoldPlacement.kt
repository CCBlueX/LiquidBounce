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
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldMovementPrediction
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.block.outlineShape
import net.ccbluex.liquidbounce.utils.block.state
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
import net.ccbluex.liquidbounce.utils.block.targetfinding.MidHeightTargetPositionFactory
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
import net.ccbluex.liquidbounce.utils.entity.wouldBeCloseToFallOff
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.hypot
import kotlin.math.round

/** Blocks a predicted click has to land inside the edges of its face, see [ScaffoldPlacement.canPlaceInTime]. */
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

    /** How to aim at the face when the bridging holds no look. */
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
        heldLook = bridging.look()
        val position = if (bridging.shouldHoldLook) heldLookPos(predictedPos, optimalLine) else predictedPos
        val offsets = when {
            ModuleFreeze.running -> BlockPosOffsets.FULL.offsets
            // In the air Telly's eyes can be two blocks past the end of the row on a diagonal
            ScaffoldDownFeature.shouldGoDown || ScaffoldBridgingTelly.placementPos != null ->
                BlockPosOffsets.DOWN.offsets
            else -> BlockPosOffsets.NORMAL.offsets
        }
        val priorityComparator = if (optimalLine != null) {
            // Prioritize the block that is closest to the line
            BlockPlacementTargetFindingOptions.leastBlockDistanceToLine(optimalLine)
        } else {
            BlockPlacementTargetFindingOptions.leastBlockDistanceToPos(position)
        }

        val searchOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(offsets, priorityComparator),
            FaceHandlingOptions(facePositionFactory(optimalLine), considerFacingAwayFaces = throughWalls),
            stackToPlaceWith = bestStack,
            PlayerLocationOnPlacement(
                position = position,
                pose = predictedPose,
                // Nearest to the held look, Telly's look back down the row, or to where scaffold aims rather than to a
                // view still turning there
                rotation = heldLook ?: ScaffoldBridgingTelly.waitingLook() ?: ModuleScaffold.aim
                    ?: RotationManager.serverRotation,
            ),
        )
        val needed = getTargetedPosition(position.toBlockPos())
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
        // A hand rests a little off the look it holds, and the view is what clicks
        val clicking = if (bridging.shouldHoldLook && !ModuleScaffold.isLiningUp()) {
            RotationManager.currentRotation ?: player.rotation
        } else {
            look
        }
        val now = placementTargetClickedBy(clicking, player.eyePosition, bestStack, positions)
            ?.takeIf { isValidCrosshairTarget(it.blockHitResult) }
        // The velocity keeps the input's push while an edge holds the player back, the position does not move
        val moved = hypot(player.x - player.xo, player.z - player.zo)
        // Below 0.01 blocks a tick the player counts as standing
        stalledTicks = if (now == null && player.onGround() && moved < 0.01) {
            stalledTicks + 1
        } else {
            0
        }
        keptLook = look.takeIf { now != null || shouldKeepLook(look, bestStack, positions) }
        return now ?: best
    }

    /**
     * The look [ModuleScaffold.bridging] would rather hold than turn to a point on the face: the look it holds, or
     * where scaffold aims already for the modes aiming nearest to it. Modes aiming at a point have none.
     */
    private fun recommendedLook(): Rotation? = when {
        bridging.shouldHoldLook -> heldLook
        aimMode == AimMode.STABILIZED || aimMode == AimMode.NEAREST_ROTATION -> {
            val view = RotationManager.currentRotation ?: player.rotation
            // Once the hand has settled near the aim, the view on the mouse grid is what clicks, and it may just miss
            ModuleScaffold.aim?.takeIf { aim -> view.rotationDeltaLengthTo(aim) > RotationUtil.gcd } ?: view
        }
        else -> null
    }

    /**
     * Whether [look] stays although it does not place right now. A held look meets the face at the latest where
     * SafeWalk stops the player, so it stays on the ground until the player stood there for [STALL_TICKS], and through
     * a hop, where a slow aim would not be back in time. Any other look stays only while it places one of the next
     * ticks.
     */
    private fun shouldKeepLook(look: Rotation, stack: ItemStack, positions: List<BlockPos>) =
        if (bridging.shouldHoldLook) {
            (player.onGround() || ModuleScaffold.isHopping) && stalledTicks < STALL_TICKS
        } else {
            canPlaceSoon(look, stack, positions)
        }

    /**
     * Whether [rotation] puts [stack] at one of [positions] from where [simulation] is in one of its next [ticks]
     * ticks while still on the ground. If that's at the edge, SafeWalk holds the player there until it does. [safely]
     * only counts a click that lands clear of the face's edges and leaves the player standing on a block.
     */
    private fun canPlaceSoon(
        rotation: Rotation,
        stack: ItemStack,
        positions: List<BlockPos>,
        simulation: SimulatedPlayer =
            SimulatedPlayer.fromClientPlayer(SimulatedInput.fromClientPlayer(DirectionalInput(player.input))),
        ticks: Int = HOLD_TICKS,
        safely: Boolean = false,
    ): Boolean {
        repeat(ticks) {
            simulation.tick()
            if (!simulation.onGround()) {
                return false
            }
            val placement = placementTargetClickedBy(rotation, simulation.eyePosition, stack, positions)
                ?.takeIf { isValidCrosshairTarget(it.blockHitResult) }
            if (placement != null &&
                (!safely || placement.isWellInsideFace() && canStandAt(simulation.position(), placement.placedBlock))
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Whether [rotation] puts [target] down before the player, moving on with [input] without sneaking, leaves the
     * ground. Only a view that reached the aim, or rests on the look the bridging holds, stays the same over those
     * ticks. A look meets a face a few hundredths of a block from its edge, so the walk is simulated as the rotation
     * turns it.
     */
    fun canPlaceInTime(target: BlockPlacementTarget, rotation: Rotation, input: DirectionalInput): Boolean {
        val held = ModuleScaffold.aim?.let { aim -> rotation.rotationDeltaLengthTo(aim) <= RotationUtil.gcd } == true
        if (!held && !bridging.shouldHoldLook) {
            return false
        }
        // Scaffold leaves a jump off the bridge out while no block can go down in the air. SafeWalk is not asked,
        // which would ask scaffold again
        val jumps = player.input.keyPresses.jump && ModuleScaffold.canPlaceInAir
        val simulation = SimulatedPlayer.fromClientPlayer(input, jumps)
        return canPlaceSoon(rotation, stack, listOf(target.placedBlock), simulation, safely = true)
    }

    /**
     * Whether [rotation], held through a jump from here with [input], builds what the jump comes down on: [target]
     * on its first tick, where the lifted eyes meet the face from further out than they do walking, and a block on top
     * of it while the player is above that, in the place it comes down.
     */
    fun canPlaceInJump(target: BlockPlacementTarget, rotation: Rotation, input: DirectionalInput): Boolean {
        val simulation = SimulatedPlayer.fromClientPlayer(input, jump = true)
        simulation.tick()
        val places = placementTargetClickedBy(rotation, simulation.eyePosition, stack, listOf(target.placedBlock))
            ?.takeIf { isValidCrosshairTarget(it.blockHitResult) }
            ?.isWellInsideFace() == true
        if (!places) {
            return false
        }
        // Scaffold lets go of the keys in the air, and 8 ticks on the jump is back down at the height of the top block
        simulation.input = SimulatedInput(DirectionalInput.NONE.toInput().toAgent())
        val reach = rotation.directionVector.scale(player.blockInteractionRange())
        // The upper side of the block the first tick places, without its edges
        val below = AABB(target.placedBlock).inflate(-FACE_MARGIN, 0.0, -FACE_MARGIN)
        var meetsTop = false
        repeat(8) {
            simulation.tick()
            val eyes = simulation.eyePosition
            val isAbove = simulation.position().y > below.maxY + 1
            val hit = below.clip(eyes, eyes.add(reach)).orElse(null)
            meetsTop = meetsTop || isAbove && hit != null && hit.y > below.maxY - 1.0E-4
        }
        return meetsTop && canStandAt(simulation.position(), target.placedBlock.above())
    }

    fun rotation(target: BlockPlacementTarget?): Rotation? {
        if (ScaffoldBridgingTelly.isFacingForward) {
            return when (ScaffoldBridgingTelly.resetMode) {
                ScaffoldBridgingTelly.ResetMode.REVERSE -> Rotation(
                    round(player.rotation.yaw / 45) * 45,
                    if (player.xRot < 45f) 45f else player.xRot
                )

                // Turned to rather than let go of, which would wait out a reaction first
                ScaffoldBridgingTelly.ResetMode.RESET -> player.rotation
            }
        }

        keptLook?.let { return it }
        val target = target ?: return ScaffoldBridgingTelly.waitingLook()
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
    val isHoldingLook
        get() = keptLook != null && bridging.shouldHoldLook

    /** What a look has to meet to place [target]: the face it clicks. */
    fun surface(target: BlockPlacementTarget) = target.surface(throughWalls)

    /**
     * The hit a click with [rotation] places [target] with. Through blocks the ray never gets there, so the planned
     * hit stands in for it once [rotation] meets [surface].
     */
    fun hitFor(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? {
        target ?: return null
        return target.verifyClick(rotation)
            ?: target.blockHitResult.takeIf { throughWalls && surface(target).isHitBy(player.eyePosition, rotation) }
    }

    private fun facePositionFactory(optimalLine: Line?): FaceTargetPositionFactory = when {
        bridging.shouldHoldLook -> NearestRotationTargetPositionFactory
        // Telly drags along the row looking back down it: on a diagonal the faces of both sides lie in that look
        bridging === ScaffoldBridgingTelly && ModuleScaffold.canPlaceInAir -> MidHeightTargetPositionFactory
        else -> when (aimMode) {
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

/**
 * Whether the player stands on a block at [position] once [placed] is down. Along a diagonal a held look often
 * places the block beside the player's way first, and the one under the player would come a tick too late.
 */
private fun canStandAt(position: Vec3, placed: BlockPos): Boolean {
    if (!player.wouldBeCloseToFallOff(position)) {
        return true
    }
    val feet = player.dimensions.makeBoundingBox(position).inflate(-0.05, 0.0, -0.05).move(0.0, -0.5, 0.0)
    return feet.intersects(AABB(placed))
}

/**
 * Where a held look puts the next block down from. Up to two ticks before an edge scaffold predicts from where the
 * player is, whose middle is still over the last block then, and the face of the next one is still turned away from
 * the eyes: without a target scaffold would stop the player there, before the look can meet the face. The player
 * clicks where it leaves the block.
 */
private fun heldLookPos(predictedPos: Vec3, line: Line?): Vec3 {
    if (line == null || getTargetedPosition(predictedPos.toBlockPos()).state?.isAir != false) {
        return predictedPos
    }
    return ScaffoldMovementPrediction.getFallOffPositionOnLine(line) ?: predictedPos
}
