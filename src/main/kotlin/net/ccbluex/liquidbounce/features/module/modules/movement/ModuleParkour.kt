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
package net.ccbluex.liquidbounce.features.module.modules.movement

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer.SimulatedPlayerInput
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.navigation.walk.MotionTraits
import net.ccbluex.liquidbounce.utils.navigation.walk.PlayerMotion
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkAbilities
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkJumpTargets
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkJumps
import net.ccbluex.liquidbounce.utils.navigation.walk.ifNaN
import net.ccbluex.liquidbounce.utils.navigation.walk.keys
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * Parkour module
 *
 * Jumps at the very edge of a block. With [Assist], it picks the block across the gap you are heading for, jumps at
 * the tick that lands best, turns the jump towards it, steers in the air and stops you on small landings.
 */
object ModuleParkour : ClientModule("Parkour", ModuleCategories.MOVEMENT) {

    private object Assist : ToggleableValueGroup(this, "Assist", true) {
        val maxAngle by float("MaxAngle", 35f, 5f..90f, "°")
        val rotations = tree(RotationsValueGroup(this))
    }

    init {
        tree(Assist)
    }

    private class JumpTarget(val pos: BlockPos, val aim: Vec3, val jumpNow: Boolean)

    private var target: JumpTarget? = null
    private var aimYaw = Float.NaN
    private var sprint = false

    /** Whether an assisted jump is in the air. */
    private var airborne = false
    private var ticksSinceLanding = BRAKE_TICKS

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        val target = target ?: return@handler
        val delta = target.aim.subtract(player.position())
        // Close to the aim, the way there turns around once the player passes it, so the yaw is held.
        if (delta.horizontalDistanceSqr() >= MIN_AIM_DISTANCE * MIN_AIM_DISTANCE) {
            aimYaw = delta.yaw
        }
        if (aimYaw.isNaN()) {
            return@handler
        }

        RotationManager.setRotationTarget(
            Rotation(aimYaw, player.xRot),
            valueGroup = Assist.rotations,
            priority = Priority.NOT_IMPORTANT,
            provider = this,
        )
    }

    /** Holds the sprint key; the game still decides whether the player sprints, and tells the server. */
    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (sprint && event.source == SprintEvent.Source.INPUT && event.directionalInput.isMoving) {
            event.sprint = true
        }
    }

    @Suppress("unused")
    private val movementHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
        sprint = false
        val running = event.directionalInput.isMoving && !event.sneak
        val free = !player.isInWater && !player.onClimbable() && !player.abilities.flying
        if (!Assist.enabled || !running || !free) {
            release()
            if (running) {
                jumpAtEdge(event)
            }
            return@handler
        }

        if (!assist(event)) {
            jumpAtEdge(event)
        }
    }

    /** False when there is nothing to assist this tick. */
    private fun assist(event: MovementInputEvent): Boolean {
        val terrain = NavigationTerrain.live()
        val abilities = abilities()
        val state = PlayerMotion(terrain, abilities.motion)
        state.reset(player)
        val jumps = WalkJumps(terrain, abilities)
        val yaw = RotationManager.movementYaw

        if (!player.onGround()) {
            steerInAir(event, jumps, state, yaw)
            return true
        }

        if (airborne) {
            airborne = false
            ticksSinceLanding = 0
        } else if (ticksSinceLanding < BRAKE_TICKS) {
            ticksSinceLanding++
        }

        val heading = getMovementDirectionOfInput(player.yRot, event.directionalInput)
        val target = pickTarget(WalkJumpTargets(terrain, abilities.motion), jumps, state, yaw, heading)
        if (target == null) {
            release()
            return ticksSinceLanding < BRAKE_TICKS && stopsOnLanding(event, state, yaw)
        }

        this.target = target
        event.directionalInput = keys(1, 0, player.yRot)
        event.jump = target.jumpNow
        airborne = target.jumpNow
        sprint = true
        return true
    }

    /** Presses the keys that land the furthest inside the block jumped to. */
    private fun steerInAir(event: MovementInputEvent, jumps: WalkJumps, state: PlayerMotion, yaw: Float) {
        val target = target ?: return
        val air = jumps.airKeys(state, yaw, target.pos, target.aim.y) ?: return
        event.directionalInput = keys(air.forward, air.strafe, player.yRot)
        event.jump = false
        sprint = air.forward > 0
    }

    /** The block across a gap within MaxAngle of [heading] that a jump from here lands on best. */
    private fun pickTarget(
        targets: WalkJumpTargets,
        jumps: WalkJumps,
        state: PlayerMotion,
        yaw: Float,
        heading: Float,
    ): JumpTarget? {
        val position = player.position()
        // Over the edge, the player still stands on the block behind it.
        val feet = BlockPos.containing(position).let { feet ->
            player.mainSupportingBlockPos.map { BlockPos(it.x, feet.y, it.z) }.orElse(feet)
        }
        val height = targets.terrain.standHeight(feet.x, feet.y, feet.z).ifNaN { position.y }
        val maxAngle = Assist.maxAngle
        var best: JumpTarget? = null
        var bestScore = Double.MAX_VALUE
        targets.forEach(feet.x, feet.y, feet.z, height, MAX_DROP) { x, y, z, top ->
            val aim = Vec3(x + 0.5, top, z + 0.5)
            val angle = abs(Mth.wrapDegrees(aim.subtract(position).yaw - heading))
            if (angle > maxAngle) {
                return@forEach true
            }

            val pos = BlockPos(x, y, z)
            val jump = jumps.fromHere(state, yaw, pos, top) ?: return@forEach true
            val score = angle / maxAngle - jump.margin
            if (score < bestScore) {
                best = JumpTarget(pos, aim, jumpNow = jump.jumps && jump.takeoff == 0)
                bestScore = score
            }
            true
        }
        return best
    }

    /** After a landing, holds back when the keys pressed would run off a block too small to stop on later. */
    private fun stopsOnLanding(event: MovementInputEvent, state: PlayerMotion, yaw: Float): Boolean {
        val input = event.directionalInput
        val forward = (if (input.forwards) 1 else 0) - (if (input.backwards) 1 else 0)
        val strafe = (if (input.left) 1 else 0) - (if (input.right) 1 else 0)
        val probe = state.copy()
        val runsOff = (0 until BRAKE_LOOKAHEAD).any {
            probe.tick(yaw, forward, strafe, jump = false, sprint = player.isSprinting)
            !probe.onGround
        }
        if (!runsOff || player.deltaMovement.horizontalDistance() <= BRAKE_SPEED) {
            return false
        }

        event.directionalInput = keys(-1, 0, player.yRot)
        return true
    }

    /** Jumps on the last tick before running off a block. */
    private fun jumpAtEdge(event: MovementInputEvent) {
        if (!player.onGround() || mc.options.keyJump.isDown) {
            return
        }

        // The shared simulation of the player is renewed at this priority too, so it may still be the last tick's.
        val input = SimulatedPlayerInput.fromClientPlayer(event.directionalInput, jump = false, sneaking = event.sneak)
        val simulation = SimulatedPlayer.fromClientPlayer(input)
        simulation.tick()
        if (!simulation.onGround()) {
            event.jump = true
        }
    }

    private fun release() {
        target = null
        aimYaw = Float.NaN
    }

    private fun abilities() = WalkAbilities(
        canBreak = false,
        canPlace = false,
        placeableBlocks = 0,
        canParkour = true,
        riskyJumps = true,
        canSprint = player.foodData.foodLevel > MIN_SPRINT_FOOD,
        safeFall = SAFE_FALL,
        waterFall = WATER_FALL,
        breakTicks = { Double.POSITIVE_INFINITY },
        avoid = LongOpenHashSet(),
        motion = MotionTraits.capture(player),
    )

    override fun onDisabled() {
        release()
        sprint = false
        airborne = false
        ticksSinceLanding = BRAKE_TICKS
    }

    private const val MAX_DROP = 4
    private const val SAFE_FALL = 3
    private const val WATER_FALL = 23
    private const val MIN_SPRINT_FOOD = 6
    private const val BRAKE_TICKS = 10
    private const val BRAKE_LOOKAHEAD = 3
    private const val BRAKE_SPEED = 0.1
    private const val MIN_AIM_DISTANCE = 0.75
}
