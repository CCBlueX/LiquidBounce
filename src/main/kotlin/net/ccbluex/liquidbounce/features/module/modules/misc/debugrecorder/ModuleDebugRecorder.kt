package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.*
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import java.text.SimpleDateFormat
import java.util.*

object ModuleDebugRecorder : ClientModule("DebugRecorder", Category.MISC, disableOnQuit = true) {

    init {
        // [Debug Recorder] is usually used by developers and testers and is not needed in the auto config.
        doNotIncludeAlways()
    }

    val modes = choices("Mode", GenericDebugRecorder, arrayOf(
        MinaraiCombatRecorder,
        MinaraiTrainer,

        GenericDebugRecorder,
        DebugCPSRecorder,
        AimDebugRecorder,
        BoxDebugRecorder
    ))

    abstract class DebugRecorderMode<T>(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
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

        internal val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

        override fun disable() {
            if (this.packets.isEmpty()) {
                chat(regular("No packets recorded."))
                return
            }

            runCatching {
                // Create parent folder
                folder.mkdirs()

                val baseName = dateFormat.format(Date())
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
                    .onHover(HoverEvent(HoverEvent.Action.SHOW_TEXT, regular("Browse...")))
                    .onClick(ClickEvent(ClickEvent.Action.OPEN_FILE, path.toString()))

                chat(regular("Log was written to "), text, regular("."))
            }

            this.packets.clear()
        }
    }
}
