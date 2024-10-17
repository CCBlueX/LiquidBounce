package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.item.BowItem
import net.minecraft.item.Items
import net.minecraft.util.Identifier
import net.minecraft.entity.Entity  // Import Entity class

abstract class MurderMysteryGenericMode(name: String) : Choice(name), MurderMysteryMode {
    protected val bowSkins = HashSet<String>()
    protected val murdererSkins = HashSet<String>()

    /**
     * What is our current player doing? Is he murderer?
     */
    protected var currentPlayerType = MurderMysteryMode.PlayerType.NEUTRAL

    val repeatable =
        repeatable {
            currentPlayerType = player.handItems.firstNotNullOfOrNull {
                when {
                    it.item is BowItem || it.item == Items.ARROW -> MurderMysteryMode.PlayerType.DETECTIVE_LIKE
                    MurderMysterySwordDetection.isSword(it.item) -> MurderMysteryMode.PlayerType.MURDERER
                    else -> null
                }
            } ?: MurderMysteryMode.PlayerType.NEUTRAL
        }

    override fun reset() {
        this.bowSkins.clear()
        this.murdererSkins.clear()
    }

    override fun handleHasBow(
        entity: AbstractClientPlayerEntity,
        locationSkin: Identifier,
    ) {
        // Access the entityId directly from the entity (inherited from Entity)
        val entityId = entity.id  // Using .id which is accessible in the Entity class
        if (bowSkins.add(locationSkin.path)) {
            chat("${entity.gameProfile.name} has a bow. (Entity ID: $entityId)")
            ModuleMurderMystery.playBow = true
        }
    }

    override fun getPlayerType(player: AbstractClientPlayerEntity): MurderMysteryMode.PlayerType {
        // Access the entityId here if needed
        val entityId = player.id  // Using .id inherited from Entity
        return when (player.skinTextures.texture.path) {
            in murdererSkins -> MurderMysteryMode.PlayerType.MURDERER
            in bowSkins -> MurderMysteryMode.PlayerType.DETECTIVE_LIKE
            else -> MurderMysteryMode.PlayerType.NEUTRAL
        }
    }

    override fun shouldAttack(entity: AbstractClientPlayerEntity): Boolean {
        val targetPlayerType = getPlayerType(entity)

        // Access entityId here for logging or additional logic
        val entityId = entity.id  // Using .id inherited from Entity
        return when (currentPlayerType) {
            MurderMysteryMode.PlayerType.MURDERER -> targetPlayerType != MurderMysteryMode.PlayerType.MURDERER
            else -> targetPlayerType == MurderMysteryMode.PlayerType.MURDERER
        }
    }
}
