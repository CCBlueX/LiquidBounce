package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import org.lwjgl.glfw.GLFW

object DebugCPSRecorder : ModuleDebugRecorder.DebugRecorderMode("CPS") {

    val packetHandler = handler<PacketEvent> { event ->
        if (event.packet !is HandSwingC2SPacket)
            return@handler

        recordPacket(JsonObject().apply {
            addProperty("type", "swingPacket")
            addProperty("time", System.currentTimeMillis())
        })
    }
    val mouseHandler = handler<MouseButtonEvent> { event ->
        if (event.button == 0 && event.action == GLFW.GLFW_PRESS) {
            recordPacket(JsonObject().apply {
                addProperty("type", "mousePress")
                addProperty("time", System.currentTimeMillis())
            })
        }
    }
}
