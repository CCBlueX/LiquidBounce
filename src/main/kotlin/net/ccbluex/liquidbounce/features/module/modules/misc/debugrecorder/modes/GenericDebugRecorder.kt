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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.io.toJson
import net.minecraft.entity.Entity
import java.util.concurrent.CopyOnWriteArraySet

object GenericDebugRecorder : ModuleDebugRecorder.DebugRecorderMode<JsonObject>("Generic") {

    data class ScheduledEntityDebug(var ticksLeft: Int, val entityId: Int)

    private val waitingEntites = CopyOnWriteArraySet<ScheduledEntityDebug>()

    fun debugEntityIn(entity: Entity, ticks: Int) {
        waitingEntites.add(ScheduledEntityDebug(ticks, entity.id))
    }

    val repeatable = tickHandler {
        val due = waitingEntites.filter {
            it.ticksLeft--
            it.ticksLeft <= 0
        }

        for (scheduledEntityDebug in due) {
            val entity = world.getEntityById(scheduledEntityDebug.entityId)

            if (entity != null) {
                recordDebugInfo(ModuleDebugRecorder, "entity", debugObject(entity))
            }
        }

        waitingEntites.removeAll(due)
    }

    fun recordDebugInfo(module: ClientModule, packetName: String, packet: JsonElement) {
        recordPacket(JsonObject().apply {
            addProperty("module", module.name)
            addProperty("packet", packetName)
            addProperty("time", System.currentTimeMillis())
            add("data", packet)
        })
    }

    fun debugObject(entity: Entity): JsonElement {
        return JsonObject().apply {
            addProperty("id", entity.id)
            add("pos", entity.pos.toJson())
            add("velocity", entity.velocity.toJson())
        }
    }
}
