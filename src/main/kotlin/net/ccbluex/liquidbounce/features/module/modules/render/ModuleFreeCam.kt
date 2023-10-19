/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.event.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.entity.yAxisMovement
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
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

object ModuleFreeCam : Module("FreeCam", Category.RENDER) {

    private val speed by float("Speed", 1f, 0.1f..2f)
    private val freeze by boolean("Freeze", false)
    private val interactFromCamera by boolean("InteractFromCamera", false)
    private val renderCrosshair by boolean("RenderCrosshair", false)
    private val renderHand by boolean("RenderHand", true)
    private val disableRotations by boolean("DisableRotations", true)
    private val resetRotation by boolean("ResetRotation", true)

    private var pos = Vec3d.ZERO
    private var lastPos = Vec3d.ZERO

    override fun enable() {
        updatePosition(player.eyes, lastPosBeforePos = false, increase = false)
    }

    val tickHandler = handler<PlayerTickEvent> {
        if (player.age < 3) {
            updatePosition(player.eyes, lastPosBeforePos = false, increase = false)
        }

        val speed = this.speed.toDouble()

        val velocity = Vec3d.of(Vec3i.ZERO).apply { strafe(player.directionYaw, speed, keyboardCheck = true) }
            .withAxis(Direction.Axis.Y, player.input.yAxisMovement * speed)

        updatePosition(velocity, lastPosBeforePos = true, increase = true)
    }

    val jumpHandler = handler<PlayerJumpEvent> {
        it.cancelEvent()
    }

    val moveHandler = handler<PlayerMoveEvent> {
        if (!freeze) {
            return@handler
        }

        it.movement.x = 0.0
        it.movement.y = 0.0
        it.movement.z = 0.0
    }

    fun applyPosition(entity: Entity, tickDelta: Float) {
        val player = mc.player ?: return
        val camera = mc.gameRenderer.camera ?: return

        if (!enabled || entity != player) {
            return
        }

        return camera.setPos(interpolatePosition(tickDelta, lastPos, pos))
    }

    fun cancelMovementInput(original: Float): Float {
        if (!enabled) {
            return original
        }

        return 0.0f
    }

    fun renderPlayerFromAllPerspectives(entity: LivingEntity): Boolean {
        val player = mc.player ?: return entity.isSleeping

        if (!enabled || entity != player) {
            return entity.isSleeping
        }

        return entity.isSleeping || !mc.gameRenderer.camera.isThirdPerson
    }

    fun modifyRaycast(original: Vec3d, entity: Entity, tickDelta: Float): Vec3d {
        val player = mc.player ?: return original

        if (!enabled || entity != player || !interactFromCamera) {
            return original
        }

        return interpolatePosition(tickDelta, lastPos, pos)
    }

    fun shouldRenderCrosshair(isFirstPerson: Boolean) = isFirstPerson && !(enabled && !renderCrosshair)

    fun shouldDisableHandRender() = enabled && !renderHand

    fun shouldDisableRotations() = enabled && disableRotations

    private fun updatePosition(newPos: Vec3d, lastPosBeforePos: Boolean, increase: Boolean) {
        if (lastPosBeforePos) {
            lastPos = pos
        }
        pos += if (increase) newPos else newPos - pos
        if (!lastPosBeforePos) {
            lastPos = pos
        }
    }

    override fun disable() {
        if(resetRotation){
            val rotation = ModuleRotations.displayRotations()
            player.yaw = rotation.yaw
            player.pitch = rotation.pitch
        }
    }

    private fun interpolatePosition(tickDelta: Float, lastPos: Vec3d, currPos: Vec3d): Vec3d {
        return Vec3d(
            lastPos.x + (currPos.x - lastPos.x) * tickDelta,
            lastPos.y + (currPos.y - lastPos.y) * tickDelta,
            lastPos.z + (currPos.z - lastPos.z) * tickDelta
        )
    }

}
