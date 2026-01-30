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
package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.adapter.toUnderlinedString
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.AimDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.BoxDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCPSRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCombatRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCombatTrainerRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.GenericDebugRecorder
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.underline
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
import java.time.LocalDateTime

object ModuleDebugRecorder : ClientModule("DebugRecorder", ModuleCategories.MISC, disableOnQuit = true) {

    init {
        // [Debug Recorder] is usually used by developers and testers and is not needed in the auto config.
        doNotIncludeAlways()
    }

    val modes = choices("Mode", GenericDebugRecorder, arrayOf(
        DebugCombatRecorder,
        DebugCombatTrainerRecorder,

        GenericDebugRecorder,
        DebugCPSRecorder,
        AimDebugRecorder,
        BoxDebugRecorder
    ))

    abstract class DebugRecorderMode<T>(name: String) : Mode(name) {
        override val parent: ModeValueGroup<*>
            get() = modes

        val folder = ConfigSystem.rootFolder.resolve("debug-recorder/$name").apply {
            mkdirs()
        }
        internal val packets = mutableListOf<T>()

        protected fun recordPacket(packet: T) {
            if (!this.isSelected) {
                return
            }

            packets.add(packet)
        }

        override fun enable() {
            this.packets.clear()
            chat(regular("Recording "), variable(name), regular("..."))
        }

        override fun disable() {
            if (this.packets.isEmpty()) {
                chat(regular("No packets recorded."))
                return
            }

            runCatching {
                // Create parent folder
                folder.mkdirs()

                val baseName = LocalDateTime.now().toUnderlinedString()
                var file = folder.resolve("${baseName}.json")

                var idx = 0
                while (file.exists()) {
                    file = folder.resolve("${baseName}_${idx++}.json")
                }

                file.bufferedWriter().use { writer ->
                    publicGson.toJson(this.packets, writer)
                }
                file.absolutePath
            }.onFailure {
                chat(markAsError("Failed to write log to file $it".asText()))
            }.onSuccess { path ->
                val text = path.asText()
                    .underline(true)
                    .onHover(HoverEvent.ShowText(regular("Browse...")))
                    .onClick(ClickEvent.OpenFile(path))

                chat(regular("Log was written to "), text, regular("."))
            }

            this.packets.clear()
        }
    }
}
