@ -0,0 +1,62 @@
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.minecraft.item.BlockItem
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand

internal object NoFallHoplite : Choice("Hoplite") {

    override val parent: ChoiceConfigurable
        get() = ModuleNoFall.modes

    /*
    * Seems to be taken off Grim's github. Issue #1275
    * Code taken from LiquidBounce's Issue List.
    * Made it automatically switch to a block in the hotbar if the player is falling over 3 blocks.
    * Naming Reason: Hoplite is one of the only servers that use Latest grim and that this bypasess on.
    * MccIsland's Grim Fork has managed to patch this bypass.
    *
    *
    *
     */

    val tickHandler = handler<PlayerTickEvent> {
        if (mc.player != null && !mc.player!!.isOnGround && mc.player!!.fallDistance > 2f) {
            for (hand in Hand.values()) {
                val stack = mc.player!!.getStackInHand(hand)
                if (!stack.isEmpty) {
                    if (mc.player!!.isUsingItem) {
                        mc.interactionManager!!.stopUsingItem(mc.player)
                    }
                    val originVec = mc.player!!.pos
                    mc.player!!.setPosition(originVec.add(0.0, 1.0E-9, 0.0))
                    if (mc.interactionManager!!.interactItem(mc.player, hand).shouldSwingHand()) {
                        mc.networkHandler!!.sendPacket(HandSwingC2SPacket(hand))
                    }
                    if (mc.player!!.isUsingItem) {
                        mc.interactionManager!!.stopUsingItem(mc.player)
                    }
                    mc.player!!.setPosition(originVec)
                    mc.player!!.onLanding()
                    break
                }
            }
        }

        // If the player is still falling over 3 blocks, switch to any block in the hotbar
        if (mc.player != null && !mc.player!!.isOnGround && mc.player!!.fallDistance > 3f) {
            for (i in 0..8) {
                val stack = mc.player!!.inventory.getStack(i)
                if (!stack.isEmpty && stack.item is BlockItem) {
                    mc.player!!.inventory.selectedSlot = i
                    break
                }
            }
        }
    }
}
