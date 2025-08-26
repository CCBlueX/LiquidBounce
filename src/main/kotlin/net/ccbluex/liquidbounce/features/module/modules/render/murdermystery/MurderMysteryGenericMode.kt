package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.item.BowItem
import net.minecraft.item.Items
import net.minecraft.util.Identifier

abstract class MurderMysteryGenericMode(name: String) : Choice(name), MurderMysteryMode {
    protected val bowSkins = HashSet<String>()
    protected val murdererSkins = HashSet<String>()

    /**
     * What is our current player doing? Is he murderer?
     */
    protected var currentPlayerType = MurderMysteryMode.PlayerType.NEUTRAL

    val repeatable =
        tickHandler {
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
        if (bowSkins.add(locationSkin.path)) {
            chat(entity.gameProfile.name + " has a bow.")

            ModuleMurderMystery.playBow = true
        }
    }

    override fun getPlayerType(player: AbstractClientPlayerEntity): MurderMysteryMode.PlayerType {
        return when (player.skinTextures.texture.path) {
            in murdererSkins -> MurderMysteryMode.PlayerType.MURDERER
            in bowSkins -> MurderMysteryMode.PlayerType.DETECTIVE_LIKE
            else -> MurderMysteryMode.PlayerType.NEUTRAL
        }
    }

    override fun shouldAttack(entity: AbstractClientPlayerEntity): Boolean {
        val targetPlayerType = getPlayerType(entity)

        return when (currentPlayerType) {
            MurderMysteryMode.PlayerType.MURDERER -> targetPlayerType != MurderMysteryMode.PlayerType.MURDERER
            else -> targetPlayerType == MurderMysteryMode.PlayerType.MURDERER
        }
    }
}
