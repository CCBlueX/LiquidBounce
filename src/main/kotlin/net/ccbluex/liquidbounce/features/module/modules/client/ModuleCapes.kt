@file:Suppress("unused")
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.cosmetic.CapeCosmeticsManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.io.FileInputStream

object ModuleCapes : ClientModule("Capes", Category.CLIENT) {

    internal val capeMode = choices(
        "Mode",
        ClientMode,
        arrayOf(ClientMode, FileImageMode)
    )

    sealed class CapeMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*> get() = capeMode
        abstract fun getTexture(): Identifier
        open fun clearCache() {}
    }


    object ClientMode : CapeMode("Client") {

        private val image by enumChoice("Image", ClientCape.JMcomicFix)
        private var cachedTexture: Identifier? = null

        override fun clearCache() {
            cachedTexture?.let { mc.textureManager.destroyTexture(it) }
            cachedTexture = null
        }

        override fun getTexture(): Identifier {
            val selected = image
            if (cachedTexture == null) {
                // 注册为与资源路径一致的动态图片（保持和 registerAsDynamicImageFromClientResources 行为一致）
                val newId = "image/capes/${selected.fileName}.png".registerAsDynamicImageFromClientResources()
                cachedTexture = newId
            }
            return cachedTexture!!
        }

        enum class ClientCape(
            override val choiceName: String,
            val fileName: String
        ) : NamedChoice {
            Astolfo("Astolfo", "Astolfo"),
            AzureWare("AzureWare", "AzureWare"),
            FDP("FDP", "FDP"),
            Hanabi("Hanabi", "Hanabi"),
            Tenacity("Tenacity", "Tenacity"),
            LiquidBounce("LiquidBounce", "LiquidBounce"),
            JMcomicFix("JMcomicFix", "JMcomicFix"),
            Novoline("Novoline", "Novoline"),
            Opal("Opal", "Opal"),
            PowerX("PowerX", "PowerX"),
            Rise("Rise", "Rise"),
            Sensei("Sensei", "Sensei"),
            VapeV4("VapeV4", "Vape_V4"),
            VapeLite("VapeLite", "Vape_Lite");
        }
    }


    object FileImageMode : CapeMode("File") {
        private val customImage by file("CustomImage")
        private var cachedTexture: Identifier? = null
        private var cachedNativeImage: NativeImage? = null
        private var cachedPath: String? = null

        override fun clearCache() {
            cachedTexture?.let { mc.textureManager.destroyTexture(it) }
            cachedTexture = null
            cachedNativeImage?.close()
            cachedNativeImage = null
            cachedPath = null
        }

        override fun getTexture(): Identifier {
            val file = customImage.absoluteFile.takeIf { it.exists() && it.isFile && it.canRead() }
            if (file == null) {
                cachedTexture?.let { return it }
                clearCache()
                val defaultId = Identifier.of("liquidbounce", "cape-default")
                cachedNativeImage = NativeImage.read(
                    javaClass.getResourceAsStream("/assets/liquidbounce/image/capes/JMcomicFix.png")!!
                )
                mc.textureManager.registerTexture(defaultId, NativeImageBackedTexture(cachedNativeImage))
                cachedTexture = defaultId
                return defaultId
            }

            val path = file.absolutePath
            if (cachedPath != path) {
                clearCache()
                FileInputStream(file).use { fis ->
                    cachedNativeImage = NativeImage.read(fis)
                }
                val id = Identifier.of("liquidbounce", "cape-file-${System.currentTimeMillis().toString(36)}")
                mc.textureManager.registerTexture(id, NativeImageBackedTexture(cachedNativeImage))
                cachedTexture = id
                cachedPath = path
            }
            return cachedTexture!!
        }
    }

    private var localCapeIdentifier: Identifier? = null
    private var localCapeModeSelected: CapeMode? = null

    fun getLocalCapeTextureId(): Identifier {
        val mode = capeMode.activeChoice

        if (localCapeIdentifier == null || localCapeModeSelected != mode) {
            localCapeIdentifier?.let { mc.textureManager.destroyTexture(it) }

            localCapeModeSelected?.clearCache()

            localCapeIdentifier = mode.getTexture()
            localCapeModeSelected = mode
        }
        return localCapeIdentifier!!
    }


    fun getCapeTextureId(): Identifier = getLocalCapeTextureId()

    override fun onEnabled() {
        localCapeIdentifier?.let { mc.textureManager.destroyTexture(it) }
        localCapeModeSelected?.clearCache()
        localCapeIdentifier = null
        localCapeModeSelected = null
    }

    override fun onDisabled() {
        localCapeIdentifier?.let { mc.textureManager.destroyTexture(it) }
        localCapeModeSelected?.clearCache()
        localCapeIdentifier = null
        localCapeModeSelected = null

        CapeCosmeticsManager.clearAllCachedCapes()
    }

}
