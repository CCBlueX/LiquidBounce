package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.minecraft.item.BlockItem
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand

internal object NoFallHoplite : Choice("Hoplite") {

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleNoFall.modes

    /*
    * Seems to be taken off Grim's GitHub. Issue #1275
    *
    * Code taken from LiquidBounce's Issue List.
    * Made it automatically switch to a block in the hotbar if the player is falling over 3 blocks.
    * Naming Reason: Hoplite is one of the few servers that use Latest grim and that this bypasess on.
    * MccIsland's Grim Fork has managed to patch this bypass.
    */
    val tickHandler = handler<PlayerTickEvent> {
        if (!player.isOnGround && player.fallDistance > 2f) {
            val hand = Hand.entries.find { !player.getStackInHand(it).isEmpty } ?: return@handler
            val wasUsingItem = player.isUsingItem

            if (wasUsingItem) {
                interaction.stopUsingItem(player)
            }

            val originVec = player.pos
            player.setPosition(originVec.add(0.0, 1.0E-9, 0.0))
            if (interaction.interactItem(player, hand).shouldSwingHand()) {
                network.sendPacket(HandSwingC2SPacket(hand))
            }

            if (wasUsingItem) {
                interaction.stopUsingItem(player)
            }

            player.setPosition(originVec)
            player.onLanding()
        }

        // If the player is still falling over 3 blocks, switch to any block in the hotbar
        if (player.fallDistance > 3f) {
            for (i in 0..8) {
                val itemStack = player.inventory.getStack(i)
                if (!itemStack.isEmpty && itemStack.item is BlockItem) {
                    SilentHotbar.selectSlotSilently(this, i, 5)
                    return@handler
                }
            }
        }
    }
}
