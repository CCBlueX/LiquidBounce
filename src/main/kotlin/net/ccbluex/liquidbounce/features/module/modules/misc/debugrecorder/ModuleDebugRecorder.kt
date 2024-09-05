package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder

import com.google.gson.Gson
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.AimDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.BoxDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCPSRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.GenericDebugRecorder
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import java.nio.charset.Charset
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*

//@RegisterModule(dev = true)
object ModuleDebugRecorder : Module("DebugRecorder", Category.MISC) {

    val modes = choices("Mode", GenericDebugRecorder, arrayOf(
        GenericDebugRecorder,
        DebugCPSRecorder,
        AimDebugRecorder,
        BoxDebugRecorder
    ))

    abstract class DebugRecorderMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        private val packets = mutableListOf<Any>()

        protected fun recordPacket(packet: Any) {
            if (!this.isActive) {
                return
            }

            packets.add(packet)
        }

        override fun enable() {
            this.packets.clear()
            chat(regular("Recording "), variable(name), regular("..."))
        }

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

        override fun disable() {
            if (this.packets.isEmpty()) {
                chat(regular("No packets recorded."))

                return
            }

            runCatching {
                val baseName = dateFormat.format(Date())
                val folder = ConfigSystem.rootFolder.resolve("debugRecorder/$name")

                folder.mkdirs()

                var file = folder.resolve("${baseName}.json")

                var idx = 0

                while (file.exists()) {
                    file = folder.resolve("${baseName}_${idx++}.json")
                }

                Files.write(file.toPath(), Gson().toJson(this.packets).toByteArray(Charset.forName("UTF-8")))

                file.absolutePath
            }.onFailure {
                chat(markAsError("Failed to write log to file $it".asText()))
            }.onSuccess { path ->
                val text = path.asText().styled {
                    it.withUnderline(true)
                        .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, regular("Browse...")))
                        .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_FILE, path.toString()))
                }

                chat(regular("Log was written to "), text, regular("."))
            }

            this.packets.clear()
        }
    }

}
