/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

package net.ccbluex.liquidbounce.features.command.commands.client

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.IN_DEVELOPMENT
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.AutoConfig.serializeAutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.register.IncludeCommand
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.usesViaFabricPlus
import net.ccbluex.liquidbounce.utils.combat.combatTargetsConfigurable
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.minecraft.SharedConstants
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import java.io.StringWriter

/**
 * Debug Command to collect information about the client
 * in order to help developers to fix bugs or help users
 * with their issues.
 *
 * This command will create a JSON file with all the information
 * and send it to the CCBlueX Paste API.
 */
@IncludeCommand
object CommandDebug {

    fun createCommand() = CommandBuilder.begin("debug")
        .handler { _, args ->
            chat("§7Collecting debug information...")

            val autoConfig = StringWriter().use { writer ->
                serializeAutoConfig(writer)
                writer.toString()
            }
            val autoConfigPaste = uploadToPaste(autoConfig)
            val debugJson = createDebugJson(autoConfigPaste)

            val content = GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(debugJson)
            val paste = uploadToPaste(content)

            chat(Text.literal("Debug information has been uploaded to: ").styled { style ->
                style.withColor(TextColor.fromFormatting(Formatting.GREEN))
            }.append(Text.literal(paste).styled { style ->
                style.withColor(Formatting.YELLOW).withClickEvent(
                    ClickEvent(ClickEvent.Action.OPEN_URL, paste)
                )
            }))
        }
        .build()

    @Suppress("LongMethod")
    private fun createDebugJson(
        autoConfigPaste: String
    ) = JsonObject().apply {
        add("client", JsonObject().apply {
            addProperty("name", LiquidBounce.CLIENT_NAME)
            addProperty("version", LiquidBounce.clientVersion)
            addProperty("commit", LiquidBounce.clientCommit)
            addProperty("branch", LiquidBounce.clientBranch)
            addProperty("development", IN_DEVELOPMENT)
            addProperty("usesViaFabricPlus", usesViaFabricPlus)
        })

        add("minecraft", JsonObject().apply {
            addProperty("version", SharedConstants.getGameVersion().name)
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
            addProperty("name", mc.session.username)
            addProperty("uuid", mc.session.uuidOrNull.toString())
            addProperty("type", mc.session.accountType.toString())
        })

        add("language", JsonObject().apply {
            addProperty("language", mc.languageManager.language)
            addProperty("clientLanguage", LanguageManager.languageIdentifier)
        })

        add("server", JsonObject().apply {
            mc.currentServerEntry?.let {
                addProperty("name", it.name)
                addProperty("address", it.address)
                addProperty("protocol", it.protocolVersion)
            }
        })

        addProperty("config", autoConfigPaste)

        add("activeModules", JsonArray().apply {
            ModuleManager.filter { it.enabled }.forEach { module ->
                add(JsonPrimitive(module.name))
            }
        })

        add("scripts", JsonArray().apply {
            ScriptManager.loadedScripts.forEach { script ->
                add(JsonObject().apply {
                    addProperty("name", script.scriptName)
                    addProperty("version", script.scriptVersion)
                    addProperty("author", script.scriptAuthors.joinToString(", "))
                    addProperty("path", script.scriptFile.path)
                })
            }
        })

        add("enemies", ConfigSystem.serializeConfigurable(combatTargetsConfigurable,
            ConfigSystem.clientGson))
    }

    /**
     * Uploads the given content to the CCBlueX Paste API
     * and returns the URL of the paste.
     */
    private fun uploadToPaste(content: String): String {
        val form = "content=$content"
        return HttpClient.postForm("https://paste.ccbluex.net/api.php", form)
    }

}
