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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.block.hole.HoleManager
import net.ccbluex.liquidbounce.utils.block.hole.HoleManagerSubscriber
import net.ccbluex.liquidbounce.utils.block.hole.HoleTracker
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max

/**
 * Module Anchor
 *
 * Pulls you into safe holes.
 *
 * @author ccetl
 */
object ModuleAnchor : ClientModule(
    "Anchor",
    Category.MOVEMENT,
    bindAction = InputBind.BindAction.HOLD,
    disableOnQuit = true
), HoleManagerSubscriber {

    private val maxDistance by float("MaxDistance", 4f, 0.1f..6f)
    private val horizontalSpeed by float("HorizontalSpeed", 0.3f, 0f..10f)
    private val verticalSpeed by float("VerticalSpeed", 0.1f, 0f..10f)

    var goal: Vec3d? = null

    override fun onEnabled() {
        HoleManager.subscribe(this)
        goal = null
    }

    override fun onDisabled() {
        HoleManager.unsubscribe(this)
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        // if we're already in a hole, we should just center us in that
        val playerBB = player.boundingBox
        HoleTracker.holes.firstOrNull { hole -> playerBB.intersects(hole.positions.getBoundingBox()) }?.let { hole ->
            goal = hole.positions.getBottomFaceCenter()
            return@tickHandler
        }

        val playerPos = player.pos
        val maxDistanceSq = maxDistance.sq()

        // check if the current goal is still okay, don't update it then
        goal?.let { vec3d ->
            if (vec3d.squaredDistanceTo(playerPos) > maxDistanceSq) {
                return@let
            }
        }

        // not in a hole and no valid goal means we need to search one
        goal = HoleTracker.holes
            .filter { hole -> hole.positions.to.y + 1.0 <= playerPos.y }
            .map { hole -> hole.positions.getBottomFaceCenter() }
            .filter { vec3d -> vec3d.squaredDistanceTo(playerPos) <= maxDistanceSq }
            .minByOrNull { vec3d -> vec3d.squaredDistanceTo(playerPos) }
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        val goal = goal ?: return@handler

        // determine the desired movement
        val delta = goal.subtract(player.pos)

        // apply the movement
        val movement = event.movement
        modifyHorizontalSpeed(movement, delta, goal)
        modifyVerticalSpeed(movement, delta)
    }

    private fun modifyHorizontalSpeed(movement: Vec3d, delta: Vec3d, goal: Vec3d) {
        if (horizontalSpeed == 0f) {
            // only cancel the movement if the player would fall into the hole
            val playerBB = player.boundingBox

            // is the player bounding box within the hole (ignoring y)?
            if (playerBB.minX > goal.x - 0.5 &&
                playerBB.maxX < goal.x + 0.5 &&
                playerBB.minZ > goal.z - 0.5 &&
                playerBB.maxZ < goal.z + 0.5) {
                movement.x = 0.0
                movement.z = 0.0
            }

            return
        }

        // determine the speed limit
        val horizontalSpeedLimit = if (horizontalSpeed == 0f) {
            0.0
        } else {
            max(horizontalSpeed.toDouble(), hypot(movement.x, movement.z))
        }

        // clamp the speed
        val exceedsHSpeed = hypot(delta.x, delta.z) > horizontalSpeedLimit
        if (exceedsHSpeed) {
            val adjusted = delta.normalize().multiply(horizontalSpeedLimit, 0.0, horizontalSpeedLimit)
            delta.x = adjusted.x
            delta.z = adjusted.z
        }

        // modify the original movement
        movement.x = delta.x
        movement.z = delta.z
    }

    private fun modifyVerticalSpeed(movement: Vec3d, delta: Vec3d) {
        if (verticalSpeed == 0f) {
            return
        }

        // determine the speed limit
        val verticalSpeedLimit = max(verticalSpeed.toDouble(), abs(movement.y))

        // clamp the speed
        val exceedsVSpeed = abs(delta.y) > verticalSpeedLimit
        if (exceedsVSpeed) {
            delta.y = delta.normalize().y * verticalSpeedLimit
        }

        // modify the original movement
        movement.y = delta.y
    }

    override val running: Boolean
        get() = super.running && !player.isGliding && !player.isSpectator && !player.isSwimming

    override fun horizontalDistance() = ceil(maxDistance).toInt()

    override fun verticalDistance() = ceil(maxDistance).toInt()

}
