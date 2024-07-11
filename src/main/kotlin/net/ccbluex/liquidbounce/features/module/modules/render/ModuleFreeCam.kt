/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
@file:Suppress("detekt:TooManyFunctions")

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

/**
 * FreeCam module
 *
 * Allows you to move out of your body.
 */
object ModuleFreeCam : Module("FreeCam", Category.RENDER, disableOnQuit = true) {

    private val speed by float("Speed", 1f, 0.1f..2f)

    /**
     * Allows to interact from the camera perspective. This is very useful to interact with blocks that
     * are behind the player or walls. Similar functionality to the GhostBlock module.
     */
    private val allowCameraInteract by boolean("AllowCameraInteract", true)

    /**
     * Allows to change the player's rotation while in FreeCam mode. This is useful to look around while
     */
    private val allowRotationChange by boolean("AllowRotationChange", true)

    private data class PositionPair(var pos: Vec3d, var lastPos: Vec3d) {
        operator fun plusAssign(velocity: Vec3d) {
            lastPos = pos
            pos += velocity
        }

        fun interpolate(tickDelta: Float) = Vec3d(
            lastPos.x + (pos.x - lastPos.x) * tickDelta,
            lastPos.y + (pos.y - lastPos.y) * tickDelta,
            lastPos.z + (pos.z - lastPos.z) * tickDelta
        )

    }

    private var pos: PositionPair? = null

    override fun enable() {
        updatePosition(Vec3d.ZERO)
        super.enable()
    }

    override fun disable() {
        pos = null

        // Reset player rotation
        if (!allowRotationChange) {
            val rotation = ModuleRotations.displayRotations()

            player.yaw = rotation.yaw
            player.pitch = rotation.pitch
        }
        super.disable()
    }

    val inputHandler = handler<MovementInputEvent> { event ->
        val speed = this.speed.toDouble()
        val yAxisMovement = when {
            event.jumping -> 1.0f
            event.sneaking -> -1.0f
            else -> 0.0f
        }
        val directionYaw = getMovementDirectionOfInput(player.yaw, event.directionalInput)

        val velocity = Vec3d.of(Vec3i.ZERO)
            .apply { strafe(directionYaw, speed, keyboardCheck = true) }
            .withAxis(Direction.Axis.Y, yAxisMovement * speed)
        updatePosition(velocity)

        event.directionalInput = DirectionalInput.NONE
        event.jumping = false
        event.sneaking = false
    }

    fun applyCameraPosition(entity: Entity, tickDelta: Float) {
        val camera = mc.gameRenderer.camera

        if (!enabled || entity != player) {
            return
        }

        return camera.setPos(pos?.interpolate(tickDelta) ?: return)
    }

    fun renderPlayerFromAllPerspectives(entity: LivingEntity): Boolean {
        if (!enabled || entity != player) {
            return entity.isSleeping
        }

        return entity.isSleeping || !mc.gameRenderer.camera.isThirdPerson
    }

    /**
     * Modify the raycast position
     */
    fun modifyRaycast(original: Vec3d, entity: Entity, tickDelta: Float): Vec3d {
        if (!enabled || entity != mc.player || !allowCameraInteract) {
            return original
        }

        return pos?.interpolate(tickDelta) ?: original
    }

    fun shouldDisableCrosshair() = enabled && !allowCameraInteract

    fun shouldDisableRotations() = enabled && !allowRotationChange

    private fun updatePosition(velocity: Vec3d) {
        pos = (pos ?: PositionPair(player.eyes, player.eyes)).apply { this += velocity }
    }

}
