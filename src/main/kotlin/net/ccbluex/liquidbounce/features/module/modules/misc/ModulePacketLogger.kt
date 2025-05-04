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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper
import net.minecraft.network.packet.Packet
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.max

/**
 * Module PacketLogger
 *
 * Prints all packets and their fields.
 *
 * @author ccetl
 */
object ModulePacketLogger : ClientModule("PacketLogger", Category.MISC) {

    private val bound by multiEnumChoice("Bound", PacketBound.SERVER)
    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val packets by textArray("Packets", mutableListOf())

    private val classNames = ConcurrentHashMap<Class<out Packet<*>>, String>()
    private val fieldNames = ConcurrentHashMap<Field, String>()

    init {
        // Do not include this module in the auto config, as this is for debugging purposes only.
        doNotIncludeAlways()
    }

    override fun disable() {
        classNames.clear()
        fieldNames.clear()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        onPacket(event.origin, event.packet, event.isCancelled)
    }

    fun onPacket(origin: TransferOrigin, packet: Packet<*>, canceled: Boolean = false) {
        if (!running || bound.none { it.origin == origin }) {
            return
        }

        val text = Text.empty().formatted(Formatting.WHITE)
        if (origin == TransferOrigin.RECEIVE) {
            text.append(message("receive"))
        } else {
            text.append(message("send"))
        }

        val clazz = packet::class.java

        text.append(" ")
        val packetName = getPacketName(clazz)
        if (!filter(packetName, packets)) {
            return
        }

        text.append(packetName)

        if (canceled) {
            text.append(" (".asText().formatted(Formatting.RED))
            text.append(message("canceled").formatted(Formatting.RED))
            text.append(")".asText().formatted(Formatting.RED))
        }

        text.appendFields(clazz, packet)

        chat(text, metadata = MessageMetadata(prefix = false))
    }

    private fun getPacketName(clazz: Class<out Packet<*>>): String {
        fun getClassName(clazz: Class<*>): CharSequence {
            val remapClassName = EnvironmentRemapper.remapClass(clazz)
            val lastDotIndex = remapClassName.lastIndexOf('.')
            val lastDollarIndex = remapClassName.lastIndexOf('$')
            return remapClassName.subSequence(max(lastDotIndex, lastDollarIndex) + 1, remapClassName.length)
        }

        return classNames.computeIfAbsent(clazz) {
            val classNames = ArrayDeque<CharSequence>()
            classNames.add(getClassName(clazz))

            var superclass: Class<*>? = clazz.superclass
            while (superclass.isNotRoot()) {
                classNames.addFirst(getClassName(superclass))
                superclass = superclass.superclass
            }

            classNames.joinToString(".")
        }
    }

    private fun MutableText.appendFields(clazz: Class<out Packet<*>>, packet: Packet<*>) {
        var start = true

        var currentClass: Class<*>? = clazz

        while (currentClass.isNotRoot()) {
            currentClass.declaredFields.forEach { field ->
                if (Modifier.isStatic(field.modifiers)) {
                    return@forEach
                }

                field.isAccessible = true

                if (start) {
                    append(":")
                    start = false
                }

                append("\n")

                val name = fieldNames.computeIfAbsent(field) {
                    EnvironmentRemapper.remapField(currentClass!!.name, field.name)
                }

                val value = try {
                    field.get(packet)?.toString()
                } catch (@Suppress("SwallowedException") _: IllegalAccessException) {
                    "null"
                }

                append("-$name: ".asText().formatted(Formatting.GRAY))
                append("$value".asText().formatted(Formatting.GRAY))
            }

            currentClass = currentClass.superclass
        }
    }

    @OptIn(ExperimentalContracts::class)
    fun Class<*>?.isNotRoot(): Boolean {
        contract {
            returns(true) implies (this@isNotRoot != null)
        }
        return !(this == null || this === java.lang.Record::class.java || this.superclass == null)
    }

    override val running: Boolean
        get() = !isDestructed && enabled

    @Suppress("unused")
    private enum class PacketBound(
        override val choiceName: String,
        val origin: TransferOrigin,
    ) : NamedChoice {
        CLIENT("Client", TransferOrigin.RECEIVE),
        SERVER("Server", TransferOrigin.SEND)
    }
}
