/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2025 CCBlueX
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
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.models.client.AutoSettings
import net.ccbluex.liquidbounce.api.services.client.ClientApi
import net.ccbluex.liquidbounce.api.types.enums.AutoSettingsStatusType
import net.ccbluex.liquidbounce.api.types.enums.AutoSettingsType
import net.ccbluex.liquidbounce.authlib.utils.array
import net.ccbluex.liquidbounce.authlib.utils.int
import net.ccbluex.liquidbounce.authlib.utils.obj
import net.ccbluex.liquidbounce.authlib.utils.string
import net.ccbluex.liquidbounce.config.ConfigSystem.deserializeConfigurable
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.features.spoofer.SpooferManager
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.util.Formatting
import java.io.Reader
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.*

data class IncludeConfiguration(
    val includeBinds: Boolean = false,
    val includeAction: Boolean = false,
    val includeHidden: Boolean = false
) {
    companion object {
        val DEFAULT = IncludeConfiguration()
    }
}

object AutoConfig {

    @Volatile
    var loadingNow = false
        set(value) {
            field = value

            // After completion of loading, sync ClickGUI
            if (!value) {
                ModuleClickGui.reload()
            }
        }

    var includeConfiguration = IncludeConfiguration.DEFAULT

    @Volatile
    var configs: Array<AutoSettings>? = null
        private set

    /**
     * Reloads auto settings list.
     *
     * @return successfully reloaded or not
     */
    suspend fun reloadConfigs(): Boolean = try {
        configs = ClientApi.requestSettingsList()
        true
    } catch (e: Exception) {
        logger.error("Failed to load auto configs", e)
        false
    }

    inline fun withLoading(block: () -> Unit) {
        loadingNow = true
        try {
            block()
        } finally {
            loadingNow = false
        }
    }

    suspend fun loadAutoConfig(autoConfig: AutoSettings) = withLoading {
        ClientApi.requestSettingsScript(autoConfig.settingId).use(::loadAutoConfig)
    }

    /**
     * Deserialize module configurable from a reader
     */
    fun loadAutoConfig(
        reader: Reader,
        modules: Collection<Configurable> = emptyList()
    ) {
        JsonParser.parseReader(publicGson.newJsonReader(reader))?.let { jsonElement ->
            loadAutoConfig(jsonElement.asJsonObject, modules)
        }
    }

    /**
     * Handles the data from a configurable, which might be an auto config and therefore has data which
     * should be displayed to the user.
     *
     * @param jsonObject The JSON object of the configurable
     * @see ConfigSystem.deserializeConfigurable
     */
    fun loadAutoConfig(
        jsonObject: JsonObject,
        modules: Collection<Configurable> = emptyList()
    ) {
        chat(metadata = MessageMetadata(prefix = false))
        chat(regular("Auto Config").formatted(Formatting.LIGHT_PURPLE).bold(true))

        val name = jsonObject.string("name") ?: throw IllegalArgumentException("Auto Config has no name")
        when (name) {
            "autoconfig" -> {
                // Deserialize Module Configurable
                jsonObject.obj("modules")?.let { moduleObject ->
                    deserializeModuleConfigurable(moduleObject, modules)
                }

                // Deserialize Spoofer Configurable
                jsonObject.obj("spoofers")?.let { spooferObject ->
                    deserializeConfigurable(SpooferManager, spooferObject)
                }
            }
            "modules" -> deserializeModuleConfigurable(jsonObject, modules)
            else -> error("Unknown auto config type: $name")
        }

        // Auto Config
        printOutInformation(jsonObject)
    }

    /**
     * Print out information from the auto config
     */
    private fun printOutInformation(jsonObject: JsonObject) {
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
            formatAutoConfigProtocolInfo(pVersion, pName)
        }

        val date = jsonObject.string("date")
        val time = jsonObject.string("time")
        val author = jsonObject.string("author")
        val lbVersion = jsonObject.string("clientVersion")
        val lbCommit = jsonObject.string("clientCommit")

        if (date != null || time != null) {
            chat(
                regular("on "),
                variable(if (!date.isNullOrBlank()) "$date " else ""),
                variable(if (!time.isNullOrBlank()) time else "")
            )
        }

        if (author != null) {
            chat(
                regular("by "),
                variable(author)
            )
        }

        if (lbVersion != null) {
            chat(
                regular("with LiquidBounce "),
                variable(lbVersion),
                regular(" "),
                variable(lbCommit ?: "")
            )
        }

        jsonObject.array("chat")?.let { chatMessages ->
            for (messages in chatMessages) {
                chat(messages.asString)
            }
        }
    }

    private fun formatAutoConfigProtocolInfo(pVersion: Int, pName: String) {
        // Check if the protocol is identical
        val (protocolName, protocolVersion) = protocolVersion

        // Give user notification about the protocol of the config and his current protocol.
        // If they are not identical, make the message red and bold to make it more visible.
        // If the protocol is identical, make the message green to make it more visible
        val matchesVersion = protocolVersion == pVersion

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
                NotificationEvent.Severity.ERROR
            )

            if (usesViaFabricPlus) {
                if (inGame) {
                    chat(markAsError("Please reconnect to the server to apply the correct protocol."))
                } else {
                    selectProtocolVersion(pVersion)
                }
            } else {
                chat(markAsError("Please install ViaFabricPlus to apply the correct protocol."))
            }
        }
    }

    /**
     * Created an auto config, which stores the moduleConfigur
     */
    fun serializeAutoConfig(
        writer: Writer,
        includeConfiguration: IncludeConfiguration = IncludeConfiguration.DEFAULT,
        autoSettingsType: AutoSettingsType = AutoSettingsType.RAGE,
        statusType: AutoSettingsStatusType = AutoSettingsStatusType.BYPASSING
    ) {
        this.includeConfiguration = includeConfiguration

        // Store the config
        val moduleTree = ConfigSystem.serializeConfigurable(ModuleManager.modulesConfigurable, publicGson)
        val spooferTree = ConfigSystem.serializeConfigurable(SpooferManager, publicGson)

        if (!moduleTree.isJsonObject || !spooferTree.isJsonObject) {
            error("Root element is not a json object")
        }

        val jsonObject = JsonObject()
        jsonObject.addProperty("name", "autoconfig")

        jsonObject.add("modules", moduleTree.asJsonObject)
        jsonObject.add("spoofers", spooferTree.asJsonObject)

        val author = mc.session.username

        val now = Date()
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy")
        val timeFormatter = SimpleDateFormat("HH:mm:ss")
        val date = dateFormatter.format(now)
        val time = timeFormatter.format(now)

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

        jsonObject.add("type", publicGson.toJsonTree(autoSettingsType))
        jsonObject.add("status", publicGson.toJsonTree(statusType))

        publicGson.newJsonWriter(writer).use {
            publicGson.toJson(jsonObject, it)
        }

        this.includeConfiguration = IncludeConfiguration.DEFAULT
    }

    /**
     * Deserialize module configurable from a JSON object
     */
    private fun deserializeModuleConfigurable(
        jsonObject: JsonObject,
        modules: Collection<Configurable> = emptyList()
    ) {
        // Deserialize full module configurable
        if (modules.isEmpty()) {
            deserializeConfigurable(ModuleManager.modulesConfigurable, jsonObject)
            return
        }

        modules.forEach { module ->
            val moduleConfigurable = ModuleManager.modulesConfigurable.inner.find { value ->
                value.name == module.name
            } as? Configurable ?: return@forEach

            val moduleElement = jsonObject.asJsonObject["value"].asJsonArray.find { jsonElement ->
                jsonElement.asJsonObject["name"].asString == module.name
            } ?: return@forEach

            deserializeConfigurable(moduleConfigurable, moduleElement)
        }
    }

}
