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
package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.atomicfu.atomic
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.adapter.toUnderlinedString
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.highlight
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.toName
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.isNotRoot
import net.ccbluex.liquidbounce.utils.kotlin.toFullString
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.Packet
import net.minecraft.resources.Identifier
import okio.appendingSink
import okio.buffer
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Module PacketLogger
 *
 * Prints all packets and their fields.
 *
 * @author ccetl
 */
object ModulePacketLogger : ClientModule("PacketLogger", ModuleCategories.MISC) {

    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val clientPackets by c2sPackets("C2SPackets", sortedSetOf())
    private val serverPackets by s2cPackets("S2CPackets", sortedSetOf())
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

        val packetId = packet.type().id
        if (!filter(packetId, if (origin == TransferOrigin.INCOMING) serverPackets else clientPackets)) {
            return
        }

        outputTarget.forEach {
            it.handle(origin, packet, canceled, packetId)
        }
    }

    private enum class OutputTarget(override val tag: String) : Tagged {
        CHAT("Chat") {
            override fun handle(origin: TransferOrigin, packet: Packet<*>, canceled: Boolean, packetId: Identifier) {
                val clazz = packet.javaClass

                val packetClassName = classNames.computeIfAbsent(clazz, EnvironmentRemapper::remapClass)
                    .substringAfterLast('.')

                val text = "".asText()
                if (origin == TransferOrigin.INCOMING) {
                    text.append(message("receive").withStyle(ChatFormatting.BLUE).bold(true))
                } else {
                    text.append(message("send").withStyle(ChatFormatting.GRAY).bold(true))
                }

                text.append(" ")

                text.append(highlight(packetClassName).copyable(copyContent = packetClassName))

                val packetName = packetId.toName()

                text.append(regular(" (ID: "))
                text.append(variable(packetName).copyable(copyContent = packetName))
                text.append(regular(")"))

                if (clazz.isRecord) {
                    text.append(" (Record)".asPlainText(ChatFormatting.DARK_GRAY))
                }

                if (canceled) {
                    text.append(" (".asPlainText(ChatFormatting.RED))
                    text.append(message("canceled").withStyle(ChatFormatting.RED))
                    text.append(")".asPlainText(ChatFormatting.RED))
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
                        .writeUtf8(origin.tag)
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

    private fun MutableComponent.appendFields(clazz: Class<out Packet<*>>, packet: Packet<*>) {
        val fieldTexts = collectFields(clazz, packet).mapToArray { (name, type, value) ->
            buildList {
                add("- ".asPlainText(ChatFormatting.GRAY))
                add(name.asText().withStyle(ChatFormatting.AQUA).copyable(copyContent = name))
                if (showFieldType) {
                    add(": ".asPlainText(ChatFormatting.GRAY))
                    val typeString = type.toFullString()
                    add(typeString.asText().withStyle(ChatFormatting.YELLOW).copyable(copyContent = typeString))
                }
                add(" = ".asPlainText(ChatFormatting.GRAY))
                val valueString = value.toString()
                add(valueString.asText().withStyle(ChatFormatting.WHITE).copyable(copyContent = valueString))
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
