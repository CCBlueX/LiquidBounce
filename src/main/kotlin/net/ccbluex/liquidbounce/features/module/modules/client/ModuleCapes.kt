package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.cosmetic.CapeCosmeticsManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.minecraft.util.Identifier

object ModuleCapes : ClientModule("Capes", Category.CLIENT) {

    @Suppress("UNUSED")
    enum class CapeMode(
        override val choiceName: String,
        val fileName: String
    ) : NamedChoice {
        Astolfo("Astolfo", "Astolfo"),
        BlueArchive("BlueArchive", "BlueArchive"),
        Diana("Diana", "Diana"),
        FDP("FDP", "FDP"),
        Tenacity("Tenacity", "Tenacity"),
        LiquidBounce("LiquidBounce", "LiquidBounce"),
        JMcomicFix("JMcomicFix", "JMcomicFix"),
        Novoline("Novoline", "Novoline"),
        Opal("Opal", "Opal"),
        PowerX("PowerX", "PowerX"),
        Rise("Rise", "Rise"),
        Sensei("Sensei", "Sensei"),
        VapeV4("VapeV4", "Vape_V4"),
        VapeLite("VapeLite", "Vape_Lite"),

    }


    val capeMode by enumChoice("CapeMode", CapeMode.JMcomicFix)

    private var localCapeIdentifier: Identifier? = null
    private var localCapeModeSelected: CapeMode? = null


    fun getLocalCapeTextureId(): Identifier {
        val mode = capeMode

        if (localCapeIdentifier == null || localCapeModeSelected != mode) {

            localCapeIdentifier?.let { mc.textureManager.destroyTexture(it) }

            val newId = "image/capes/${mode.fileName}.png"
                .registerAsDynamicImageFromClientResources()
            localCapeIdentifier = newId
            localCapeModeSelected = mode
        }
        return localCapeIdentifier!!
    }

    fun getCapeTextureId(): Identifier = getLocalCapeTextureId()

    override fun onEnabled() {


        localCapeIdentifier?.let { mc.textureManager.destroyTexture(it) }
        localCapeIdentifier = null
        localCapeModeSelected = null
    }

    override fun onDisabled() {

        localCapeIdentifier?.let { mc.textureManager.destroyTexture(it) }
        localCapeIdentifier = null
        localCapeModeSelected = null

        CapeCosmeticsManager.clearAllCachedCapes()
    }
}
