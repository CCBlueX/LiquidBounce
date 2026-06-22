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

package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.lastPos
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.io.toJsonObject
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.world.getEntitiesInCube
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

object AimDebugRecorder : ModuleDebugRecorder.DebugRecorderMode<JsonObject>("Aim") {

    private const val RANGE = 10.0

    val repeatable = handler<GameTickEvent> {
        val playerRotation = player.rotation
        val playerLastRotation = player.lastRotation

        val turnSpeed = playerLastRotation.rotationDeltaTo(playerRotation)

        val crosshairTarget = mc.hitResult

        recordPacket(JsonObject().apply {
            addProperty("health", player.health)
            addProperty("yaw", playerRotation.yaw)
            addProperty("pitch", playerRotation.pitch)
            addProperty("last_yaw", playerLastRotation.yaw)
            addProperty("last_pitch", playerLastRotation.pitch)
            addProperty("turn_speed_h", turnSpeed.deltaYaw)
            addProperty("turn_speed_v", turnSpeed.deltaPitch)

            add("velocity", player.deltaMovement.toJsonObject())

            world.getEntitiesInCube<LivingEntity>(player.position(), RANGE) {
                it.shouldBeAttacked() && it.distanceToSqr(player) < RANGE * RANGE
            }.minByOrNull {
                it.distanceTo(player)
            }?.let {
                val vector = it.position() - player.position()
                add("vec", vector.toJsonObject())
                val velocity = it.position() - it.lastPos
                add("velocity", velocity.toJsonObject())
                addProperty("distance", player.distanceTo(it))
                val rotation = Rotation.lookingAt(point = it.boundingBox.center, from = player.eyePosition)

                val delta = rotation.rotationDeltaTo(playerRotation)

                addProperty("diff_h", delta.deltaYaw)
                addProperty("diff_v", delta.deltaPitch)
                addProperty("yaw_target", rotation.yaw)
                addProperty("pitch_target", rotation.pitch)

                addProperty("crosshair",
                    if (crosshairTarget?.type == HitResult.Type.ENTITY && crosshairTarget is EntityHitResult) {
                        crosshairTarget.entity.id == it.id
                    } else {
                        false
                    }
                )
            }
        })
    }

}
