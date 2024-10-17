package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.entity.Entity  // Import Entity class

object MurderMysteryClassicMode : MurderMysteryGenericMode("Classic") {
    private val murdererEntityIds = mutableSetOf<Int>()  // Set to store detected murderer entity IDs

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleMurderMystery.modes

    override fun handleHasSword(
        entity: AbstractClientPlayerEntity,
        locationSkin: Identifier,
    ) {
        // Access the entityId directly from the entity (inherited from Entity)
        val entityId = entity.id  // Using .id from the Entity class

        // First check if the entity ID is already classified as a murderer
        if (murdererEntityIds.add(entityId)) {
            chat("It's " + entity.gameProfile.name + " (Entity ID: $entityId)")
            ModuleMurderMystery.playHurt = true
        } else if (entityId == 0) {  // If entity ID is 0 or not available, fall back to skin detection
            if (murdererSkins.add(locationSkin.path)) {
                chat("It's " + entity.gameProfile.name + " (Skin fallback)")
                ModuleMurderMystery.playHurt = true
            }
        }
    }
}
