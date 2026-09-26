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
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.io.toJsonObject
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.world.getEntitiesInCube
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

object BoxDebugRecorder : ModuleDebugRecorder.DebugRecorderMode<JsonObject>("Box") {

    private const val RANGE = 10.0

    val repeatable = handler<GameTickEvent> {
        val crosshairTarget = mc.hitResult

        if (crosshairTarget?.type != HitResult.Type.ENTITY || crosshairTarget !is EntityHitResult) {
            return@handler
        }

        recordPacket(JsonObject().apply {
            world.getEntitiesInCube<LivingEntity>(player.position(), RANGE) {
                it.shouldBeAttacked() && it.distanceToSqr(player) < RANGE * RANGE
                    && crosshairTarget.entity.id == it.id
            }.minByOrNull {
                it.distanceToSqr(player)
            }?.let {
                val vector = it.boundingBox.center - crosshairTarget.location
                add("vec", vector.toJsonObject())
            }
        })
    }

}
