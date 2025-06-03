package net.ccbluex.liquidbounce.features.module.modules.combat.autorod

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.interactItem
import net.minecraft.item.Items

@Suppress("MagicNumber")
internal class Using : Configurable("Using") {
    private val onItemUsing by boolean("IgnoreUsingItem", false)

    private val push = tree(Push())
    private val pullback = tree(Pullback())

    internal var isUsingRod = false
    private var resetSlot: Int? = null

    internal fun startRodUsing(slot: HotbarItemSlot) = push.testPushRod {
        val (yaw, pitch) = RotationManager.currentRotation ?: player.rotation

        slot.takeIf { it !is OffHandSlot }
            ?.hotbarSlotForServer
            ?.let {
                resetSlot = player.inventory.selectedSlot

                player.inventory.selectedSlot = it
                interaction.syncSelectedSlot()
            }
        interactItem(slot.useHand, yaw, pitch)

        isUsingRod = true
        pullback.reset()
    }

    internal fun proceedUsingRod() = pullback.testPullbackRod {
        if (canUseRodWhenUsingItem()) {
            interaction.stopUsingItem(player)

            resetSlot?.let {
                player.inventory.selectedSlot = it
                interaction.syncSelectedSlot()
            }
        }

        resetSlot = null
        isUsingRod = false
        push.reset()
    }

    internal fun canUseRodWhenUsingItem() =
        onItemUsing
        || (player.activeItem?.item != Items.FISHING_ROD
            && !(player.usingItem || KillAuraAutoBlock.blockVisual))
}
