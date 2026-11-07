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

package net.ccbluex.liquidbounce.features.command.commands.client

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.bodyAsJson
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig.serializeAutoConfig
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.accountType
import net.ccbluex.liquidbounce.config.gson.util.mapToJsonArray
import net.ccbluex.liquidbounce.config.gson.util.toJsonArray
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.features.addon.AddonManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.usesViaFabricPlus
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.plus
import net.minecraft.ChatFormatting
import net.minecraft.SharedConstants
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Style
import okhttp3.FormBody
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.net.URI
import java.util.EnumSet
import kotlin.io.path.div

/**
 * Debug Command to collect information about the client
 * in order to help developers to fix bugs or help users
 * with their issues.
 *
 * This command will create a JSON file with all the information
 * and upload it together with the latest log to logs.liquidbounce.net.
 */
object CommandDebug : CommandRegistrar {

    private const val LOG_API = "https://logs.liquidbounce.net/api/1/log"
    private const val MAX_LOG_BYTES = 10L * 1024 * 1024
    private const val MAX_LOG_LINES = 25000

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    override fun register(dispatcher: com.mojang.brigadier.CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("debug") {
            execSuspend {
                chat("Collecting debug information...".asPlainText(ChatFormatting.GRAY))

                val buffer = okio.Buffer()

                buffer.outputStream().writer().use {
                    serializeAutoConfig(it)
                }
                val autoConfig = buffer.readUtf8()
                val autoConfigPaste = uploadLog(autoConfig)
                buffer.clear()

                val logPaste = withContext(Dispatchers.IO) { readLatestLog() }?.let { uploadLog(it) }

                val debugJson = createDebugJson(autoConfigPaste, logPaste)
                buffer.outputStream().writer().use {
                    gson.toJson(debugJson, it)
                }
                val paste = uploadLog(buffer.readUtf8())
                buffer.clear()

                chat(
                    "Debug information has been uploaded to: ".asPlainText(ChatFormatting.GREEN),
                    paste.asPlainText(Style.EMPTY + ChatFormatting.YELLOW + ClickEvent.OpenUrl(URI(paste))),
                )

                if (logPaste != null) {
                    chat(
                        "Latest log has been uploaded to: ".asPlainText(ChatFormatting.GREEN),
                        logPaste.asPlainText(Style.EMPTY + ChatFormatting.YELLOW + ClickEvent.OpenUrl(URI(logPaste))),
                    )
                }
            }
        }
    }

    @Suppress("LongMethod")
    private fun createDebugJson(
        autoConfigPaste: String,
        logPaste: String?
    ) = JsonObject().apply {
        add("client", JsonObject().apply {
            addProperty("name", LiquidBounce.CLIENT_NAME)
            addProperty("version", LiquidBounce.clientVersion)
            addProperty("commit", LiquidBounce.clientCommit)
            addProperty("branch", LiquidBounce.clientBranch)
            addProperty("development", LiquidBounce.IN_DEVELOPMENT)
            addProperty("usesViaFabricPlus", usesViaFabricPlus)
        })

        add("minecraft", JsonObject().apply {
            addProperty("version", SharedConstants.getCurrentVersion().name())
            addProperty("protocol", SharedConstants.getProtocolVersion())
        })

        add("java", JsonObject().apply {
            addProperty("version", System.getProperty("java.version"))
            addProperty("vendor", System.getProperty("java.vendor"))
        })

        add("os", JsonObject().apply {
            addProperty("name", System.getProperty("os.name"))
            addProperty("version", System.getProperty("os.version"))
            addProperty("architecture", System.getProperty("os.arch"))
        })

        add("user", JsonObject().apply {
            addProperty("language", System.getProperty("user.language"))
            addProperty("country", System.getProperty("user.country"))
            addProperty("timezone", System.getProperty("user.timezone"))
        })

        add("profile", JsonObject().apply {
            addProperty("name", mc.user.name)
            addProperty("uuid", mc.user.profileId.toString())
            addProperty("type", mc.user.accountType)
        })

        add("language", JsonObject().apply {
            addProperty("language", mc.languageManager.selected)
            addProperty("clientLanguage", LanguageManager.clientLanguage.tag)
        })

        add("server", JsonObject().apply {
            mc.currentServer?.let {
                addProperty("name", it.name)
                addProperty("address", it.ip)
                addProperty("protocol", it.protocol)
            }
        })

        addProperty("config", autoConfigPaste)
        addProperty("log", logPaste)

        add("activeModules", ModuleManager.filter { it.running }.mapToJsonArray { module ->
            JsonPrimitive(module.name)
        })

        add("addons", AddonManager.addons.mapToJsonArray { addon ->
            JsonObject().apply {
                addProperty("id", addon.id)
                addProperty("name", addon.displayName)
                addProperty("version", addon.version)
                add("authors", addon.authors.toJsonArray())
                addProperty("state", addon.state.name)
                addProperty("modules", addon.registeredModules.size)
            }
        })

        add("enemies", publicGson.toJsonTree(GlobalSettingsTarget.combat, EnumSet::class.javaObjectType))
    }

    /**
     * Reads the end of `logs/latest.log` that fits into the limits of the log API,
     * or returns null when there is no log file.
     */
    private fun readLatestLog(): String? {
        val path = (mc.gameDirectory.toPath() / "logs" / "latest.log").toOkioPath()
        val size = FileSystem.SYSTEM.metadataOrNull(path)?.size ?: return null
        val skipped = (size - MAX_LOG_BYTES).coerceAtLeast(0L)
        var content = FileSystem.SYSTEM.read(path) {
            skip(skipped)
            readUtf8(size - skipped)
        }

        if (skipped > 0) {
            // Drop the line that was cut off
            content = content.substringAfter('\n')
        }

        return content.lineSequence().toList().takeLast(MAX_LOG_LINES).joinToString("\n")
    }

    /**
     * Uploads the given content to the log API and returns the URL of the log.
     */
    private suspend fun uploadLog(content: String): String = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().add("content", content).build()
        val response = HttpClient.request(LOG_API, HttpMethod.POST, body = body).bodyAsJson<LogResponse>()
        check(response.success && response.url != null) { "Failed to upload log: ${response.error}" }
        response.url
    }

    private class LogResponse(val success: Boolean, val url: String?, val error: String?)

}
