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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import org.lwjgl.glfw.GLFW

object DebugCPSRecorder : ModuleDebugRecorder.DebugRecorderMode<JsonObject>("CPS") {

    val packetHandler = handler<PacketEvent> { event ->
        if (event.packet !is HandSwingC2SPacket) {
            return@handler
        }

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
