/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat.Companion.jwtToken
import net.ccbluex.liquidbounce.features.special.AntiModDisable
import net.ccbluex.liquidbounce.features.special.AutoReconnect.delay
import net.ccbluex.liquidbounce.features.special.BungeeCordSpoof
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.GuiBackground.Companion.enabled
import net.ccbluex.liquidbounce.ui.client.GuiBackground.Companion.particles
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiDonatorCape.Companion.capeEnabled
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiDonatorCape.Companion.transferCode
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.altgenerator.GuiTheAltening.Companion.apiKey
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.value.AbstractValue
import java.io.File
import java.io.IOException
import java.util.*
import java.util.function.Consumer

/**
 * Constructor of config
 *
 * @param file
 * of config
 */
class ValuesConfig(file: File) : FileConfig(file)
{
    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig()
    {
        var backwardCompatibility = false

        val jsonElement = JsonParser().parse(file.bufferedReader())
        if (jsonElement is JsonNull) return
        val jsonObject = jsonElement as JsonObject
        for ((key, value) in jsonObject.entrySet()) when (key.lowercase(Locale.getDefault()))
        {
            "commandprefix" -> LiquidBounce.commandManager.prefix = value.asCharacter
            "showrichpresence" -> LiquidBounce.clientRichPresence.showRichPresenceValue = value.asBoolean

            "targets" ->
            {
                val jsonValue = value as JsonObject
                if (jsonValue.has("TargetPlayer")) EntityUtils.targetPlayer = jsonValue["TargetPlayer"].asBoolean
                if (jsonValue.has("TargetMobs")) EntityUtils.targetMobs = jsonValue["TargetMobs"].asBoolean
                if (jsonValue.has("TargetAnimals")) EntityUtils.targetAnimals = jsonValue["TargetAnimals"].asBoolean
                if (jsonValue.has("TargetInvisible")) EntityUtils.targetInvisible = jsonValue["TargetInvisible"].asBoolean
                if (jsonValue.has("TargetArmorStand")) EntityUtils.targetArmorStand = jsonValue["TargetArmorStand"].asBoolean
                if (jsonValue.has("TargetDead")) EntityUtils.targetDead = jsonValue["TargetDead"].asBoolean
            }

            "features" ->
            {
                val jsonValue = value as JsonObject
                if (jsonValue.has("AntiModDisable")) AntiModDisable.enabled = jsonValue["AntiModDisable"].asBoolean
                if (jsonValue.has("AntiModDisableBlockFML")) AntiModDisable.blockFMLPackets = jsonValue["AntiModDisableBlockFMLPackets"].asBoolean
                if (jsonValue.has("AntiModDisableBlockFMLProxyPackets")) AntiModDisable.blockFMLProxyPackets = jsonValue["AntiModDisableBlockFMLProxyPackets"].asBoolean
                if (jsonValue.has("AntiModDisableSpoofBrandPayloadPackets")) AntiModDisable.blockClientBrandRetrieverPackets = jsonValue["AntiModDisableSpoofBrandPayloadPackets"].asBoolean
                if (jsonValue.has("AntiModDisableBlockWDLPayloads")) AntiModDisable.blockWDLPayloads = jsonValue["AntiModDisableBlockWDLPayloads"].asBoolean
                if (jsonValue.has("AntiModDisableBlockBetterSprintingPayloads")) AntiModDisable.blockBetterSprintingPayloads = jsonValue["AntiModDisableBlockBetterSprintingPayloads"].asBoolean
                if (jsonValue.has("AntiModDisableBlock5zigPayloads")) AntiModDisable.block5zigsmodPayloads = jsonValue["AntiModDisableBlock5zigPayloads"].asBoolean
                if (jsonValue.has("AntiModDisableBlockPermsReplPayloads")) AntiModDisable.blockReplicatedPermissionsPayloads = jsonValue["AntiModDisableBlockPermsReplPayloads"].asBoolean
                if (jsonValue.has("AntiModDisableBlockDIPermsPayloads")) AntiModDisable.blockDIPermissionsPayloads = jsonValue["AntiModDisableBlockDIPermsPayloads"].asBoolean
                if (jsonValue.has("AntiModDisableBlockCrackedVapeSabotages")) AntiModDisable.blockCrackedVapeSabotages = jsonValue["AntiModDisableBlockCrackedVapeSabotages"].asBoolean
                if (jsonValue.has("AntiModDisableBlockSchematicaPayloads")) AntiModDisable.blockSchematicaPayloads = jsonValue["AntiModDisableBlockSchematicaPayloads"].asBoolean
                if (jsonValue.has("AntiModDisableDebug")) AntiModDisable.debug = jsonValue["AntiModDisableDebug"].asBoolean
                if (jsonValue.has("BungeeSpoof")) BungeeCordSpoof.enabled = jsonValue["BungeeSpoof"].asBoolean
                if (jsonValue.has("AutoReconnectDelay")) delay = jsonValue["AutoReconnectDelay"].asInt
            }

            "thealtening" ->
            {
                val jsonValue = value as JsonObject
                if (jsonValue.has("API-Key")) apiKey = jsonValue["API-Key"].asString
            }

            "liquidchat" ->
            {
                val jsonValue = value as JsonObject
                if (jsonValue.has("token")) jwtToken = jsonValue["token"].asString
            }

            "donatorcape" ->
            {
                val jsonValue = value as JsonObject
                if (jsonValue.has("TransferCode")) transferCode = jsonValue["TransferCode"].asString
                if (jsonValue.has("CapeEnabled")) capeEnabled = jsonValue["CapeEnabled"].asBoolean
            }

            "background" ->
            {
                val jsonValue = value as JsonObject
                if (jsonValue.has("Enabled")) enabled = jsonValue["Enabled"].asBoolean
                if (jsonValue.has("Particles")) particles = jsonValue["Particles"].asBoolean
            }

            else ->
            {
                val module = LiquidBounce.moduleManager.getModule(key)
                if (module != null)
                {
                    val jsonModule = value.asJsonObject
                    for (moduleValue in module.values)
                    {
                        val element = jsonModule[moduleValue.name]
                        if (element != null) moduleValue.fromJson(element)
                    }

                    for (moduleValue in module.flatValues) if (moduleValue.isAliasPresent(jsonModule))
                    {
                        moduleValue.fromJsonAlias(jsonModule)
                        ClientUtils.logger.info("[FileManager] [Backward-compatibility] Value ${module.name}.${moduleValue.name}")
                        backwardCompatibility = true
                    }
                }
            }
        }

        if (backwardCompatibility)
        {
            ClientUtils.logger.info("[FileManager] Loaded values with backward-compatibility feature.")
            saveConfig()
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig()
    {
        val jsonObject = JsonObject()
        jsonObject.addProperty("CommandPrefix", LiquidBounce.commandManager.prefix)
        jsonObject.addProperty("ShowRichPresence", LiquidBounce.clientRichPresence.showRichPresenceValue)

        val jsonTargets = JsonObject()
        jsonTargets.addProperty("TargetPlayer", EntityUtils.targetPlayer)
        jsonTargets.addProperty("TargetMobs", EntityUtils.targetMobs)
        jsonTargets.addProperty("TargetAnimals", EntityUtils.targetAnimals)
        jsonTargets.addProperty("TargetInvisible", EntityUtils.targetInvisible)
        jsonTargets.addProperty("TargetArmorStand", EntityUtils.targetArmorStand)
        jsonTargets.addProperty("TargetDead", EntityUtils.targetDead)
        jsonObject.add("targets", jsonTargets)

        val jsonFeatures = JsonObject()
        jsonFeatures.addProperty("AntiModDisable", AntiModDisable.enabled)
        jsonFeatures.addProperty("AntiModDisableBlockFMLPackets", AntiModDisable.blockFMLPackets)
        jsonFeatures.addProperty("AntiModDisableBlockFMLProxyPackets", AntiModDisable.blockFMLProxyPackets)
        jsonFeatures.addProperty("AntiModDisableSpoofBrandPayloadPackets", AntiModDisable.blockClientBrandRetrieverPackets)
        jsonFeatures.addProperty("AntiModDisableBlockWDLPayloads", AntiModDisable.blockWDLPayloads)
        jsonFeatures.addProperty("AntiModDisableBlockBetterSprintingPayloads", AntiModDisable.blockBetterSprintingPayloads)
        jsonFeatures.addProperty("AntiModDisableBlock5zigPayloads", AntiModDisable.block5zigsmodPayloads)
        jsonFeatures.addProperty("AntiModDisableBlockPermsReplPayloads", AntiModDisable.blockReplicatedPermissionsPayloads)
        jsonFeatures.addProperty("AntiModDisableBlockDIPermsPayloads", AntiModDisable.blockDIPermissionsPayloads)
        jsonFeatures.addProperty("AntiModDisableBlockCrackedVapeSabotages", AntiModDisable.blockCrackedVapeSabotages)
        jsonFeatures.addProperty("AntiModDisableBlockSchematicaPayloads", AntiModDisable.blockSchematicaPayloads)
        jsonFeatures.addProperty("AntiModDisableDebug", AntiModDisable.debug)
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

        val backgroundObject = JsonObject()
        backgroundObject.addProperty("Enabled", enabled)
        backgroundObject.addProperty("Particles", particles)
        jsonObject.add("Background", backgroundObject)

        LiquidBounce.moduleManager.modules.filter { it.values.isNotEmpty() }.forEach { module: Module ->
            val jsonModule = JsonObject()
            module.values.forEach(Consumer { value: AbstractValue -> jsonModule.add(value.name, value.toJson()) })
            jsonObject.add(module.name, jsonModule)
        }

        val writer = file.bufferedWriter()
        writer.write(FileManager.PRETTY_GSON.toJson(jsonObject) + System.lineSeparator())
        writer.close()
    }
}
