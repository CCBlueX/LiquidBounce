/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.config

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.v1.AutoSettingsStatusType
import net.ccbluex.liquidbounce.api.v1.AutoSettingsType
import net.ccbluex.liquidbounce.authlib.utils.array
import net.ccbluex.liquidbounce.authlib.utils.int
import net.ccbluex.liquidbounce.authlib.utils.string
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.util.Formatting
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.*

object AutoConfig {

    /**
     * Handles the data from a configurable, which might be an auto config and therefore has data which
     * should be displayed to the user.
     *
     * @param jsonObject The json object of the configurable
     * @see ConfigSystem.deserializeConfigurable
     */
    fun handlePossibleAutoConfig(jsonObject: JsonObject) {
        // If the name is not modules, it cannot be an auto config
        if (jsonObject.string("name") != "modules") {
            return
        }

        chat(prefix = false)
        chat(regular("Auto Config").styled { it.withFormatting(Formatting.LIGHT_PURPLE).withBold(true) })

        // Auto Config
        val serverAddress = jsonObject.string("serverAddress")
        if (serverAddress != null) {
            chat(
                regular("for server "),
                variable(serverAddress)
            )
        }

        val pName = jsonObject.string("protocolName")
        val pVersion = jsonObject.int("protocolVersion")

        if (pName != null && pVersion != null) {
            // Check if protocol is identical
            val (protocolName, protocolVersion) = protocolVersion

            // Give user notification about the protocol of the config and his current protocol,
            // if they are not identical, make the message red and bold to make it more visible
            // also, if the protocol is identical, make the message green to make it more visible

            val matchesVersion = protocolName == pName && protocolVersion == pVersion

            chat(
                regular("for protocol "),
                variable("$pName $pVersion")
                    .styled {
                        if (!matchesVersion) {
                            it.withFormatting(Formatting.RED, Formatting.BOLD)
                        } else {
                            it.withFormatting(Formatting.GREEN)
                        }
                    },
                regular(" and your current protocol is "),
                variable("$protocolName $protocolVersion")
            )

            if (!matchesVersion) {
                notification(
                    "Auto Config",
                    "The auto config was made for protocol $pName, " +
                        "but your current protocol is $protocolName",
                    NotificationEvent.Severity.ERROR)
            }
        }

        val date = jsonObject.string("date")
        val time = jsonObject.string("time")
        val author = jsonObject.string("author")
        if (date != null || time != null) {
            chat(
                regular("on "),
                variable(if (!date.isNullOrBlank()) "$date $time " else ""),
                variable(if (!time.isNullOrBlank()) time else "")
            )
        }

        if (author != null) {
            chat(
                regular("by "),
                variable(author)
            )
        }

        jsonObject.array("chat")?.let { chatMessages ->
            for (messages in chatMessages) {
                chat(messages.asString)
            }
        }
    }

    /**
     * Created an auto config, which stores the moduleConfigur
     */
    fun serializeAutoConfig(
        writer: Writer,
        autoSettingsType: AutoSettingsType = AutoSettingsType.RAGE,
        statusType: AutoSettingsStatusType = AutoSettingsStatusType.BYPASSING
    ) {
        // Store the config
        val jsonTree =
            ConfigSystem.serializeConfigurable(ModuleManager.modulesConfigurable, ConfigSystem.autoConfigGson)

        if (!jsonTree.isJsonObject) {
            error("Root element is not a json object")
        }

        val jsonObject = jsonTree.asJsonObject

        val author = mc.session.username

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy")
        val timeFormatter = SimpleDateFormat("HH:mm:ss")
        val date = dateFormatter.format(Date())
        val time = timeFormatter.format(Date())

        val (protocolName, protocolVersion) = protocolVersion

        jsonObject.addProperty("author", author)
        jsonObject.addProperty("date", date)
        jsonObject.addProperty("time", time)
        jsonObject.addProperty("clientVersion", LiquidBounce.clientVersion)
        jsonObject.addProperty("clientCommit", LiquidBounce.clientCommit)
        mc.currentServerEntry?.let {
            jsonObject.addProperty("serverAddress", it.address.dropPort().rootDomain())
        }
        jsonObject.addProperty("protocolName", protocolName)
        jsonObject.addProperty("protocolVersion", protocolVersion)

        jsonObject.add("type",
            ConfigSystem.autoConfigGson.toJsonTree(autoSettingsType))
        jsonObject.add("status",
            ConfigSystem.autoConfigGson.toJsonTree(statusType))

        ConfigSystem.autoConfigGson.newJsonWriter(writer).use {
            ConfigSystem.autoConfigGson.toJson(jsonObject, it)
        }
    }

}
