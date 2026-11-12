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
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
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
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sin

/** Degrees a GodBridge look turns away from straight back while bridging straight. */
private const val GOD_BRIDGE_SIDE = 45f

/**
 * How steeply a GodBridge look points down: steep enough to meet the face of a row a little before its edge, where
 * SafeWalk would stop the player, and still at that edge.
 */
private const val GOD_BRIDGE_PITCH = 79f

/** Degrees past halfway to the next 45 degree step the movement has to turn before the GodBridge look follows. */
private const val GOD_BRIDGE_STEP_HOLD = 10f

/**
 * Blocks the player has to stand off the middle of the row before the GodBridge look changes sides. Until then the
 * look still meets the face, and StabilizeMovement's pull towards the middle may overshoot it a little.
 */
private const val GOD_BRIDGE_SIDE_HOLD = 0.15

/** Ticks ahead a kept look may still place the next block, see [ScaffoldPlacement.findTarget]. */
private const val HOLD_TICKS = 3

/** Ticks a GodBridge look may leave the player standing at an edge before scaffold aims at the face instead. */
private const val STALL_TICKS = 3

/** Blocks per tick below which the player counts as standing. */
private const val STALL_SPEED = 0.01

/**
 * Where scaffold puts the next block while it bridges, and where on it [ModuleScaffold.aimMode] recommends to look.
 */
internal object ScaffoldPlacement {

    /** The look scaffold keeps this tick because it places the next block now or soon, see [findTarget]. */
    private var keptLook: Rotation? = null

    /** Ticks the player has stood without the kept GodBridge look placing anything. */
    private var stalledTicks = 0

    /** The 45 degree step of the movement the GodBridge look is held against, null before the player moved. */
    private var godBridgeMovement: Float? = null

    /** 1 or -1: the side of straight back the GodBridge look turns to. */
    private var godBridgeSide = 1f

    /** The stack [findTarget] last looked for a placement with. */
    private var stack = ItemStack.EMPTY

    /** Whether the look may meet the clicked face through blocks, and the face may be turned away from the eyes. */
    private val throughWalls
        get() = ModuleScaffold.visibility == Visibility.NOT_VISIBLE || ScaffoldDownFeature.shouldGoDown

    fun reset() {
        keptLook = null
        stalledTicks = 0
        godBridgeMovement = null
        godBridgeSide = 1f
    }

    /**
     * The best placement for the block the player needs, unless the look [ModuleScaffold.aimMode] recommends to hold
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
            FaceHandlingOptions(facePositionFactory(optimalLine, predictedPos), considerFacingAwayFaces = throughWalls),
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
            // Nothing to place: a GodBridge look stays rather than going back to the camera
            keptLook = if (ModuleScaffold.aimMode == AimMode.GOD_BRIDGE) godBridgeLook(predictedPos) else null
            return null
        }
        val look = recommendedLook(predictedPos) ?: return best

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
        stalledTicks = if (now == null && player.onGround() && moved < STALL_SPEED) {
            stalledTicks + 1
        } else {
            0
        }
        keptLook = look.takeIf { now != null || keepsLook(look, bestStack, positions) }
        return now ?: best
    }

    /**
     * The look [ModuleScaffold.aimMode] would rather hold than turn to a point on the face: where scaffold aims
     * already for the modes aiming nearest to it, the GodBridge look for GodBridge. Modes aiming at a point have none.
     */
    private fun recommendedLook(position: Vec3): Rotation? = when (ModuleScaffold.aimMode) {
        AimMode.STABILIZED, AimMode.NEAREST_ROTATION -> {
            val view = RotationManager.currentRotation ?: player.rotation
            // Once the hand has settled near the aim, the view on the mouse grid is what clicks, and it may just miss
            ModuleScaffold.aim?.takeIf { aim -> view.rotationDeltaLengthTo(aim) > RotationUtil.gcd } ?: view
        }
        AimMode.GOD_BRIDGE -> godBridgeLook(position)
        else -> null
    }

    /**
     * Whether [look] stays although it does not place right now. A GodBridge look meets the face at the latest where
     * SafeWalk stops the player, so it stays on the ground until the player stood there for [STALL_TICKS]; in the air
     * it only reaches the row the player left. Any other look stays only while it places one of the next ticks.
     */
    private fun keepsLook(look: Rotation, stack: ItemStack, positions: List<BlockPos>) =
        if (ModuleScaffold.aimMode == AimMode.GOD_BRIDGE) {
            player.onGround() && stalledTicks < STALL_TICKS
        } else {
            placesSoon(look, stack, positions)
        }

    /**
     * Whether [rotation] puts [stack] at one of [positions] from where the player is in one of the next [HOLD_TICKS]
     * ticks while still on the ground. If that's at the edge, SafeWalk holds the player there until it does.
     */
    private fun placesSoon(
        rotation: Rotation,
        stack: ItemStack,
        positions: List<BlockPos>,
        input: SimulatedPlayer.SimulatedPlayerInput =
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(DirectionalInput(player.input)),
    ): Boolean {
        val simulation = SimulatedPlayer.fromClientPlayer(input)
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

    /**
     * Whether [rotation] puts [target] down before the player, walking on without sneaking, leaves the ground. Only a
     * view that reached the aim stays the same over those ticks.
     */
    fun placesInTime(target: BlockPlacementTarget, rotation: Rotation): Boolean {
        val aim = ModuleScaffold.aim ?: return false
        if (rotation.rotationDeltaLengthTo(aim) > RotationUtil.gcd) {
            return false
        }
        // Built by hand: asking for SafeWalk here would ask scaffold again
        val input = SimulatedPlayer.SimulatedPlayerInput(
            DirectionalInput(player.input),
            jumping = player.input.keyPresses.jump,
            sprinting = player.isSprinting || player.input.keyPresses.sprint,
            sneaking = false,
        )
        return placesSoon(rotation, stack, listOf(target.placedBlock), input)
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
     * Whether [rotation] holds the GodBridge look, which is where to look exactly. A held aim of the other modes is
     * only somewhere on the face, and the hand may rest anywhere it still meets it.
     */
    val holdsGodBridgeLook
        get() = keptLook != null && ModuleScaffold.aimMode == AimMode.GOD_BRIDGE

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

    private fun facePositionFactory(optimalLine: Line?, position: Vec3): FaceTargetPositionFactory =
        when (ModuleScaffold.aimMode) {
            AimMode.CENTER -> CenterTargetPositionFactory
            AimMode.RANDOM -> RandomTargetPositionFactory
            AimMode.STABILIZED -> StabilizedRotationTargetPositionFactory(optimalLine)
            AimMode.NEAREST_ROTATION -> NearestRotationTargetPositionFactory
            AimMode.REVERSE_YAW -> ReverseYawTargetPositionFactory
            AimMode.DIAGONAL_YAW -> DiagonalYawTargetPositionFactory
            AimMode.ANGLE_YAW -> AngleYawTargetPositionFactory
            AimMode.EDGE_POINT -> EdgePointTargetPositionFactory
            AimMode.GOD_BRIDGE -> godBridgeLook(position)?.let(::DirectionTargetPositionFactory)
                ?: NearestRotationTargetPositionFactory
        }

    /**
     * The look a god bridging player holds the mouse at: back against the movement and [GOD_BRIDGE_SIDE] degrees
     * towards the middle of the row, or straight back along a diagonal, steeply down. It stays the same block after
     * block and only follows the movement once it turned well into another 45 degree step.
     */
    private fun godBridgeLook(position: Vec3): Rotation? {
        val input = ModuleScaffold.rawInput
        if (input != DirectionalInput.NONE) {
            val movement = player.getMovementDirectionOfInput(input)
            val held = godBridgeMovement
            if (held == null || abs(angleDifference(movement, held)) > 45f / 2 + GOD_BRIDGE_STEP_HOLD) {
                godBridgeMovement = round(movement / 45f) * 45f
            }
        }

        val movement = godBridgeMovement ?: return null
        if (movement % 90f != 0f) {
            return Rotation(movement + 180f, GOD_BRIDGE_PITCH)
        }

        // How far the player stands right of the row's middle, seen along the movement
        val radians = Math.toRadians(movement.toDouble())
        val right = -(position.x - floor(position.x) - 0.5) * cos(radians) -
            (position.z - floor(position.z) - 0.5) * sin(radians)
        if (abs(right) > GOD_BRIDGE_SIDE_HOLD) {
            godBridgeSide = sign(right).toFloat()
        }
        // Looking back, the middle of a player standing right of it is on the right, where yaw grows
        return Rotation(movement + 180f + godBridgeSide * GOD_BRIDGE_SIDE, GOD_BRIDGE_PITCH)
    }

}
