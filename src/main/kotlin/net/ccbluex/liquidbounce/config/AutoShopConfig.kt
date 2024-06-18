package net.ccbluex.liquidbounce.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.*
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.AutoShopConfig
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.HttpClient
import java.io.File

object AutoShopConfig {
    private val configFolder = File(
        ConfigSystem.rootFolder, "autoshop-configs"
    )

    private object ConditionNodeSerializer : JsonContentPolymorphicSerializer<ConditionNode>(ConditionNode::class) {
        override fun selectDeserializer(element: JsonElement): KSerializer<out ConditionNode> {
            return when {
                "id" in element.jsonObject -> ItemInfo.serializer()
                "any" in element.jsonObject -> AnyConditionNode.serializer()
                "all" in element.jsonObject -> AllConditionNode.serializer()
                else -> throw IllegalArgumentException("Unknown type: ${element.jsonObject}")
            }
        }
    }

    private val module = SerializersModule {
        polymorphic(ConditionNode::class, ConditionNodeSerializer) {
            subclass(ItemInfo::class)
            subclass(AnyConditionNode::class)
            subclass(AllConditionNode::class)
        }
    }

    private val jsonDecoder = Json { serializersModule = module; ignoreUnknownKeys = true }

    fun load(configFileName: String = ModuleAutoShop.configName): Boolean {
        try {
            val configFile = File(ConfigSystem.rootFolder, "autoshop-configs/$configFileName.json")
            val autoShopConfig = parseJsonFile(configFile)

            // add items to AutoShop
            ModuleAutoShop.disable()
            ModuleAutoShop.currentConfig = autoShopConfig
            ModuleAutoShop.prevCategorySlot = autoShopConfig.initialCategorySlot
            ModuleAutoShop.enable()
        } catch (throwable: Throwable) {
            logger.error("Failed to load items for AutoShop.", throwable)
            return false
        }

        return true
    }


    /**
     * Reads a raw shop config from a json file and returns a shop config ready to use
     */
    private fun parseJsonFile(file: File): AutoShopConfig {
        val json = file.readText()
        return jsonDecoder.decodeFromString<AutoShopConfig>(json)
    }

    /**
     * Downloads the default autoShop configs from the cloud.
     */
    fun downloadDefaultConfigs() {
        if (configFolder.exists()) {
            return
        }

        configFolder.mkdir()
        this.runCatching {
            logger.info("Downloading the default AutoShop configs...")
            // TODO: make it download the whole folder
            HttpClient.download(
                "${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/dexland.json",
                configFolder.resolve("dexland.json"))
            HttpClient.download(
                "${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/pikanetwork.json",
                configFolder.resolve("pikanetwork.json"))
            HttpClient.download(
                "${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/pikanetwork2.json",
                configFolder.resolve("pikanetwork2.json"))
            HttpClient.download(
                "${LiquidBounce.CLIENT_CLOUD}/autoshop-configs/test.json",
                configFolder.resolve("test.json"))
            logger.info("Successfully downloaded the default AutoShop configs")
        }.onFailure {
            logger.error("Failed to download the default AutoShop configs", it)
        }
    }
}
