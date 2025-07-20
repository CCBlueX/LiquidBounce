package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget.interaction
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget.network
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget.player
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget.world
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket

@Suppress("unused")
internal enum class FireworkUseMode(
    override val choiceName: String,
    val useFireworkSlot: (HotbarItemSlot, Int) -> Unit
) : NamedChoice {
    NORMAL("Normal", { slot, resetDelay ->
        useHotbarSlotOrOffhand(slot, resetDelay)
    }),
    PACKET("Packet", { slot, _ ->
        with (player.inventory.selectedSlot) {
            val slotUpdateFlag = slot !is OffHandSlot && slot.hotbarSlotForServer != this

            if (slotUpdateFlag) {
                player.inventory.selectedSlot = slot.hotbarSlotForServer
                network.sendPacket(UpdateSelectedSlotC2SPacket(slot.hotbarSlotForServer))
            }

            interaction.sendSequencedPacket(world) { sequence ->
                PlayerInteractItemC2SPacket(slot.useHand, sequence, player.yaw, player.pitch)
            }

            if (slotUpdateFlag) {
                player.inventory.selectedSlot = this
                network.sendPacket(UpdateSelectedSlotC2SPacket(this))
            }
        }
    })
}
