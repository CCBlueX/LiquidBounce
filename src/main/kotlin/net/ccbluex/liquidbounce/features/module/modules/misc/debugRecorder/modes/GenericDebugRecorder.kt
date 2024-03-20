package net.ccbluex.liquidbounce.features.module.modules.misc.debugRecorder.modes

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.debugRecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.io.toJson
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import org.lwjgl.glfw.GLFW
import java.util.concurrent.CopyOnWriteArraySet

object GenericDebugRecorder : ModuleDebugRecorder.DebugRecorderMode("Generic") {

    data class ScheduledEntityDebug(var ticksLeft: Int, val entityId: Int)

    private val waitingEntites = CopyOnWriteArraySet<ScheduledEntityDebug>()

    fun debugEntityIn(entity: Entity, ticks: Int) {
        waitingEntites.add(ScheduledEntityDebug(ticks, entity.id))
    }

    val rep = repeatable {
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
