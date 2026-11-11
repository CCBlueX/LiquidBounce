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
import net.ccbluex.liquidbounce.utils.entity.MotionTraits
import net.ccbluex.liquidbounce.utils.entity.PlayerMotion
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer.SimulatedPlayerInput
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForMovementYaw
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.ccbluex.liquidbounce.utils.navigation.walk.Abilities
import net.ccbluex.liquidbounce.utils.navigation.walk.AssistedJump
import net.ccbluex.liquidbounce.utils.navigation.walk.JumpTargets
import net.ccbluex.liquidbounce.utils.navigation.walk.Jumps
import net.ccbluex.liquidbounce.utils.navigation.walk.LeapInput
import net.ccbluex.liquidbounce.utils.render.route.JumpRenderer

/**
 * Parkour module
 *
 * Jumps at the very edge of a block. With [Assist], it picks the block across the gap you are heading for, jumps at
 * the tick that lands best, turns the jump towards it, steers in the air and stops you on small landings, and shows
 * the jump it goes for.
 */
object ModuleParkour : ClientModule("Parkour", ModuleCategories.MOVEMENT) {

    private val assisted = AssistedJump()

    private object Assist : ToggleableValueGroup(this, "Assist", true) {
        val maxAngle by float("MaxAngle", 35f, 5f..90f, "°")
        val rotations = tree(RotationsValueGroup(this))
        val render = tree(JumpRenderer(this, assisted))
    }

    init {
        tree(Assist)
    }

    private var aimYaw = Float.NaN
    private var sprint = false

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        assisted.target ?: return@handler
        val yaw = assisted.aimYaw(player.position(), player.deltaMovement, player.onGround())
        if (!yaw.isNaN()) {
            aimYaw = yaw
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

        val keys = assist(event)
        if (assisted.target == null) {
            aimYaw = Float.NaN
        }
        if (keys == null) {
            jumpAtEdge(event)
            return@handler
        }

        event.directionalInput = getDirectionalInputForMovementYaw(keys.forward, keys.strafe, player.yRot)
        event.jump = keys.jump
        event.sneak = keys.sneak
        sprint = keys.sprint
    }

    /** The keys the assist presses this tick, or null when there is nothing to assist. */
    private fun assist(event: MovementInputEvent): LeapInput? {
        val terrain = Terrain.live()
        val abilities = abilities()
        val state = PlayerMotion(terrain, abilities.motion)
        state.reset(player)

        val input = event.directionalInput
        val keys = LeapInput(
            forward = (if (input.forwards) 1 else 0) - (if (input.backwards) 1 else 0),
            strafe = (if (input.left) 1 else 0) - (if (input.right) 1 else 0),
            jump = event.jump,
            sprint = player.isSprinting,
        )
        val heading = getMovementDirectionOfInput(player.yRot, input)
        val yaw = RotationManager.movementYaw
        val jumps = Jumps(terrain, abilities)
        val assist = assisted.tick(
            state, yaw, heading, keys, Assist.maxAngle, jumps, JumpTargets(terrain, abilities.motion),
        )
        if (Assist.render.enabled) {
            assisted.trace(state, yaw, jumps)
        }
        return assist
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
        assisted.release()
        aimYaw = Float.NaN
    }

    private fun abilities() = Abilities(
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
        assisted.reset()
        aimYaw = Float.NaN
        sprint = false
    }

    private const val SAFE_FALL = 3
    private const val WATER_FALL = 23
    private const val MIN_SPRINT_FOOD = 6
}
