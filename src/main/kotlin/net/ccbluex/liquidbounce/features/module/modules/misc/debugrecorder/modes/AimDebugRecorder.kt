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
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.lastPos
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.math.minus
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

object AimDebugRecorder : ModuleDebugRecorder.DebugRecorderMode<JsonObject>("Aim") {

    val repeatable = tickHandler {
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

            add("velocity", JsonObject().apply {
                addProperty("x", player.deltaMovement.x)
                addProperty("y", player.deltaMovement.y)
                addProperty("z", player.deltaMovement.z)
            })

            world.entitiesForRendering().filter {
                it.shouldBeAttacked() && it.distanceTo(player) < 10.0f
            }.minByOrNull {
                it.distanceTo(player)
            }?.let {
                val vector = it.position() - player.position()
                add("vec", JsonObject().apply {
                    addProperty("x", vector.x)
                    addProperty("y", vector.y)
                    addProperty("z", vector.z)
                })
                val velocity = it.position() - it.lastPos
                add("velocity", JsonObject().apply {
                    addProperty("x", velocity.x)
                    addProperty("y", velocity.y)
                    addProperty("z", velocity.z)
                })
                addProperty("distance", player.distanceTo(it))
                val rotation = Rotation.lookingAt(point = it.box.center, from = player.eyePosition)

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
