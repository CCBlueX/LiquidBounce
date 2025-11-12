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
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.atomicfu.atomic
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.adapter.toUnderlinedString
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.isNotRoot
import net.ccbluex.liquidbounce.utils.kotlin.toFullString
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper
import net.minecraft.network.packet.Packet
import net.minecraft.text.MutableText
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import okio.appendingSink
import okio.buffer
import java.io.File
import java.lang.reflect.*
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Module PacketLogger
 *
 * Prints all packets and their fields.
 *
 * @author ccetl
 */
object ModulePacketLogger : ClientModule("PacketLogger", Category.MISC) {

    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val clientPackets by clientPackets("ClientPackets", sortedSetOf())
    private val serverPackets by serverPackets("ServerPackets", sortedSetOf())
    private val showFieldType by boolean("ShowFieldType", true)

    private val outputTarget by multiEnumChoice("OutputTarget", OutputTarget.CHAT, canBeNone = false).onChanged {
        if (OutputTarget.FILE in it) {
            createFileIfNeeded()
        }
    }

    private val outputDir = ConfigSystem.rootFolder.resolve("packet-logger").apply { mkdirs() }

    private val classNames = ConcurrentHashMap<Class<out Packet<*>>, String>()
    private val fieldNames = ConcurrentHashMap<Field, String>()

    init {
        // Do not include this module in the auto config, as this is for debugging purposes only.
        doNotIncludeAlways()
    }

    private val outputFile = atomic<File?>(null)

    private fun createFileIfNeeded() {
        outputFile.compareAndSet(
            expect = null,
            update = outputDir.resolve("${LocalDateTime.now().toUnderlinedString()}.csv"),
        )
    }

    override fun onEnabled() {
        createFileIfNeeded()
        super.onEnabled()
    }

    override fun onDisabled() {
        outputFile.value = null
        classNames.clear()
        fieldNames.clear()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        onPacket(event.origin, event.packet, event.isCancelled)
    }

    fun onPacket(origin: TransferOrigin, packet: Packet<*>, canceled: Boolean = false) {
        if (!running) {
            return
        }

        val packetId = packet.packetType.id
        if (!filter(packetId, if (origin == TransferOrigin.INCOMING) serverPackets else clientPackets)) {
            return
        }

        outputTarget.forEach {
            it.handle(origin, packet, canceled, packetId)
        }
    }

    private enum class OutputTarget(override val choiceName: String) : NamedChoice {
        CHAT("Chat") {
            override fun handle(origin: TransferOrigin, packet: Packet<*>, canceled: Boolean, packetId: Identifier) {
                val clazz = packet.javaClass

                val packetClassName = classNames.computeIfAbsent(clazz, EnvironmentRemapper::remapClass)
                    .substringAfterLast('.')

                val text = "".asText()
                if (origin == TransferOrigin.INCOMING) {
                    text.append(message("receive").formatted(Formatting.BLUE).bold(true))
                } else {
                    text.append(message("send").formatted(Formatting.GRAY).bold(true))
                }

                text.append(" ")

                text.append(highlight(packetClassName).copyable(copyContent = packetClassName))

                val packetName = packetId.toName()

                text.append(regular(" (ID: "))
                text.append(variable(packetName).copyable(copyContent = packetName))
                text.append(regular(")"))

                if (clazz.isRecord) {
                    text.append(" (Record)".asPlainText(Formatting.DARK_GRAY))
                }

                if (canceled) {
                    text.append(" (".asPlainText(Formatting.RED))
                    text.append(message("canceled").formatted(Formatting.RED))
                    text.append(")".asPlainText(Formatting.RED))
                }

                text.appendFields(clazz, packet)

                chat(text, metadata = MessageMetadata(prefix = false))
            }
        },

        FILE("File") {
            override fun handle(origin: TransferOrigin, packet: Packet<*>, canceled: Boolean, packetId: Identifier) {
                val file = outputFile.value ?: return

                val clazz = packet.javaClass

                val packetClassName = classNames.computeIfAbsent(clazz, EnvironmentRemapper::remapClass)
                    .substringAfterLast('.')

                file.appendingSink().buffer().use {
                    it.writeUtf8(System.currentTimeMillis().toString())
                        .writeByte(','.code)
                        .writeUtf8(origin.choiceName)
                        .writeByte(','.code)
                        .writeUtf8(packetClassName)
                        .writeByte(','.code)
                        .writeUtf8(packetId.toString())
                        .writeByte(','.code)
                        .writeUtf8(canceled.toString())
                        .writeByte(','.code)
                        .writeByte('"'.code)

                    collectFields(clazz, packet).forEach { (name, type, value) ->
                        it.writeUtf8(name)
                            .writeByte(':'.code)
                            .writeUtf8(type.toFullString())
                            .writeByte('='.code)
                            .writeUtf8(value.toString())
                            .writeByte(';'.code)
                    }

                    it.writeByte('"'.code).writeByte('\n'.code)
                }
            }
        };

        abstract fun handle(origin: TransferOrigin, packet: Packet<*>, canceled: Boolean, packetId: Identifier)
    }

    @JvmRecord
    private data class PacketField(val name: String, val type: Type, val value: Any?)

    private fun collectFields(clazz: Class<out Packet<*>>, packet: Packet<*>): List<PacketField> {
        val fields = mutableListOf<PacketField>()

        var currentClass: Class<*>? = clazz

        while (currentClass.isNotRoot()) {
            currentClass.declaredFields.forEach { field ->
                if (Modifier.isStatic(field.modifiers)) {
                    return@forEach
                }

                field.isAccessible = true

                val name = fieldNames.computeIfAbsent(field) {
                    EnvironmentRemapper.remapField(currentClass!!.name, field.name)
                }

                val value = try {
                    field.get(packet)?.toString()
                } catch (@Suppress("SwallowedException") _: IllegalAccessException) {
                    "null"
                }

                fields += PacketField(name, field.genericType, value)
            }

            currentClass = currentClass.superclass
        }

        return fields
    }

    private fun MutableText.appendFields(clazz: Class<out Packet<*>>, packet: Packet<*>) {
        val fieldTexts = collectFields(clazz, packet).mapToArray { (name, type, value) ->
            buildList {
                add("- ".asPlainText(Formatting.GRAY))
                add(name.asText().formatted(Formatting.AQUA).copyable(copyContent = name))
                if (showFieldType) {
                    add(": ".asPlainText(Formatting.GRAY))
                    val typeString = type.toFullString()
                    add(typeString.asText().formatted(Formatting.YELLOW).copyable(copyContent = typeString))
                }
                add(" = ".asPlainText(Formatting.GRAY))
                val valueString = value.toString()
                add(valueString.asText().formatted(Formatting.WHITE).copyable(copyContent = valueString))
            }.asText()
        }

        if (fieldTexts.isNotEmpty()) {
            append(":")
            fieldTexts.forEach {
                append(PlainText.NEW_LINE)
                append(it)
            }
        }
    }

    override val running: Boolean
        get() = !isDestructed && enabled

}
