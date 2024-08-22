package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.io.toJson
import net.minecraft.entity.Entity
import java.util.concurrent.CopyOnWriteArraySet

object GenericDebugRecorder : ModuleDebugRecorder.DebugRecorderMode("Generic") {

    data class ScheduledEntityDebug(var ticksLeft: Int, val entityId: Int)

    private val waitingEntites = CopyOnWriteArraySet<ScheduledEntityDebug>()

    fun debugEntityIn(entity: Entity, ticks: Int) {
        waitingEntites.add(ScheduledEntityDebug(ticks, entity.id))
    }

    val repeatable = repeatable {
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

    fun recordDebugInfo(module: Module, packetName: String, packet: JsonElement) {
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
