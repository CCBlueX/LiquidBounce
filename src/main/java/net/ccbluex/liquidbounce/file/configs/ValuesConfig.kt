/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce.clientRichPresence
import net.ccbluex.liquidbounce.LiquidBounce.commandManager
import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.cape.CapeService
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat.jwtToken
import net.ccbluex.liquidbounce.features.special.AutoReconnect.delay
import net.ccbluex.liquidbounce.features.special.BungeeCordSpoof
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockFML
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockPayloadPackets
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockProxyPacket
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockResourcePackExploit
import net.ccbluex.liquidbounce.features.special.ClientFixes.clientBrand
import net.ccbluex.liquidbounce.features.special.ClientFixes.fmlFixesEnabled
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import net.ccbluex.liquidbounce.lang.LanguageManager.overrideLanguage
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.altsLength
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.enabledClientTitle
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.enabledCustomBackground
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.particles
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.stylisedAlts
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.unformattedAlts
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.altgenerator.GuiTheAltening.Companion.apiKey
import net.ccbluex.liquidbounce.utils.EntityUtils.targetAnimals
import net.ccbluex.liquidbounce.utils.EntityUtils.targetDead
import net.ccbluex.liquidbounce.utils.EntityUtils.targetInvisible
import net.ccbluex.liquidbounce.utils.EntityUtils.targetMobs
import net.ccbluex.liquidbounce.utils.EntityUtils.targetPlayer
import java.io.*

class ValuesConfig(file: File) : FileConfig(file) {

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        val jsonElement = JsonParser().parse(file.bufferedReader())
        if (jsonElement is JsonNull) return

        val jsonObject = jsonElement as JsonObject
        for ((key, value) in jsonObject.entrySet()) {
            when {
                key.equals("commandprefix", true) ->
                    commandManager.prefix = value.asCharacter

                key.equals("discordRPC", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("ShowRichPresence")) clientRichPresence.showRPCValue = jsonValue["ShowRichPresence"].asBoolean
                    if (jsonValue.has("ShowRichPresenceServerIP")) clientRichPresence.showRPCServerIP = jsonValue["ShowRichPresenceServerIP"].asBoolean
                    if (jsonValue.has("RichPresenceCustomText")) clientRichPresence.customRPCText = jsonValue["RichPresenceCustomText"].asString
                    if (jsonValue.has("ShowRichPresenceModulesCount")) clientRichPresence.showRPCModulesCount = jsonValue["ShowRichPresenceModulesCount"].asBoolean
                }
                key.equals("targets", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("TargetPlayer")) targetPlayer = jsonValue["TargetPlayer"].asBoolean
                    if (jsonValue.has("TargetMobs")) targetMobs = jsonValue["TargetMobs"].asBoolean
                    if (jsonValue.has("TargetAnimals")) targetAnimals = jsonValue["TargetAnimals"].asBoolean
                    if (jsonValue.has("TargetInvisible")) targetInvisible = jsonValue["TargetInvisible"].asBoolean
                    if (jsonValue.has("TargetDead")) targetDead = jsonValue["TargetDead"].asBoolean
                }
                key.equals("features", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("AntiForge")) fmlFixesEnabled = jsonValue["AntiForge"].asBoolean
                    if (jsonValue.has("AntiForgeFML")) blockFML = jsonValue["AntiForgeFML"].asBoolean
                    if (jsonValue.has("AntiForgeProxy")) blockProxyPacket = jsonValue["AntiForgeProxy"].asBoolean
                    if (jsonValue.has("AntiForgePayloads")) blockPayloadPackets = jsonValue["AntiForgePayloads"].asBoolean
                    if (jsonValue.has("FixResourcePackExploit")) blockResourcePackExploit = jsonValue["FixResourcePackExploit"].asBoolean
                    if (jsonValue.has("ClientBrand")) clientBrand = jsonValue["ClientBrand"].asString
                    if (jsonValue.has("BungeeSpoof")) BungeeCordSpoof.enabled = jsonValue["BungeeSpoof"].asBoolean
                    if (jsonValue.has("AutoReconnectDelay")) delay = jsonValue["AutoReconnectDelay"].asInt
                }
                key.equals("thealtening", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("API-Key")) apiKey = jsonValue["API-Key"].asString
                }
                key.equals("liquidchat", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("token")) jwtToken = jsonValue["token"].asString
                }
                key.equals("DonatorCape", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("TransferCode")) {
                        CapeService.knownToken = jsonValue["TransferCode"].asString
                    }
                }
                key.equals("clientConfiguration", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("EnabledClientTitle")) enabledClientTitle = jsonValue["EnabledClientTitle"].asBoolean
                    if (jsonValue.has("EnabledBackground")) enabledCustomBackground = jsonValue["EnabledBackground"].asBoolean
                    if (jsonValue.has("Particles")) particles = jsonValue["Particles"].asBoolean
                    if (jsonValue.has("StylisedAlts")) stylisedAlts = jsonValue["StylisedAlts"].asBoolean
                    if (jsonValue.has("AltsLength")) altsLength = jsonValue["AltsLength"].asInt
                    if (jsonValue.has("CleanAlts")) unformattedAlts = jsonValue["CleanAlts"].asBoolean
                    if (jsonValue.has("OverrideLanguage")) overrideLanguage = jsonValue["OverrideLanguage"].asString
                }
                key.equals("background", true) -> { // Compatibility with old versions
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("Enabled")) enabledCustomBackground = jsonValue["Enabled"].asBoolean
                    if (jsonValue.has("Particles")) particles = jsonValue["Particles"].asBoolean
                }
                else -> {
                    val module = moduleManager[key] ?: continue

                    val jsonModule = value as JsonObject
                    for (moduleValue in module.values) {
                        val element = jsonModule[moduleValue.name]
                        if (element != null) moduleValue.fromJson(element)
                    }
                }
            }
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig() {
        val jsonObject = JsonObject()
        jsonObject.run {
            addProperty("CommandPrefix", commandManager.prefix)
        }

        val jsonDiscordRPC = JsonObject()
        jsonDiscordRPC.run {
            addProperty("ShowRichPresence", clientRichPresence.showRPCValue)
            addProperty("ShowRichPresenceServerIP", clientRichPresence.showRPCServerIP)
            addProperty("RichPresenceCustomText", clientRichPresence.customRPCText)
            addProperty("ShowRichPresenceModulesCount", clientRichPresence.showRPCModulesCount)
        }
        jsonObject.add("discordRPC", jsonDiscordRPC)

        val jsonTargets = JsonObject()
        jsonTargets.run {
            addProperty("TargetPlayer", targetPlayer)
            addProperty("TargetMobs", targetMobs)
            addProperty("TargetAnimals", targetAnimals)
            addProperty("TargetInvisible", targetInvisible)
            addProperty("TargetDead", targetDead)
        }

        jsonObject.add("targets", jsonTargets)
        val jsonFeatures = JsonObject()
        jsonFeatures.run {
            addProperty("AntiForge", fmlFixesEnabled)
            addProperty("AntiForgeFML", blockFML)
            addProperty("AntiForgeProxy", blockProxyPacket)
            addProperty("AntiForgePayloads", blockPayloadPackets)
            addProperty("FixResourcePackExploit", blockResourcePackExploit)
            addProperty("ClientBrand", clientBrand)
            addProperty("BungeeSpoof", BungeeCordSpoof.enabled)
            addProperty("AutoReconnectDelay", delay)
        }
        jsonObject.add("features", jsonFeatures)

        val theAlteningObject = JsonObject()
        theAlteningObject.addProperty("API-Key", apiKey)
        jsonObject.add("thealtening", theAlteningObject)

        val liquidChatObject = JsonObject()
        liquidChatObject.addProperty("token", jwtToken)
        jsonObject.add("liquidchat", liquidChatObject)

        val capeObject = JsonObject()
        capeObject.addProperty("TransferCode", CapeService.knownToken)
        jsonObject.add("DonatorCape", capeObject)

        val clientObject = JsonObject()
        clientObject.run {
            addProperty("EnabledClientTitle", enabledClientTitle)
            addProperty("EnabledBackground", enabledCustomBackground)
            addProperty("Particles", particles)
            addProperty("StylisedAlts", stylisedAlts)
            addProperty("AltsLength", altsLength)
            addProperty("CleanAlts", unformattedAlts)
            addProperty("OverrideLanguage", overrideLanguage)
        }
        jsonObject.add("clientConfiguration", clientObject)

        for (module in moduleManager.modules) {
            if (module.values.isEmpty()) continue

            val jsonModule = JsonObject()
            for (value in module.values) jsonModule.add(value.name, value.toJson())
            jsonObject.add(module.name, jsonModule)
        }

        file.writeText(PRETTY_GSON.toJson(jsonObject))
    }
}