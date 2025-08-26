package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.item.BowItem
import net.minecraft.item.Items
import net.minecraft.util.Identifier

object MurderMysteryInfectionMode : MurderMysteryGenericMode("Infection") {
    override val parent
        get() = ModuleMurderMystery.modes

    val rep =
        tickHandler {
            world.players
                .filterIsInstance<AbstractClientPlayerEntity>()
                .filter {
                    it.isUsingItem && player.handItems.any { stack -> stack.item is BowItem } ||
                        player.handItems.any { stack -> stack.item == Items.ARROW }
                }
                .forEach { playerEntity ->
                    handleHasBow(playerEntity, playerEntity.skinTextures.texture)
                }
        }

    override fun handleHasSword(
        entity: AbstractClientPlayerEntity,
        locationSkin: Identifier,
    ) {
        if (murdererSkins.add(locationSkin.path) && murdererSkins.size == 1) {
            chat(entity.gameProfile.name + " is the first infected.")

            ModuleMurderMystery.playHurt = true
        }
    }

    override fun disallowsArrowDodge(): Boolean {
        // Don't dodge if we are not dead yet.
        return currentPlayerType == MurderMysteryMode.PlayerType.DETECTIVE_LIKE
    }

}
