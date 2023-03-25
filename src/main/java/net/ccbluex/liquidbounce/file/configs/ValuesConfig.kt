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
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat.Companion.jwtToken
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
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.altsLength
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.enabledClientTitle
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.enabledCustomBackground
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.particles
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.stylisedAlts
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.unformattedAlts
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiDonatorCape.Companion.capeEnabled
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiDonatorCape.Companion.transferCode
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.altgenerator.GuiTheAltening.Companion.apiKey
import net.ccbluex.liquidbounce.utils.EntityUtils
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
            if (key.equals("CommandPrefix", ignoreCase = true)) {
                commandManager.prefix = value.asCharacter
            } else if (key.equals("ShowRichPresence", ignoreCase = true)) {
                clientRichPresence.showRichPresenceValue = value.asBoolean
            } else if (key.equals("targets", ignoreCase = true)) {
                val jsonValue = value as JsonObject
                if (jsonValue.has("TargetPlayer")) EntityUtils.targetPlayer = jsonValue["TargetPlayer"].asBoolean
                if (jsonValue.has("TargetMobs")) EntityUtils.targetMobs = jsonValue["TargetMobs"].asBoolean
                if (jsonValue.has("TargetAnimals")) EntityUtils.targetAnimals = jsonValue["TargetAnimals"].asBoolean
                if (jsonValue.has("TargetInvisible")) EntityUtils.targetInvisible =
                    jsonValue["TargetInvisible"].asBoolean
                if (jsonValue.has("TargetDead")) EntityUtils.targetDead = jsonValue["TargetDead"].asBoolean
            } else if (key.equals("features", ignoreCase = true)) {
                val jsonValue = value as JsonObject
                if (jsonValue.has("AntiForge")) fmlFixesEnabled = jsonValue["AntiForge"].asBoolean
                if (jsonValue.has("AntiForgeFML")) blockFML = jsonValue["AntiForgeFML"].asBoolean
                if (jsonValue.has("AntiForgeProxy")) blockProxyPacket =
                    jsonValue["AntiForgeProxy"].asBoolean
                if (jsonValue.has("AntiForgePayloads")) blockPayloadPackets =
                    jsonValue["AntiForgePayloads"].asBoolean
                if (jsonValue.has("FixResourcePackExploit")) blockResourcePackExploit =
                    jsonValue["FixResourcePackExploit"].asBoolean
                if (jsonValue.has("ClientBrand")) clientBrand = jsonValue["ClientBrand"].asString
                if (jsonValue.has("BungeeSpoof")) BungeeCordSpoof.enabled = jsonValue["BungeeSpoof"].asBoolean
                if (jsonValue.has("AutoReconnectDelay")) delay = jsonValue["AutoReconnectDelay"].asInt
            } else if (key.equals("thealtening", ignoreCase = true)) {
                val jsonValue = value as JsonObject
                if (jsonValue.has("API-Key")) apiKey = jsonValue["API-Key"].asString
            } else if (key.equals("liquidchat", ignoreCase = true)) {
                val jsonValue = value as JsonObject
                if (jsonValue.has("token")) jwtToken = jsonValue["token"].asString
            } else if (key.equals("DonatorCape", ignoreCase = true)) {
                val jsonValue = value as JsonObject
                if (jsonValue.has("TransferCode")) transferCode = jsonValue["TransferCode"].asString
                if (jsonValue.has("CapeEnabled")) capeEnabled = jsonValue["CapeEnabled"].asBoolean
            } else if (key.equals("clientConfiguration", ignoreCase = true)) {
                val jsonValue = value as JsonObject
                if (jsonValue.has("EnabledClientTitle")) enabledClientTitle = jsonValue["EnabledClientTitle"].asBoolean
                if (jsonValue.has("EnabledBackground")) enabledCustomBackground =
                    jsonValue["EnabledBackground"].asBoolean
                if (jsonValue.has("Particles")) particles = jsonValue["Particles"].asBoolean
                if (jsonValue.has("StylisedAlts")) stylisedAlts = jsonValue["StylisedAlts"].asBoolean
                if (jsonValue.has("AltsLength")) altsLength = jsonValue["AltsLength"].asInt
                if (jsonValue.has("CleanAlts")) unformattedAlts = jsonValue["CleanAlts"].asBoolean
            } else if (key.equals("Background", ignoreCase = true)) { // Compatibility with old versions
                val jsonValue = value as JsonObject
                if (jsonValue.has("Enabled")) enabledCustomBackground = jsonValue["Enabled"].asBoolean
                if (jsonValue.has("Particles")) particles = jsonValue["Particles"].asBoolean
            } else {
                val module = moduleManager[key] ?: continue

                val jsonModule = value as JsonObject
                for (moduleValue in module.values) {
                    val element = jsonModule[moduleValue.name]
                    if (element != null) moduleValue.fromJson(element)
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
        jsonObject.addProperty("CommandPrefix", commandManager.prefix)
        jsonObject.addProperty("ShowRichPresence", clientRichPresence.showRichPresenceValue)

        val jsonTargets = JsonObject()
        jsonTargets.addProperty("TargetPlayer", EntityUtils.targetPlayer)
        jsonTargets.addProperty("TargetMobs", EntityUtils.targetMobs)
        jsonTargets.addProperty("TargetAnimals", EntityUtils.targetAnimals)
        jsonTargets.addProperty("TargetInvisible", EntityUtils.targetInvisible)
        jsonTargets.addProperty("TargetDead", EntityUtils.targetDead)

        jsonObject.add("targets", jsonTargets)
        val jsonFeatures = JsonObject()
        jsonFeatures.addProperty("AntiForge", fmlFixesEnabled)
        jsonFeatures.addProperty("AntiForgeFML", blockFML)
        jsonFeatures.addProperty("AntiForgeProxy", blockProxyPacket)
        jsonFeatures.addProperty("AntiForgePayloads", blockPayloadPackets)
        jsonFeatures.addProperty("FixResourcePackExploit", blockResourcePackExploit)
        jsonFeatures.addProperty("ClientBrand", clientBrand)
        jsonFeatures.addProperty("BungeeSpoof", BungeeCordSpoof.enabled)
        jsonFeatures.addProperty("AutoReconnectDelay", delay)
        jsonObject.add("features", jsonFeatures)

        val theAlteningObject = JsonObject()
        theAlteningObject.addProperty("API-Key", apiKey)
        jsonObject.add("thealtening", theAlteningObject)

        val liquidChatObject = JsonObject()
        liquidChatObject.addProperty("token", jwtToken)
        jsonObject.add("liquidchat", liquidChatObject)

        val capeObject = JsonObject()
        capeObject.addProperty("TransferCode", transferCode)
        capeObject.addProperty("CapeEnabled", capeEnabled)
        jsonObject.add("DonatorCape", capeObject)

        val clientObject = JsonObject()
        clientObject.addProperty("EnabledClientTitle", enabledClientTitle)
        clientObject.addProperty("EnabledBackground", enabledCustomBackground)
        clientObject.addProperty("Particles", particles)
        clientObject.addProperty("StylisedAlts", stylisedAlts)
        clientObject.addProperty("AltsLength", altsLength)
        clientObject.addProperty("CleanAlts", unformattedAlts)
        jsonObject.add("clientConfiguration", clientObject)

        for (module in moduleManager.modules) {
            if (module.values.isEmpty()) continue

            val jsonModule = JsonObject()
            for (value in module.values) jsonModule.add(value.name, value.toJson())
            jsonObject.add(module.name, jsonModule)
        }

        val printWriter = PrintWriter(FileWriter(file))
        printWriter.println(PRETTY_GSON.toJson(jsonObject))
        printWriter.close()
    }
}