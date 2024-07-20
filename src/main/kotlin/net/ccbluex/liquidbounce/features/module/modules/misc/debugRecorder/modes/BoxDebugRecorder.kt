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
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.math.minus
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult

object BoxDebugRecorder : ModuleDebugRecorder.DebugRecorderMode("Box") {

    val repeatable = repeatable {
        val crosshairTarget = mc.crosshairTarget

        if (crosshairTarget?.type != HitResult.Type.ENTITY || crosshairTarget !is EntityHitResult) {
            return@repeatable
        }

        recordPacket(JsonObject().apply {
            world.entities.filter {
                it.shouldBeAttacked() && it.distanceTo(player) < 10.0f && crosshairTarget.entity.id == it.id
            }.minByOrNull {
                it.distanceTo(player)
            }?.let {
                val vector = it.box.center - crosshairTarget.pos
                add("vec", JsonObject().apply {
                    addProperty("x", vector.x)
                    addProperty("y", vector.y)
                    addProperty("z", vector.z)
                })
            }
        })
    }

}
