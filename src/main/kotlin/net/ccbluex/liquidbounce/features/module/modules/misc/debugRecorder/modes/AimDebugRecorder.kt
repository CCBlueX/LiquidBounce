/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.misc.debugRecorder.modes

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.misc.debugRecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult

object AimDebugRecorder : ModuleDebugRecorder.DebugRecorderMode("Aim") {

    val repeatable = repeatable {
        val playerRotation = player.rotation
        val playerLastRotation = player.lastRotation

        val turnSpeedH = RotationManager.angleDifference(playerRotation.yaw, playerLastRotation.yaw)
        val turnSpeedV = RotationManager.angleDifference(playerRotation.pitch, playerLastRotation.pitch)
        val crosshairTarget = mc.crosshairTarget

        recordPacket(JsonObject().apply {
            addProperty("health", player.health)
            addProperty("yaw", playerRotation.yaw)
            addProperty("pitch", playerRotation.pitch)
            addProperty("last_yaw", playerLastRotation.yaw)
            addProperty("last_pitch", playerLastRotation.pitch)
            addProperty("turn_speed_h", turnSpeedH)
            addProperty("turn_speed_v", turnSpeedV)

            world.entities.filter {
                it.shouldBeAttacked() && it.distanceTo(player) < 10.0f
            }.minByOrNull {
                it.distanceTo(player)
            }?.let {
                val vector = it.pos.subtract(player.pos)
                add("vec", JsonObject().apply {
                    addProperty("x", vector.x)
                    addProperty("y", vector.y)
                    addProperty("z", vector.z)
                })
                addProperty("distance", player.distanceTo(it))
                val rotation = RotationManager.makeRotation(it.box.center, player.eyes)

                val diffH = RotationManager.angleDifference(playerRotation.yaw, rotation.yaw)
                val diffV = RotationManager.angleDifference(playerRotation.pitch, rotation.pitch)

                addProperty("diff_h", diffH)
                addProperty("diff_v", diffV)
                addProperty("yaw_target", rotation.yaw)
                addProperty("pitch_target", rotation.pitch)

                addProperty("crosshair",
                    if (crosshairTarget?.type == HitResult.Type.ENTITY && crosshairTarget is EntityHitResult) {
                        crosshairTarget.entity.id == it.id
                    } else false)
            }
        })
    }

}
