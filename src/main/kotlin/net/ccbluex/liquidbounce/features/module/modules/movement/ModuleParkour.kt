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
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.navigation.walk.MotionTraits
import net.ccbluex.liquidbounce.utils.navigation.walk.PlayerMotion
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkAbilities
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkJumpTargets
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkJumps
import net.ccbluex.liquidbounce.utils.navigation.walk.keys
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * Parkour module
 *
 * Helps with the jumps you run at: picks the block across the gap you are heading for, jumps at the tick that lands
 * best, turns the jump towards it, steers in the air and stops you on small landings. Without such a block it jumps
 * at the very edge.
 */
object ModuleParkour : ClientModule("Parkour", ModuleCategories.MOVEMENT) {

    private val assist by boolean("Assist", true)
    private val maxAngle by float("MaxAngle", 35f, 5f..90f, "°")
    private val rotations = tree(RotationsValueGroup(this))

    /**
     * The block being jumped to, while a jump is assisted.
     */
    private var target: BlockPos? = null
    private var targetHeight = 0.0
    private var aim: Vec3? = null
    private var sprint = false

    /**
     * Whether an assisted jump is in the air, and the ticks since one landed, while the landing may need a stop.
     */
    private var airborne = false
    private var sinceLanding = BRAKE_TICKS

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        val aim = aim ?: return@handler
        val delta = aim.subtract(player.position())
        if (delta.horizontalDistanceSqr() < MIN_AIM_DISTANCE_SQR) {
            return@handler
        }

        RotationManager.setRotationTarget(
            Rotation(delta.yaw, player.xRot),
            valueGroup = rotations,
            priority = Priority.NOT_IMPORTANT,
            provider = this,
        )
    }

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (sprint && event.directionalInput.isMoving) {
            event.sprint = true
        }
    }

    @Suppress("unused")
    private val movementHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
        sprint = false
        val running = event.directionalInput.isMoving && !event.sneak
        val free = !player.isInWater && !player.onClimbable() && !player.abilities.flying
        if (!assist || !running || !free) {
            release()
            if (running) {
                jumpAtEdge(event)
            }
            return@handler
        }

        val terrain = NavigationTerrain(LiveWorld(world))
        val abilities = abilities()
        val state = PlayerMotion(terrain, abilities.motion)
        state.reset(player)
        val jumps = WalkJumps(terrain, abilities)
        val yaw = RotationManager.movementYaw

        if (!player.onGround()) {
            steerInAir(event, jumps, state, yaw)
            return@handler
        }

        if (airborne) {
            airborne = false
            sinceLanding = 0
        } else if (sinceLanding < BRAKE_TICKS) {
            sinceLanding++
        }

        val heading = getMovementDirectionOfInput(player.yRot, event.directionalInput)
        val jump = pickTarget(WalkJumpTargets(terrain, abilities.motion), jumps, state, yaw, heading)
        when {
            jump != null -> {
                event.directionalInput = keys(1, 0, player.yRot)
                event.jump = jump
                airborne = jump
                sprint = true
            }

            sinceLanding < BRAKE_TICKS && stopsOnLanding(event, state, yaw) -> release()

            else -> {
                release()
                jumpAtEdge(event)
            }
        }
    }

    /**
     * Keeps steering towards the block jumped to, pressing the keys that land the furthest inside it.
     */
    private fun steerInAir(event: MovementInputEvent, jumps: WalkJumps, state: PlayerMotion, yaw: Float) {
        val target = target ?: return
        val keys = jumps.airKeys(state, yaw, target, targetHeight) ?: return
        event.directionalInput = keys(keys.forward, keys.strafe, player.yRot)
        event.jump = false
        sprint = keys.forward > 0
    }

    /**
     * Picks the block across a gap within [maxAngle] of [heading] that a jump from here lands on best, and aims at
     * it; whether to jump this tick, or null without one.
     */
    private fun pickTarget(
        targets: WalkJumpTargets,
        jumps: WalkJumps,
        state: PlayerMotion,
        yaw: Float,
        heading: Float,
    ): Boolean? {
        val position = player.position()
        val feet = BlockPos.containing(position)
        val height = targets.terrain.standHeight(feet.x, feet.y, feet.z).takeUnless { it.isNaN() } ?: position.y
        var best: BlockPos? = null
        var bestHeight = 0.0
        var bestScore = Double.MAX_VALUE
        var jumpNow = false
        targets.forEach(feet.x, feet.y, feet.z, height, MAX_DROP) { x, y, z, top ->
            val center = Vec3(x + 0.5, top, z + 0.5)
            val angle = abs(Mth.wrapDegrees(center.subtract(position).yaw - heading))
            if (angle > maxAngle) {
                return@forEach true
            }

            val pos = BlockPos(x, y, z)
            val jump = jumps.fromHere(state, yaw, pos, top) ?: return@forEach true
            val score = angle / maxAngle - jump.margin
            if (score < bestScore) {
                best = pos
                bestHeight = top
                bestScore = score
                jumpNow = jump.jumps && jump.takeoff == 0
            }
            true
        }

        val target = best ?: return null
        this.target = target
        targetHeight = bestHeight
        aim = Vec3(target.x + 0.5, bestHeight, target.z + 0.5)
        return jumpNow
    }

    /**
     * After a landing, holds back when the keys pressed would run off a block too small to stop on later.
     */
    private fun stopsOnLanding(event: MovementInputEvent, state: PlayerMotion, yaw: Float): Boolean {
        val probe = state.copy()
        val input = event.directionalInput
        val forward = (if (input.forwards) 1 else 0) - (if (input.backwards) 1 else 0)
        val strafe = (if (input.left) 1 else 0) - (if (input.right) 1 else 0)
        for (tick in 0 until BRAKE_LOOKAHEAD) {
            probe.tick(yaw, forward, strafe, jump = false, sprint = player.isSprinting)
            if (!probe.onGround) {
                if (player.deltaMovement.horizontalDistance() <= BRAKE_SPEED) {
                    return false
                }
                event.directionalInput = keys(-1, 0, player.yRot)
                return true
            }
        }
        return false
    }

    /**
     * Jumps on the last tick before running off a block.
     */
    private fun jumpAtEdge(event: MovementInputEvent) {
        if (!player.onGround() || mc.options.keyJump.isDown) {
            return
        }

        val simulation = PlayerSimulationCache.getSimulationForLocalPlayer()
        if (!simulation.getSnapshotAt(1).onGround) {
            event.jump = true
        }
    }

    private fun release() {
        target = null
        aim = null
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
        sinceLanding = BRAKE_TICKS
    }

    private const val MAX_DROP = 4
    private const val SAFE_FALL = 3
    private const val WATER_FALL = 23
    private const val MIN_SPRINT_FOOD = 6
    private const val BRAKE_TICKS = 10
    private const val BRAKE_LOOKAHEAD = 3
    private const val BRAKE_SPEED = 0.1
    private const val MIN_AIM_DISTANCE_SQR = 1.0E-4
}
