package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.util.Identifier

object MurderMysteryClassicMode : MurderMysteryGenericMode("Classic") {

    // Set to track the unique entity IDs of known murderers.
    private val knownMurderers = mutableSetOf<Int>()
    
    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleMurderMystery.modes

    override fun handleHasSword(
        entity: AbstractClientPlayerEntity,
        locationSkin: Identifier,
    ) {
        // Use entity.getId() instead of entity.entityId
        val entityId = entity.id
        
        // Prioritize entity ID if available.
        if (entityId > 0 && !ModuleMurderMystery.knownMurderers.contains(entityId)) {
            ModuleMurderMystery.knownMurderers.add(entityId)
            chat("It's " + entity.gameProfile.name)
            ModuleMurderMystery.playHurt = true

        // If entity ID is unavailable, fall back to using the skin for identification (less precise).
        } else if (murdererSkins.add(locationSkin.path)) {
            chat("It's " + entity.gameProfile.name + " (by skin)")
            ModuleMurderMystery.playHurt = true
        }
    }
}
