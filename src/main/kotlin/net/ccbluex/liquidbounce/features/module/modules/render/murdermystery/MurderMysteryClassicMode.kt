package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.util.Identifier

object MurderMysteryClassicMode : MurderMysteryGenericMode("Classic") {
    override val parent
        get() = ModuleMurderMystery.modes

    override fun handleHasSword(
        entity: AbstractClientPlayerEntity,
        locationSkin: Identifier,
    ) {
        if (murdererSkins.add(locationSkin.path)) {
            chat("It's " + entity.gameProfile.name)

            ModuleMurderMystery.playHurt = true
        }
    }
}
